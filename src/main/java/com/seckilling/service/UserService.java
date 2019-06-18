package com.seckilling.service;

import com.seckilling.error.BusinessException;
import com.seckilling.service.model.UserModel;

public interface UserService {

    UserModel getUserById(Integer id);

    UserModel getUserByIdFromCache(Integer id);

    void register(UserModel userModel) throws BusinessException;

    UserModel validateLogin(String cellphone, String encryptedPassword) throws BusinessException ;
}
