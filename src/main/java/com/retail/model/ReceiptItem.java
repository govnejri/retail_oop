package com.retail.model;

import java.math.BigDecimal;


public class ReceiptItem {
    private Integer id;
    private Integer receiptId;
    private Integer productId;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal lineTotal;


    private String productSku;
    private String productName;

    public ReceiptItem() {
    }

    public ReceiptItem(Integer productId, Integer quantity, BigDecimal purchasePrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.lineTotal = purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Integer receiptId) {
        this.receiptId = receiptId;
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
        recalculateLineTotal();
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
        recalculateLineTotal();
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
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

    private void recalculateLineTotal() {
        if (purchasePrice != null && quantity != null) {
            this.lineTotal = purchasePrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @Override
    public String toString() {
        return String.format("ReceiptItem{productId=%d, qty=%d, price=%s, total=%s}",
                productId, quantity, purchasePrice, lineTotal);
    }
}
