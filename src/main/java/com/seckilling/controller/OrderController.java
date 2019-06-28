package com.seckilling.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.seckilling.common.Constants;
import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.mq.MQProducer;
import com.seckilling.response.CommonReturnType;
import com.seckilling.service.ItemService;
import com.seckilling.service.OrderService;
import com.seckilling.service.PromoService;
import com.seckilling.service.model.UserModel;
import com.seckilling.util.CaptchaUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;


@Controller("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true", origins = {"*"})
public class OrderController extends BaseController {

    @Resource
    private HttpServletRequest httpServletRequest;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private MQProducer producer;

    @Resource
    private ItemService itemService;

    @Resource
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;


    @PostConstruct
    public void init() {
        //create a congestion window whose size = 30, at most 30 requests are handled at the same time
        executorService = Executors.newFixedThreadPool(30);

        orderCreateRateLimiter = RateLimiter.create(300.0);  //limit on a single server
    }


    @RequestMapping(value = "/generateCaptcha", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public void generateCaptcha(HttpServletResponse response)
            throws BusinessException, IOException {
        //get user login info
        String token = httpServletRequest.getParameterMap().get("token")[0];  //get token from URL params
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EBusinessError.USER_NOT_LOGIN, "User has not logged in, cannot create captcha");
        }

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(EBusinessError.USER_NOT_LOGIN, "User has not logged in, cannot create generate promo token");
        }

        Map<String,Object> map = CaptchaUtil.generateCodeAndPic();
        redisTemplate.opsForValue().set(String.format(Constants.REDIS_CAPTCHA, userModel.getId()), map.get("captcha"));
        redisTemplate.expire(String.format(Constants.REDIS_CAPTCHA, userModel.getId()), 5, TimeUnit.MINUTES);
        ImageIO.write((RenderedImage) map.get("captchaPic"), "jpeg", response.getOutputStream());

        System.out.println("Captcha：" + map.get("captcha"));
    }


    @RequestMapping(value = "/generateToken", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(name="itemId") Integer itemId,
                                          @RequestParam(name="promoId") Integer promoId,
                                          @RequestParam(name="captcha") String captcha)
            throws BusinessException {
        //get user login info
        String token = httpServletRequest.getParameterMap().get("token")[0];  //get token from URL params
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EBusinessError.USER_NOT_LOGIN, "User has not logged in, cannot create generate promo token");
        }

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(EBusinessError.USER_NOT_LOGIN, "User has not logged in, cannot create generate promo token");
        }

        //check captcha
        String captchaInRedis = (String) redisTemplate.opsForValue().get(String.format(Constants.REDIS_CAPTCHA, userModel.getId()));
        if (StringUtils.isEmpty(captchaInRedis)) {
            throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID, "Invalid request");
        }
        if (!captcha.equalsIgnoreCase(captchaInRedis)) {
            throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID, "Wrong captcha");
        }

        //generate promo token
        String promoToken = promoService.generateSecondKillToken(userModel.getId(), itemId, promoId);
        if (promoToken == null) {
            throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID, "Generating token failed");
        }

        return CommonReturnType.create(promoToken);
    }


    /**
     * Logic: OrderController:createOrder() -> MQProducer:transactionAsyncDeductStock() -> OrderService:createOder()
     */
    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name="itemId") Integer itemId,
                                        @RequestParam(name="quantity") Integer quantity,
                                        @RequestParam(name="promoId", required = false) Integer promoId,
                                        @RequestParam(name="promoToken", required = false) String promoToken)
            throws BusinessException {

        if (!orderCreateRateLimiter.tryAcquire()) {
            throw new BusinessException(EBusinessError.REACH_RATE_LIMIT);
        }

        //get user login info
        String userToken = httpServletRequest.getParameterMap().get("token")[0];  //get token from URL params
        if (StringUtils.isEmpty(userToken)) {
            throw new BusinessException(EBusinessError.USER_NOT_LOGIN, "User has not logged in, cannot create an order");
        }

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(userToken);
        if (userModel == null) {
            throw new BusinessException(EBusinessError.USER_NOT_LOGIN, "User has not logged in, cannot create an order");
        }

        //check promo token
        if (promoId != null) {
            String tokenKey = String.format(Constants.REDIS_PROMO_TOKEN, promoId, userModel.getId(), itemId);
            String promoTokenInRedis = (String) redisTemplate.opsForValue().get(tokenKey);
            if (promoTokenInRedis == null || !StringUtils.equals(promoTokenInRedis, promoToken)) {
                throw new BusinessException(EBusinessError.PARAMETER_NOT_VALID, "Invalid promo token");
            }
        }

        //submit and wait
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                // init stock log before creating order (used to track order status)
                String stockLogId = itemService.initStockLog(itemId, quantity);

                // create order and send msg in producer
                if (!producer.transactionAsyncDeductStock(userModel.getId(), itemId, quantity, promoId, stockLogId)) {
                    throw new BusinessException(EBusinessError.UNKNOWN_ERROR, "Creating order failed");
                }
                return null;
            }
        });

        try {
            future.get();  //block
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new BusinessException(EBusinessError.UNKNOWN_ERROR);
        }

        return CommonReturnType.create(null);
    }

}
