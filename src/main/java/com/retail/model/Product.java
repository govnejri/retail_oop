package com.retail.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class Product {
    private Integer id;
    private String sku;
    private String name;
    private String description;
    private Integer categoryId;
    private Integer unitId;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private Integer minStockLevel;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String categoryName;
    private String unitName;
    private Integer stockQuantity;

    public Product() {
        this.active = true;
        this.minStockLevel = 0;
        this.purchasePrice = BigDecimal.ZERO;
    }

    public Product(String sku, String name, BigDecimal sellingPrice) {
        this();
        this.sku = sku;
        this.name = name;
        this.sellingPrice = sellingPrice;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Integer getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(Integer minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public boolean isLowStock() {
        return stockQuantity != null && stockQuantity <= minStockLevel;
    }

    @Override
    public String toString() {
        return String.format("Product{id=%d, sku='%s', name='%s', price=%s, stock=%d}",
                id, sku, name, sellingPrice, stockQuantity != null ? stockQuantity : 0);
    }
}
