package com.seckilling.controller;

import com.seckilling.common.Constants;
import com.seckilling.controller.viewobject.ItemVO;
import com.seckilling.error.BusinessException;
import com.seckilling.response.CommonReturnType;
import com.seckilling.service.CacheService;
import com.seckilling.service.ItemService;
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

        //return to frontend
        ItemVO itemVO = convertItemModelToItemVO(res);

        return CommonReturnType.create(itemVO);
    }

    //Item details page
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name="id") Integer id) {
        ItemModel itemModel = null;
        String itemKey = "item_" + id;

        // 1. try to get item from local cache based on item id
        itemModel = (ItemModel) cacheService.getFromCommonCache(itemKey);
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
            cacheService.setCommonCache(itemKey, itemModel);
        }

        ItemVO itemVO = convertItemModelToItemVO(itemModel);

        return CommonReturnType.create(itemVO);
    }


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
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            itemVO.setPromoStatus(Constants.NO_PROMO);
        }

        return itemVO;
    }

}
