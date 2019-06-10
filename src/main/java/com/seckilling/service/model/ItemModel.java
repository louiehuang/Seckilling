package com.seckilling.service.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

public class ItemModel implements Serializable {

    private Integer id;

    @NotBlank(message = "item name cannot be empty")
    private String name;

    @NotBlank(message = "description cannot be empty")
    private String description;

    @NotBlank(message = "image url cannot be empty")
    private String imgUrl;

    @NotNull(message = "price cannot be null")
    @Min(value = 0, message = "price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "stock cannot be null")
    private Integer stock;

    //Do not checks sales field since this is not a param needed when creating new item
    private Integer sales;

    /**
     * if promoModel is not null, then this item has an promotion activity (either has not started or is ongoing)
     */
    private PromoModel promoModel;


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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public PromoModel getPromoModel() {
        return promoModel;
    }

    public void setPromoModel(PromoModel promoModel) {
        this.promoModel = promoModel;
    }
}
