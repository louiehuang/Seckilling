package com.seckilling.service;

import com.seckilling.service.model.PromoModel;

public interface PromoService {

    PromoModel getPromoByItemId(Integer itemId);

    void publishPromo(Integer promoId);

    /**
     * generate token for second kill promotion
     */
    String generateSecondKillToken(Integer userId, Integer itemId, Integer promoId);

}
