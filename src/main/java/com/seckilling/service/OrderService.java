package com.seckilling.service;

import com.seckilling.error.BusinessException;
import com.seckilling.service.model.OrderModel;

public interface OrderService {

    /**
     * Frontend also passes an param called promoId,
     * check whether this promoId belongs to this item and the promotion activity is ongoing.
     */
    OrderModel createOder(Integer userId, Integer itemId, Integer quantity, Integer promoId) throws BusinessException;

}
