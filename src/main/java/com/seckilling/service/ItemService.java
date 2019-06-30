package com.seckilling.service;

import com.seckilling.error.BusinessException;
import com.seckilling.service.model.ItemModel;

import java.util.List;

public interface ItemService {

    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    ItemModel getItemById(Integer id);

    List<ItemModel> getAllItems();

    ItemModel getItemByIdFromRedis(Integer id);

    void addStockInRedis(Integer itemId, Integer quantity);

    boolean deductStockInRedis(Integer itemId, Integer quantity);

//    boolean asyncDeductStock(Integer itemId, Integer quantity);

    void increaseSales(Integer itemId, Integer quantity);

    String initStockLog(Integer itemId, Integer quantity);
}
