package com.seckilling.common;

public final class Constants {
    private Constants() {

    }

    public static final int MAX_QUANTITY = 100;
    public static final String ORDER_AUTO_SEQUENCE = "order_info";

    public static final int NO_PROMO = 0;
    public static final int PROMO_NOT_STARTED = 1;
    public static final int PROMO_ONGOING = 2;
    public static final int PROMO_ENDED = 3;

    public static final int STOCK_LOG_INIT = 1;
    public static final int STOCK_LOG_DEDUCT_OK = 2;
    public static final int STOCK_LOG_ROLL_BACK = 3;


    //exceptions
    public static final String USER_NOT_EXIST = "User does not exist";
    public static final String ITEM_NOT_EXIST = "Item does not exist";
    public static final String QUANTITY_NOT_VALID = "Quantity is not valid";

    public static final String PROMOTION_INFO_NOT_CORRECT = "Promotion info is not correct";
    public static final String PROMOTION_NOT_STARTED = "Promotion has not started yet";


    //redis keys
    public static final String REDIS_ITEM = "item_%d";  //used for item_get interface
    public static final String REDIS_ITEM_VALIDATE = "item_validate_%d";  //used when generating second kill token and creating order
    public static final String REDIS_USER_VALIDATE = "user_validate_%d";  //used when generating second kill token
    public static final String REDIS_PROMO_OUT_OF_STOCK = "promo_item_out_of_stock_%d";
    public static final String REDIS_OTP = "otp_%s";
    public static final String REDIS_PROMO_ITEM_STOCK = "promo_item_stock_%d";
    public static final String REDIS_PROMO_COUNT_THRESHOLD = "promo_count_threshold_%d";
    public static final String REDIS_PROMO_TOKEN = "promo_token_%d_uid_%d_iid_%d";
    public static final String REDIS_CAPTCHA = "captcha_%d";



}