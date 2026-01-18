package com.retail.exception;


public class InsufficientStockException extends RetailException {
    
    private final Integer productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(Integer productId, int requested, int available) {
        super(String.format("Недостаточно товара (ID: %d). Запрошено: %d, Доступно: %d", 
                productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Integer getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
