package com.seckilling.service;

import com.seckilling.error.BusinessException;
import com.seckilling.service.model.OrderModel;

public interface OrderService {
    OrderModel createOder(Integer userId, Integer itemId, Integer quantity) throws BusinessException;

}
