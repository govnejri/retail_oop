package com.retail.model.enums;


public enum UserStatus {
    ACTIVE("Активен"),
    BLOCKED("Заблокирован"),
    DELETED("Удален");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserStatus fromString(String value) {
        for (UserStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Неизвестный статус: " + value);
    }
}
