package com.seckilling.controller;

import com.seckilling.controller.viewobject.ItemVO;
import com.seckilling.error.BusinessException;
import com.seckilling.response.CommonReturnType;
import com.seckilling.service.ItemService;
import com.seckilling.service.model.ItemModel;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Controller("item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true", origins = {"*"})
public class ItemController extends BaseController {

    @Resource
    private ItemService itemService;

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
        ItemVO itemVO = convertItemModelTOItemVO(res);

        return CommonReturnType.create(itemVO);
    }

    //Item details page
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name="id") Integer id) {
        ItemModel itemModel = itemService.getItemById(id);

        ItemVO itemVO = convertItemModelTOItemVO(itemModel);

        return CommonReturnType.create(itemVO);
    }


    @RequestMapping(value = "/getAll", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getAllItems() {
        List<ItemModel> itemModelList = itemService.getAllItems();
        List<ItemVO> itemVOList = itemModelList.stream()
                .map(this::convertItemModelTOItemVO)
                .collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }


    private ItemVO convertItemModelTOItemVO(ItemModel itemModel) {
        if (itemModel == null)
            return null;
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        return itemVO;
    }

}
