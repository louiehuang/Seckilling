package com.seckilling.common;

public final class Constants {
    private Constants() {

    }

    public static final int MAX_QUANTITY = 100;
    public static final String ORDER_AUTO_SEQUENCE = "order_info";

    public static final String IS_LOGIN = "IS_LOGIN";
    public static final String LOGIN_USER = "LOGIN_USER";

    public static final int NO_PROMO = 0;
    public static final int PROMO_NOT_STARTED = 1;
    public static final int PROMO_ONGOING = 2;
    public static final int PROMO_ENDED = 3;

    //exceptions
    public static final String USER_NOT_EXIST = "User does not exist";
    public static final String ITEM_NOT_EXIST = "Item does not exist";
    public static final String QUANTITY_NOT_VALID = "Quantity is not valid";

    public static final String PROMOTION_INFO_NOT_CORRECT = "Promotion info is not correct";
    public static final String PROMOTION_NOT_STARTED = "Promotion has not started yet";


}