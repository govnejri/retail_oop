package com.retail.model.enums;


public enum UserRole {
    ADMIN("Администратор"),
    MANAGER("Главный менеджер"),
    EMPLOYEE("Сотрудник");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserRole fromString(String value) {
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Неизвестная роль: " + value);
    }
}
