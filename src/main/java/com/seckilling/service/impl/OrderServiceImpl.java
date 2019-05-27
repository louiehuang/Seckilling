package com.seckilling.service.impl;

import com.seckilling.common.Constants;
import com.seckilling.dao.OrderDOMapper;
import com.seckilling.dao.SequenceDOMapper;
import com.seckilling.dataobject.OrderDO;
import com.seckilling.dataobject.SequenceDO;
import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.service.ItemService;
import com.seckilling.service.OrderService;
import com.seckilling.service.UserService;
import com.seckilling.service.model.ItemModel;
import com.seckilling.service.model.OrderModel;
import com.seckilling.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private ItemService itemService;

    @Resource
    private UserService userService;

    @Resource
    private OrderDOMapper orderDOMapper;

    @Resource
    private SequenceDOMapper sequenceDOMapper;


    @Override
    @Transactional
    public OrderModel createOder(Integer userId, Integer itemId, Integer quantity) throws BusinessException {
        //1. check status: whether item and user exist and whether quantity is valid
        ItemModel itemModel = itemService.getItemById(itemId);
        if (itemModel == null) {
            throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID, Constants.ITEM_NOT_EXIST);
        }

        UserModel userModel = userService.getUserById(userId);
        if (userModel == null) {
            throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID, Constants.USER_NOT_EXIST);
        }

        if (quantity <= 0 || quantity >= Constants.MAX_QUANTITY) {
            throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID, Constants.QUANTITY_NOT_VALID);
        }

        //2. deduct stock when creating order (depending on concrete situation, may deduct stock when user made payment)
        if (!itemService.deductStock(itemId, quantity)) {
            throw new BusinessException(EBusinessError.STOCK_NOT_ENOUGH);
        }

        //3. write order to DB
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setQuantity(quantity);
        orderModel.setItemPrice(itemModel.getPrice());
        orderModel.setOrderPrice(itemModel.getPrice().multiply(new BigDecimal(quantity)));
        orderModel.setOrderTimestamp(Timestamp.valueOf(LocalDateTime.now()));

        //generate order id
        orderModel.setOrderId(generateOrderId());

        OrderDO orderDO = convertOrderModelToOrderDO(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //update sales of this item
        itemService.increaseSales(itemId, quantity);

        return orderModel;
    }


    /**
     * length = 16
     * first 8 chars: year month day
     * then 6 chars used as an auto-incremented number
     * last 2 chars: sharding
     *
     * Add transactional(propagation = Propagation.REQUIRES_NEW) so that it does not rollback when
     * this generateOrderId() succeed but createOder() failed, i.e. consume an order_id even if order failed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateOrderId() {
        StringBuilder orderId = new StringBuilder();

        //Used to use Joda-Time, try LocalDateTime now
        LocalDateTime now = LocalDateTime.now();
        String curDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        orderId.append(curDate);

        //auto-incremented number
        SequenceDO sequenceDO = sequenceDOMapper.selectSequenceByName(Constants.ORDER_AUTO_SEQUENCE);
        //TODO: reset current_value in DB when it is greater than 999999 (6 digits)
        int curValue = sequenceDO.getCurrentValue();  //for current use
        sequenceDO.setCurrentValue(curValue + sequenceDO.getStep());  //set value for next call
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);  //update

        for (int i = 0; i < 6 - String.valueOf(curValue).length(); i++) {
            orderId.append("0");
        }
        orderId.append(curValue);

        //last 2 digits
        orderId.append("00"); //no sharding for now

        return orderId.toString();
    }


    private OrderDO convertOrderModelToOrderDO(OrderModel orderModel) {
        if (orderModel == null)
            return null;

        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        //BigDecimal -> Double
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }
}
