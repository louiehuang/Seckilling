package com.seckilling.controller;

import com.seckilling.common.Constants;
import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.response.CommonReturnType;
import com.seckilling.service.OrderService;
import com.seckilling.service.model.OrderModel;
import com.seckilling.service.model.UserModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@Controller("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true", origins = {"*"})
public class OrderController extends BaseController {

    @Resource
    private OrderService orderService;

    @Resource
    private HttpServletRequest httpServletRequest;


    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name="itemId") Integer itemId,
                                        @RequestParam(name="quantity") Integer quantity,
                                        @RequestParam(name="promoId") Integer promoId)
            throws BusinessException {
        //get user login info
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute(Constants.IS_LOGIN);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EBusinessError.USER_NOT_LOGIN);
        }
        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute(Constants.LOGIN_USER);

        OrderModel orderModel = orderService.createOder(userModel.getId(), itemId, quantity, promoId);

        return CommonReturnType.create(null);
    }

}
