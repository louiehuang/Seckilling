package com.seckilling.controller;

import com.seckilling.common.Constants;
import com.seckilling.controller.viewobject.ItemVO;
import com.seckilling.error.BusinessException;
import com.seckilling.response.CommonReturnType;
import com.seckilling.service.CacheService;
import com.seckilling.service.ItemService;
import com.seckilling.service.PromoService;
import com.seckilling.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Controller("item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true", origins = {"*"})
public class ItemController extends BaseController {

    @Resource
    private ItemService itemService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private CacheService cacheService;

    @Resource
    private PromoService promoService;


    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name="itemName") String itemName,
                                       @RequestParam(name="description") String description,
                                       @RequestParam(name="imgUrl") String imgUrl,
                                       @RequestParam(name="price") BigDecimal price,
                                       @RequestParam(name="stock") Integer stock) throws BusinessException {
        ItemModel itemModel = new ItemModel();
        itemModel.setName(itemName);
        itemModel.setDescription(description);
        itemModel.setImgUrl(imgUrl);
        itemModel.setPrice(price);
        itemModel.setStock(stock);

        ItemModel res = itemService.createItem(itemModel);

        //object returned to frontend
        ItemVO itemVO = convertItemModelToItemVO(res);

        return CommonReturnType.create(itemVO);
    }


    /**
     * Publish promotion activity, set item stock and promo token threshold to Redis
     * @param id promotion id
     * @return success (suppose no failure here, need to fix error handling)
     */
    @RequestMapping(value = "/publishPromo", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType publishPromo(@RequestParam(name="id") Integer id) {
        // http://localhost:9000/item/publishPromo?id=1
        // http://23.239.1.241/item/publishPromo?id=1
        promoService.publishPromo(id);
        return CommonReturnType.create(null);
    }


    /**
     * Set normal item's stock info to Redis
     * @param id itemId
     * @return success
     */
    @RequestMapping(value = "/publishItem", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType publishItem(@RequestParam(name="id") Integer id) {
        // http://localhost:9000/item/publishItem?id=6
        itemService.publishItem(id);
        return CommonReturnType.create(null);
    }


    /**
     * Get item detailed info, get from (1) Local cache -> (2) Redis -> (3) DB
     * @param id item id
     * @return ItemVO
     */
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name="id") Integer id) {
        ItemModel itemModel = null;
        String itemKey = String.format(Constants.REDIS_ITEM, id);

        // 1. try to get item from local cache based on item id
        itemModel = (ItemModel) cacheService.getFromLocalCommonCache(itemKey);
        if (itemModel == null) {
            // 2. try get item from Redis based on item id if local cache misses
            itemModel = (ItemModel) redisTemplate.opsForValue().get(itemKey);
            if (itemModel == null) {
                // 3. get from DB
                itemModel = itemService.getItemById(id);
                // set Redis
                redisTemplate.opsForValue().set(itemKey, itemModel);
                redisTemplate.expire(itemKey, 10, TimeUnit.MINUTES);
            }
            // set local cache
            cacheService.setLocalCommonCache(itemKey, itemModel);
        }

        ItemVO itemVO = convertItemModelToItemVO(itemModel);

        return CommonReturnType.create(itemVO);
    }


    /**
     * This method is used for test
     * @return all items info found in DB
     */
    @RequestMapping(value = "/getAll", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getAllItems() {
        List<ItemModel> itemModelList = itemService.getAllItems();
        List<ItemVO> itemVOList = itemModelList.stream()
                .map(this::convertItemModelToItemVO)
                .collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }


    private ItemVO convertItemModelToItemVO(ItemModel itemModel) {
        if (itemModel == null)
            return null;
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);

        //has second killing promotion activity
        if (itemModel.getPromoModel() != null) {
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern(Constants.DATETIME_FORMAT)));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            itemVO.setPromoStatus(Constants.NO_PROMO);
        }

        return itemVO;
    }

}
