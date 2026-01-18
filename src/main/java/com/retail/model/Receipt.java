package com.retail.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Receipt {
    private Integer id;
    private String receiptNumber;
    private String supplierInfo;
    private Integer managerId;
    private LocalDateTime receiptDate;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime createdAt;


    private String managerName;
    private List<ReceiptItem> items;

    public Receipt() {
        this.totalAmount = BigDecimal.ZERO;
        this.items = new ArrayList<>();
    }

    public Receipt(Integer managerId) {
        this();
        this.managerId = managerId;
        this.receiptDate = LocalDateTime.now();
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getSupplierInfo() {
        return supplierInfo;
    }

    public void setSupplierInfo(String supplierInfo) {
        this.supplierInfo = supplierInfo;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public LocalDateTime getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDateTime receiptDate) {
        this.receiptDate = receiptDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public List<ReceiptItem> getItems() {
        return items;
    }

    public void setItems(List<ReceiptItem> items) {
        this.items = items;
    }

    public void addItem(ReceiptItem item) {
        items.add(item);
        recalculateTotal();
    }

    
    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(ReceiptItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return String.format("Receipt{id=%d, number='%s', date=%s, amount=%s}",
                id, receiptNumber, receiptDate, totalAmount);
    }
}
