package com.seckilling.service;

import com.seckilling.service.model.PromoModel;

public interface PromoService {

    PromoModel getPromoByItemId(Integer itemId);

    void publishPromo(Integer promoId);

}
