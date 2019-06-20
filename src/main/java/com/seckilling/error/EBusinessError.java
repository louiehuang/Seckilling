package com.seckilling.error;

public enum EBusinessError implements CommonError {
    //Common errors
    PARAMETER_NOT_VALID(10001, "Parameter is not valid"),
    UNKNOWN_ERROR(10002, "Unknown error"),

    //User errors
    USER_NOT_EXIST(20001, "User does not exist"),
    USER_LOGIN_FAIL(20002, "Wrong cellphone number or password"),
    USER_NOT_LOGIN(20003, "User has not logged in"),

    //Order errors
    STOCK_NOT_ENOUGH(30001, "Stock is not enough"),

    //MQ errors
    MQ_SEND_FAIL(40001, "Message sending failed"),

    ;

    private int errCode;
    private String errMsg;

    EBusinessError(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
