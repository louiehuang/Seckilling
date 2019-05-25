package com.seckilling;

import com.seckilling.dao.UserDOMapper;
import com.seckilling.dataobject.UserDO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Hello world!
 *
 */
@SpringBootApplication(scanBasePackages = {"com.seckilling"})
@RestController
@MapperScan(basePackages = {"com.seckilling.dao"})
public class App {

//    @Autowired
    @Resource
    private UserDOMapper userDOMapper;

    public static void main( String[] args ) {
        SpringApplication.run(App.class, args);
    }

    @RequestMapping("/")
    public String home() {
        UserDO userDO = userDOMapper.selectByPrimaryKey(1);
        if (userDO == null) {
            return "User not exist";
        } else {
            return "User name: " + userDO.getName();
        }
    }
}
