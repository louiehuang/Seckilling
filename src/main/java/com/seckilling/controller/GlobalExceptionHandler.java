package com.seckilling.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.response.CommonReturnType;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;


@ControllerAdvice
public class GlobalExceptionHandler{
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public CommonReturnType doError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Exception ex) {
        ex.printStackTrace();
        Map<String,Object> responseData = new HashMap<>();
        if( ex instanceof BusinessException){
            BusinessException businessException = (BusinessException) ex;
            responseData.put("errCode",businessException.getErrCode());
            responseData.put("errMsg",businessException.getErrMsg());
        }else if(ex instanceof ServletRequestBindingException){
            responseData.put("errCode", EBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg", "URL binding failed");
        }else if(ex instanceof NoHandlerFoundException){
            responseData.put("errCode",EBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg","No corresponding URL found");
        }else{
            responseData.put("errCode", EBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg",EBusinessError.UNKNOWN_ERROR.getErrMsg());
        }
        return CommonReturnType.create(responseData,"fail");
    }
}
