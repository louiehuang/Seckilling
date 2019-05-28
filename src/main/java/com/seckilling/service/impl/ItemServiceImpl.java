package com.seckilling.service.impl;

import com.seckilling.common.Constants;
import com.seckilling.dao.ItemDOMapper;
import com.seckilling.dao.ItemStockDOMapper;
import com.seckilling.dataobject.ItemDO;
import com.seckilling.dataobject.ItemStockDO;
import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.service.ItemService;
import com.seckilling.service.PromoService;
import com.seckilling.service.model.ItemModel;
import com.seckilling.service.model.PromoModel;
import com.seckilling.validator.ValidationResult;
import com.seckilling.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
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
    @Transactional
    public boolean deductStock(Integer itemId, Integer quantity) {
        int affectedRows = itemStockDOMapper.deductStock(itemId, quantity);
        //if deduct succeed, return true
        return affectedRows > 0;
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
