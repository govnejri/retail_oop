package com.retail.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Sale {
    private Integer id;
    private String saleNumber;
    private Integer employeeId;
    private LocalDateTime saleDate;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private BigDecimal finalAmount;
    private boolean returned;
    private LocalDateTime createdAt;


    private String employeeName;
    private List<SaleItem> items;

    public Sale() {
        this.totalAmount = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
        this.finalAmount = BigDecimal.ZERO;
        this.returned = false;
        this.items = new ArrayList<>();
    }

    public Sale(Integer employeeId) {
        this();
        this.employeeId = employeeId;
        this.saleDate = LocalDateTime.now();
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSaleNumber() {
        return saleNumber;
    }

    public void setSaleNumber(String saleNumber) {
        this.saleNumber = saleNumber;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public boolean isReturned() {
        return returned;
    }

    public void setReturned(boolean returned) {
        this.returned = returned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    public void addItem(SaleItem item) {
        items.add(item);
        recalculateTotal();
    }

    public void removeItem(SaleItem item) {
        items.remove(item);
        recalculateTotal();
    }

    
    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(SaleItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.finalAmount = totalAmount.subtract(discount != null ? discount : BigDecimal.ZERO);
    }

    @Override
    public String toString() {
        return String.format("Sale{id=%d, number='%s', date=%s, amount=%s}",
                id, saleNumber, saleDate, finalAmount);
    }
}
