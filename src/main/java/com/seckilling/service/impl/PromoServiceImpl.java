package com.seckilling.service.impl;

import com.seckilling.common.Constants;
import com.seckilling.dao.PromoDOMapper;
import com.seckilling.dataobject.PromoDO;
import com.seckilling.service.ItemService;
import com.seckilling.service.PromoService;
import com.seckilling.service.UserService;
import com.seckilling.service.model.ItemModel;
import com.seckilling.service.model.PromoModel;
import com.seckilling.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImpl implements PromoService {

    @Resource
    private PromoDOMapper promoDOMapper;

    @Resource
    private ItemService itemService;

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        PromoModel promoModel = convertPromoDOToPromoModel(promoDO);

        if (promoModel == null)
            return null;

        //check and set promotion activity status
        setPromoModelStatus(promoModel);

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
        redisTemplate.opsForValue().set(String.format(Constants.REDIS_PROMO_ITEM_STOCK, itemModel.getId()), itemModel.getStock());

        //set limitation for the number of promo tokens
        redisTemplate.opsForValue().set(String.format(Constants.REDIS_PROMO_COUNT_THRESHOLD, promoId), 5 * itemModel.getStock());
    }


    @Override
    public String generateSecondKillToken(Integer userId, Integer itemId, Integer promoId) {
        // check stock, if already out of stock, return fail creating order
        if (redisTemplate.hasKey(String.format(Constants.REDIS_PROMO_OUT_OF_STOCK, itemId))) {
            return null;
        }

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);

        PromoModel promoModel = convertPromoDOToPromoModel(promoDO);

        if (promoModel == null)
            return null;

        //check and set promotion activity status
        setPromoModelStatus(promoModel);

        //check item is valid
        ItemModel itemModel = itemService.getItemByIdFromRedis(itemId);
        if (itemModel == null) {
            return null;
        }

        //check user is valid
        UserModel userModel = userService.getUserByIdFromRedis(userId);
        if (userModel == null) {
            return null;
        }

        //check promotion is valid, make sure that promotion matches the item and promotion is ongoing
        if (itemModel.getPromoModel() == null || promoId.intValue() != itemModel.getPromoModel().getId()) {
            return null;
        }
        if (promoModel.getStatus() != Constants.PROMO_ONGOING) {
            return null;
        }

        //check promo token threshold
        long result = redisTemplate.opsForValue().increment(String.format(Constants.REDIS_PROMO_COUNT_THRESHOLD, promoId), -1);
        if (result < 0) {
            return null;
        }

        //generate promo token
        String promoToken = UUID.randomUUID().toString().replaceAll("-" , "");

        //set to redis
//        String tokenKey = "promo_token_" + promoId + "_uid_" + userId + "_iid_" + itemId;
        String tokenKey = String.format(Constants.REDIS_PROMO_TOKEN, promoId, userId, itemId);
        redisTemplate.opsForValue().set(tokenKey, promoToken);
        redisTemplate.expire(tokenKey, 5, TimeUnit.MINUTES);

        return promoToken;
    }


    private void setPromoModelStatus(PromoModel promoModel) {
        if (promoModel.getStartDate().isAfterNow()) {
            promoModel.setStatus(Constants.PROMO_NOT_STARTED);
        } else if (promoModel.getEndDate().isBeforeNow()) {
            promoModel.setStatus(Constants.PROMO_ENDED);
        } else {
            promoModel.setStatus(Constants.PROMO_ONGOING);
        }
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
