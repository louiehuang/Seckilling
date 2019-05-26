package com.seckilling.validator;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ValidationResult {
    private boolean hasError;
    private Map<String, String> errMsgMap;

    public ValidationResult() {
        this.hasError = false;
        this.errMsgMap = new HashMap<>();
    }

    public String getErrMsg() {
        return StringUtils.join(errMsgMap.values().toArray(), ",");
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public Map<String, String> getErrMsgMap() {
        return errMsgMap;
    }

    public void setErrMsgMap(Map<String, String> errMsgMap) {
        this.errMsgMap = errMsgMap;
    }
}
