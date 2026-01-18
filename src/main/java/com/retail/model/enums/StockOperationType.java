package com.retail.model.enums;


public enum StockOperationType {
    RECEIPT("Приход"),
    SALE("Продажа"),
    RETURN("Возврат"),
    ADJUSTMENT("Корректировка"),
    WRITE_OFF("Списание");

    private final String displayName;

    StockOperationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static StockOperationType fromString(String value) {
        for (StockOperationType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Неизвестный тип операции: " + value);
    }
}
