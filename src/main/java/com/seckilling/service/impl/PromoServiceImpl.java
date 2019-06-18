package com.seckilling.service.impl;

import com.seckilling.dao.PromoDOMapper;
import com.seckilling.dataobject.PromoDO;
import com.seckilling.service.ItemService;
import com.seckilling.service.PromoService;
import com.seckilling.service.model.ItemModel;
import com.seckilling.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
public class PromoServiceImpl implements PromoService {

    @Resource
    private PromoDOMapper promoDOMapper;

    @Resource
    private ItemService itemService;

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        PromoModel promoModel = convertPromoDOToPromoModel(promoDO);

        if (promoModel == null)
            return null;

        //check promotion activity status
        if (promoModel.getStartDate().isAfterNow()) {
            promoModel.setStatus(1);
        } else if (promoModel.getEndDate().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }

        return promoModel;
    }


    @Override
    public void publishPromo(Integer promoId) {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        Integer itemId = promoDO.getItemId();
        if (itemId == null || itemId == 0) {
            return;
        }

        ItemModel itemModel = itemService.getItemById(itemId);
        //Be aware that item could be sold during the period between we get item stock from DB and set it to cache
        //Here we simply assume that the stock will not change
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());
    }


    private PromoModel convertPromoDOToPromoModel(PromoDO promoDO) {
        if (promoDO == null)
            return null;
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
