package com.seckilling.controller;

import com.alibaba.druid.util.StringUtils;
import com.seckilling.common.Constants;
import com.seckilling.controller.viewobject.UserVO;
import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.response.CommonReturnType;
import com.seckilling.service.UserService;
import com.seckilling.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    // httpServletRequest is a bean (singleton pattern), how can it support concurrency access by multiple users?
    // This httpServletRequest wrapped by Spring bean is a proxy in essence, so it has threadLocal for each user request
    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;


    @RequestMapping(value = "/getotp", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name="cellphone") String cellphone) {
        // generate otp
        Random random = new Random();
        // rand.nextInt(MAX - MIN + 1) + MIN => [MIN, MAX]
        int randomInt = random.nextInt(90000) + 10000;  // [10000, 99999]
        String otpCode = String.valueOf(randomInt);

        // bind otp and cellphone number
        String optKey = String.format(Constants.REDIS_OTP, cellphone);
        redisTemplate.opsForValue().set(optKey, otpCode);
        redisTemplate.expire(optKey, 5, TimeUnit.MINUTES);

        // TODO: send otp to user
        System.out.println("cellphone=" + cellphone + ", otpCode=" + otpCode);

        return CommonReturnType.create(null);
    }


    @RequestMapping(value = "/register", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name="username") String userName,
                                     @RequestParam(name="gender") String gender,
                                     @RequestParam(name="age") String age,
                                     @RequestParam(name="cellphone") String cellphone,
                                     @RequestParam(name="otpCode") String otpCode,
                                     @RequestParam(name="password") String password)
            throws BusinessException, NoSuchAlgorithmException {

        String optKey = String.format(Constants.REDIS_OTP, cellphone);
        String otpCodeInRedis = (String) redisTemplate.opsForValue().get(optKey);
        if (!StringUtils.equals(otpCode, otpCodeInRedis)) {
            throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID, "otp code not match");
        }

        UserModel userModel = new UserModel();
        userModel.setName(userName);
        userModel.setGender(Byte.valueOf(gender));
        userModel.setAge(Integer.valueOf(age));
        userModel.setCellphone(cellphone);
        userModel.setRegisterMode("phone");
        userModel.setEncryptedPassword(encodeByMD5(password));

        userService.register(userModel);
        return CommonReturnType.create(null);
    }


    @RequestMapping(value = "/login", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name="cellphone") String cellphone,
                                  @RequestParam(name="password") String password)
            throws BusinessException, NoSuchAlgorithmException {
        if (StringUtils.isEmpty(cellphone) || StringUtils.isEmpty(password)) {
            throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID);
        }

        UserModel userModel = userService.validateLogin(cellphone, encodeByMD5(password));

        //add to session if login succeed
        //generate token, UUID
        String uuidToken = UUID.randomUUID().toString().replaceAll("-", "");

        //construct connection between token and the login status of user
        redisTemplate.opsForValue().set(uuidToken, userModel);
        redisTemplate.expire(uuidToken, 1, TimeUnit.HOURS);

//        this.httpServletRequest.getSession().setAttribute(Constants.IS_LOGIN, true);
//        this.httpServletRequest.getSession().setAttribute(Constants.LOGIN_USER, userModel);

        return CommonReturnType.create(uuidToken);
    }


    public String encodeByMD5(String str) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();
        return base64Encoder.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
    }


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
