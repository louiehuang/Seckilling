package com.seckilling.service.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class UserModel {
    private Integer id;

    //@NotNull: object is not null, but can be empty.
    //@NotEmpty: object is not null and size > 0.
    //@NotBlank: The string is not null and the trimmed length > 0.

    @NotBlank(message = "user name cannot be empty")
    private String name;

    @NotNull(message = "gender cannot be null")
    private Byte gender;

    @NotNull(message = "age cannot be null")
    @Min(value = 0, message = "age should be greater than 0")
    @Max(value = 150, message = "age should be less than 150")
    private Integer age;

    @NotBlank(message = "cellphone cannot be empty")
    private String cellphone;

    private String registerMode;

    private String thirdPartyId;

    @NotBlank(message = "password cannot be empty")
    private String encryptedPassword;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getGender() {
        return gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getCellphone() {
        return cellphone;
    }

    public void setCellphone(String cellphone) {
        this.cellphone = cellphone;
    }

    public String getRegisterMode() {
        return registerMode;
    }

    public void setRegisterMode(String registerMode) {
        this.registerMode = registerMode;
    }

    public String getThirdPartyId() {
        return thirdPartyId;
    }

    public void setThirdPartyId(String thirdPartyId) {
        this.thirdPartyId = thirdPartyId;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }
}
