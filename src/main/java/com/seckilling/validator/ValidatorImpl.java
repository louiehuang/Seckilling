package com.seckilling.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

@Component
public class ValidatorImpl implements InitializingBean {

    private Validator validator;

    public ValidationResult validate(Object bean) {
        ValidationResult result = new ValidationResult();
        Set<ConstraintViolation<Object>> constraintViolationSet = validator.validate(bean);
        if (constraintViolationSet.size() > 0) {
            result.setHasError(true);
            for (ConstraintViolation constraintViolation : constraintViolationSet) {
                String propertyName = constraintViolation.getPropertyPath().toString();
                String errMsg = constraintViolation.getMessage();
                result.getErrMsgMap().put(propertyName, errMsg);
            }
        }
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // instantialize hibernate validator through factory
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
}
