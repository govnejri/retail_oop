package com.retail.model;

import java.time.LocalDateTime;


public class Inventory {
    private Integer id;
    private Integer productId;
    private Integer quantity;
    private Integer reserved;
    private LocalDateTime updatedAt;

    private String productSku;
    private String productName;

    public Inventory() {
        this.quantity = 0;
        this.reserved = 0;
    }

    public Inventory(Integer productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.reserved = 0;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getReserved() {
        return reserved;
    }

    public void setReserved(Integer reserved) {
        this.reserved = reserved;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

   
    public Integer getAvailable() {
        return quantity - reserved;
    }

    @Override
    public String toString() {
        return String.format("Inventory{productId=%d, quantity=%d, reserved=%d, available=%d}",
                productId, quantity, reserved, getAvailable());
    }
}
