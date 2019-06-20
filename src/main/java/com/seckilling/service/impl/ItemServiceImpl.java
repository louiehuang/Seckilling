package com.seckilling.service.impl;

import com.seckilling.common.Constants;
import com.seckilling.dao.ItemDOMapper;
import com.seckilling.dao.ItemStockDOMapper;
import com.seckilling.dataobject.ItemDO;
import com.seckilling.dataobject.ItemStockDO;
import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.mq.MQProducer;
import com.seckilling.service.ItemService;
import com.seckilling.service.PromoService;
import com.seckilling.service.model.ItemModel;
import com.seckilling.service.model.PromoModel;
import com.seckilling.validator.ValidationResult;
import com.seckilling.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class ItemServiceImpl implements ItemService {

    @Resource
    private ValidatorImpl validator;

    @Resource
    private ItemDOMapper itemDOMapper;

    @Resource
    private PromoService promoService;

    @Resource
    private ItemStockDOMapper itemStockDOMapper;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private MQProducer mqProducer;


    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //check params
        ValidationResult validationResult = validator.validate(itemModel);
        if (validationResult.isHasError()) {
            throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID, validationResult.getErrMsg());
        }

        //itemModel -> dataObject
        ItemDO itemDO = convertItemModelToItemDO(itemModel);

        //write to DB, 2 steps
        itemDOMapper.insertSelective(itemDO);

        //if insert succeed, we have the item id now
        itemModel.setId(itemDO.getId());
        ItemStockDO itemStockDO = convertItemModelToItemStockDO(itemModel);

        itemStockDOMapper.insertSelective(itemStockDO);

        //return created object (so upstream knows the status of the object we created)
        return getItemById(itemModel.getId());
    }

    @Override
    public ItemModel getItemById(Integer id) {
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (itemDO == null)
            return null;

        //get stock
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        //DO -> model
        ItemModel itemModel = convertDataObjectToItemModel(itemDO, itemStockDO);

        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if (promoModel != null && promoModel.getStatus() != Constants.PROMO_ENDED) {
            itemModel.setPromoModel(promoModel);
        }

        return itemModel;
    }

    @Override
    public List<ItemModel> getAllItems() {
        List<ItemDO> itemDOList = itemDOMapper.selectAllItems();
        //convert List<ItemDO> to List<ItemModel>
        return itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            return convertDataObjectToItemModel(itemDO, itemStockDO);
        }).collect(Collectors.toList());
    }


    @Override
    public ItemModel getItemByIdFromCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_" + id);
        if (itemModel == null) {  // go to DB
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_" + id, itemModel);
            redisTemplate.expire("item_validate_" + id, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void addStock(Integer itemId, Integer quantity) {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, quantity);
    }


    @Override
    @Transactional
    public boolean deductStock(Integer itemId, Integer quantity) {
//        int affectedRows = itemStockDOMapper.deductStock(itemId, quantity);
//        return affectedRows > 0;  //if deduct succeed, return true

        //Only update stock in key "promo_item_stock_", stock in key "item_" and "item_validate_" remain the same
        long result = redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, quantity * -1);
        if (result >= 0) {  //stock left >= 0
            return true;
        } else {
            addStock(itemId, quantity);
            return false;
        }
    }


    @Override
    public boolean asyncDeductStock(Integer itemId, Integer quantity) {
        //send msg
        return mqProducer.asyncDeductStock(itemId, quantity);
    }


    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer quantity) {
        itemDOMapper.increaseSales(itemId, quantity);
    }


    private ItemDO convertItemModelToItemDO(ItemModel itemModel) {
        if (itemModel == null)
            return null;
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);

        //be careful with the price type
        itemDO.setPrice(itemModel.getPrice().doubleValue());

        return itemDO;
    }

    private ItemStockDO convertItemModelToItemStockDO(ItemModel itemModel) {
        if (itemModel == null)
            return null;
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());

        return itemStockDO;
    }

    private ItemModel convertDataObjectToItemModel(ItemDO itemDO, ItemStockDO itemStockDO) {
        ItemModel itemModel = new ItemModel();

        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setStock(itemStockDO.getStock());

        //be careful with the price type
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));

        return itemModel;
    }
}
