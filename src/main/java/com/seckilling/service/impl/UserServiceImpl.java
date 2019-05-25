package com.seckilling.service.impl;

import com.seckilling.dao.UserDOMapper;
import com.seckilling.dao.UserPasswordDOMapper;
import com.seckilling.dataobject.UserDO;
import com.seckilling.dataobject.UserPasswordDO;
import com.seckilling.service.UserService;
import com.seckilling.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserServiceImpl implements UserService {

//    @Autowired
    @Resource
    private UserDOMapper userDOMapper;

    @Resource
    private UserPasswordDOMapper userPasswordDOMapper;

    @Override
    public UserModel getUserById(Integer id) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        if (userDO == null)
            return null;
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(id);

        return convertFromDataObject(userDO, userPasswordDO);
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

}
