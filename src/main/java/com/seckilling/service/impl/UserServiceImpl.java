package com.seckilling.service.impl;

import com.seckilling.dao.UserDOMapper;
import com.seckilling.dao.UserPasswordDOMapper;
import com.seckilling.dataobject.UserDO;
import com.seckilling.dataobject.UserPasswordDO;
import com.seckilling.error.BusinessException;
import com.seckilling.error.EBusinessError;
import com.seckilling.service.UserService;
import com.seckilling.service.model.UserModel;
import com.seckilling.validator.ValidationResult;
import com.seckilling.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class UserServiceImpl implements UserService {

//    @Autowired
    @Resource
    private UserDOMapper userDOMapper;

    @Resource
    private UserPasswordDOMapper userPasswordDOMapper;

    @Resource
    private ValidatorImpl validator;


    @Override
    public UserModel getUserById(Integer id) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        if (userDO == null)
            return null;
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(id);

        return convertFromDataObject(userDO, userPasswordDO);
    }


    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        if (userModel == null) {
            throw new BusinessException(EBusinessError.PARAMTER_NOT_VALID);
        }

        ValidationResult validationResult = validator.validate(userModel);
        if (validationResult.isHasError()) {
            throw new BusinessException(EBusinessError.PARAMTER_NOT_VALID, validationResult.getErrMsg());
        }

        //Transactional register
        UserDO userDO = convertFromModel(userModel);
        try {
            userDOMapper.insertSelective(userDO);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(EBusinessError.PARAMTER_NOT_VALID, "cellphone number already registered");
        }

        userModel.setId(userDO.getId());
        UserPasswordDO userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);
    }


    @Override
    public UserModel validateLogin(String cellphone, String encryptedPassword) throws BusinessException {
        //get user info based on cellphone number
        UserDO userDO = userDOMapper.selectByCellphone(cellphone);
        if (userDO == null) {
            throw new BusinessException(EBusinessError.USER_LOGIN_FAIL);
        }

        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());

        UserModel userModel = convertFromDataObject(userDO, userPasswordDO);

        //check whether password matches
        if (!StringUtils.equals(encryptedPassword, userModel.getEncryptedPassword())) {
            throw new BusinessException(EBusinessError.USER_LOGIN_FAIL);
        }

        return userModel;
    }


    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO) {
        if (userDO == null)
            return null;
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);

        if (userPasswordDO != null)
            userModel.setEncryptedPassword(userPasswordDO.getEncryptedPassword());
        return userModel;
    }


    private UserDO convertFromModel(UserModel userModel) {
        if (userModel == null)
            return null;
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }


    private UserPasswordDO convertPasswordFromModel(UserModel userModel) {
        if (userModel == null)
            return null;
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        userPasswordDO.setEncryptedPassword(userModel.getEncryptedPassword());
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }

}
