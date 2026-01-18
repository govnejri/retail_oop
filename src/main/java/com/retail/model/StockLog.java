package com.retail.model;

import com.retail.model.enums.StockOperationType;

import java.time.LocalDateTime;


public class StockLog {
    private Integer id;
    private Integer productId;
    private StockOperationType operationType;
    private Integer quantityChange;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private Integer referenceId;
    private String referenceType;
    private Integer userId;
    private String notes;
    private LocalDateTime createdAt;


    private String productSku;
    private String productName;
    private String userName;

    public StockLog() {
    }

    public StockLog(Integer productId, StockOperationType operationType, 
                    Integer quantityChange, Integer quantityBefore, Integer quantityAfter,
                    Integer userId) {
        this.productId = productId;
        this.operationType = operationType;
        this.quantityChange = quantityChange;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
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

    public StockOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(StockOperationType operationType) {
        this.operationType = operationType;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(Integer quantityChange) {
        this.quantityChange = quantityChange;
    }

    public Integer getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(Integer quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return String.format("StockLog{productId=%d, type=%s, change=%+d, before=%d, after=%d, user=%d}",
                productId, operationType, quantityChange, quantityBefore, quantityAfter, userId);
    }
}
