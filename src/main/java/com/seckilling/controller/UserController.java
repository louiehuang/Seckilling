package com.seckilling.controller;

import com.seckilling.controller.viewobject.UserVO;
import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.response.CommonReturnType;
import com.seckilling.service.UserService;
import com.seckilling.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller("user")
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name="id") Integer id) throws BusinessException {
        // http://localhost:9000/user/get?id=1
        UserModel userModel = userService.getUserById(id);

        if (userModel== null) {
            throw new BusinessException(EBusinessError.USER_NOT_EXIST);
        }

        UserVO userVO = convertFromModel(userModel);
        return CommonReturnType.create(userVO);
    }

    private UserVO convertFromModel(UserModel userModel) {
        if (userModel == null)
            return null;
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }

}
