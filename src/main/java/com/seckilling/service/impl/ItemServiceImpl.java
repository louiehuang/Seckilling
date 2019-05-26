package com.seckilling.service.impl;

import com.seckilling.dao.ItemDOMapper;
import com.seckilling.dao.ItemStockDOMapper;
import com.seckilling.dataobject.ItemDO;
import com.seckilling.dataobject.ItemStockDO;
import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.service.ItemService;
import com.seckilling.service.model.ItemModel;
import com.seckilling.validator.ValidationResult;
import com.seckilling.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ItemServiceImpl implements ItemService {

    @Resource
    private ValidatorImpl validator;

    @Resource
    private ItemDOMapper itemDOMapper;

    @Resource
    private ItemStockDOMapper itemStockDOMapper;

    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //check params
        ValidationResult validationResult = validator.validate(itemModel);
        if (validationResult.isHasError()) {
            throw new BusinessException(EBusinessError.PARAMTER_NOT_VALID, validationResult.getErrMsg());
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
        return convertDataObjectToItemModel(itemDO, itemStockDO);
    }

    @Override
    public List<ItemModel> listItem() {
        return null;
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
        itemModel.setStock(itemModel.getStock());

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
