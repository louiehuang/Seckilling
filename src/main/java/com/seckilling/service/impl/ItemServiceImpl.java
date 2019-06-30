package com.seckilling.service.impl;

import com.seckilling.common.Constants;
import com.seckilling.dao.ItemDOMapper;
import com.seckilling.dao.ItemStockDOMapper;
import com.seckilling.dao.StockLogDOMapper;
import com.seckilling.dataobject.ItemDO;
import com.seckilling.dataobject.ItemStockDO;
import com.seckilling.dataobject.StockLogDO;
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
import java.util.UUID;
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

    @Resource
    private StockLogDOMapper stockLogDOMapper;


    /**
     * Create item info, write records to item table and item_stock table
     * @param itemModel ItemModel object assembled by ItemController
     * @return ItemModel object (contains PromoModel) fetched from DB
     * @throws BusinessException
     */
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

        //set stock in Redis
        redisTemplate.opsForValue().set(String.format(Constants.REDIS_ITEM_STOCK, itemModel.getId()), itemModel.getStock());

        //return created object (so upstream knows the status of the object we created)
        return getItemById(itemModel.getId());
    }


    /**
     * Set item stock to Redis
     * @param itemId itemId (primary key of item table)
     */
    @Override
    public void publishItem(Integer itemId) {
        ItemModel itemModel = getItemById(itemId);
        redisTemplate.opsForValue().set(String.format(Constants.REDIS_ITEM_STOCK, itemModel.getId()), itemModel.getStock());
    }


    /**
     * Get item info from DB by itemId
     * @param id itemId (primary key)
     * @return ItemModel object (contains PromoModel) fetched from DB
     */
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


    /**
     * Get item info from Redis by itemId
     * @param id itemId
     * @return ItemModel object cached in Redis
     */
    @Override
    public ItemModel getItemByIdFromRedis(Integer id) {
        String itemKey = String.format(Constants.REDIS_ITEM_VALIDATE, id);
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get(itemKey);
        if (itemModel == null) {  // if not found, query from DB
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set(itemKey, itemModel);
            redisTemplate.expire(itemKey, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void addStockInRedis(Integer itemId, Integer quantity, Boolean isPromo) {
        redisTemplate.opsForValue().increment(String.format(Constants.REDIS_PROMO_ITEM_STOCK, itemId), quantity);
    }


    @Override
    @Transactional
    public boolean deductStockInRedis(Integer itemId, Integer quantity, Boolean isPromo) {
        String keyTmpl = isPromo ? Constants.REDIS_PROMO_ITEM_STOCK : Constants.REDIS_ITEM_STOCK;
        //Only update stock in key "promo_item_stock_", stock in key "item_validate_" remain the same
        //update redis only here, DB records will be updated in consumer
        long result = redisTemplate.opsForValue().increment(String.format(keyTmpl, itemId), quantity * -1);
        if (result > 0) {  //stock left > 0
            return true;
        } else if (result == 0) {  //mark when sold out
            redisTemplate.opsForValue().set(String.format(keyTmpl, itemId), "true");
            return true;
        } else {
            addStockInRedis(itemId, quantity, isPromo);
            return false;
        }
    }


//    @Override
//    public boolean asyncDeductStock(Integer itemId, Integer quantity) {
//        //send msg
//        return mqProducer.asyncDeductStock(itemId, quantity);
//    }


    /**
     * Update sales column in item table
     * @param itemId itemId (primary key)
     * @param quantity sales number to update
     */
    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer quantity) {
        itemDOMapper.increaseSales(itemId, quantity);
    }


    /**
     * Insert stockLog record in DB to track the status of whether an order is processed successfully
     * @param itemId itemId in this order
     * @param quantity quantity of item bought in this order
     * @return stock_log_id (an UUID generated by this method)
     */
    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer quantity) {
        StockLogDO stockLogDO = new StockLogDO();
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replaceAll("-", ""));
        stockLogDO.setItemId(itemId);
        stockLogDO.setQuantity(quantity);
        stockLogDO.setStatus(Constants.STOCK_LOG_INIT);

        stockLogDOMapper.insertSelective(stockLogDO);

        return stockLogDO.getStockLogId();
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


    /**
     * Get all items from DB (This method is for test)
     * @return a list that contains all items
     */
    @Override
    public List<ItemModel> getAllItems() {
        List<ItemDO> itemDOList = itemDOMapper.selectAllItems();
        //convert List<ItemDO> to List<ItemModel>
        return itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            return convertDataObjectToItemModel(itemDO, itemStockDO);
        }).collect(Collectors.toList());
    }
}
