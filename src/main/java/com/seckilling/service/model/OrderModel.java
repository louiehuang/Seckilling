package com.seckilling.service.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class OrderModel {
    private Integer id;

    //String, e.g. "2019052600394813"
    private String orderId;

    private Integer userId;

    //suppose user only buy one kind of item at a time (since our system is for second killing)
    private Integer itemId;

    private Integer quantity;  //number of items

    /**
     * if promoId is not null, then this order is created when in second killing activity
     */
    private Integer promoId;

    /**
     * if promoId is not null, the price for this item is the promotion price
     */
    private BigDecimal itemPrice;  //redundant field, price of the item when this order created (price may change)

    /**
     * if promoId is not null, the total price of this order is calculated by promotion price
     */
    private BigDecimal orderPrice;

    private Timestamp orderTimestamp;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPromoId() {
        return promoId;
    }

    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public BigDecimal getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(BigDecimal orderPrice) {
        this.orderPrice = orderPrice;
    }

    public Timestamp getOrderTimestamp() {
        return orderTimestamp;
    }

    public void setOrderTimestamp(Timestamp orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }
}
