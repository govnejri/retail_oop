package com.retail.model;

import java.math.BigDecimal;


public class SaleItem {
    private Integer id;
    private Integer saleId;
    private Integer productId;
    private Integer quantity;
    private BigDecimal priceAtSale;
    private BigDecimal lineTotal;
    private Integer returnedQty;


    private String productSku;
    private String productName;

    public SaleItem() {
        this.returnedQty = 0;
    }

    public SaleItem(Integer productId, Integer quantity, BigDecimal priceAtSale) {
        this();
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtSale = priceAtSale;
        this.lineTotal = priceAtSale.multiply(BigDecimal.valueOf(quantity));
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
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

    public BigDecimal getPriceAtSale() {
        return priceAtSale;
    }

    public void setPriceAtSale(BigDecimal priceAtSale) {
        this.priceAtSale = priceAtSale;
        recalculateLineTotal();
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public Integer getReturnedQty() {
        return returnedQty;
    }

    public void setReturnedQty(Integer returnedQty) {
        this.returnedQty = returnedQty;
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

    
    public Integer getReturnableQty() {
        return quantity - returnedQty;
    }

    private void recalculateLineTotal() {
        if (priceAtSale != null && quantity != null) {
            this.lineTotal = priceAtSale.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @Override
    public String toString() {
        return String.format("SaleItem{productId=%d, qty=%d, price=%s, total=%s}",
                productId, quantity, priceAtSale, lineTotal);
    }
}
