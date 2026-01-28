package com.retail.util;

import com.retail.exception.ValidationException;

import java.math.BigDecimal;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Validator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern SKU_PATTERN =
            Pattern.compile("^[A-Za-z0-9-]+$");
    private static final Pattern LOGIN_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,50}$");

    private Validator() {
    }

    // Lambda валидаторы
    public static final Predicate<String> NOT_EMPTY =
            value -> value != null && !value.trim().isEmpty();

    public static final Predicate<BigDecimal> POSITIVE_AMOUNT =
            amount -> amount != null && amount.compareTo(BigDecimal.ZERO) > 0;

    public static final Predicate<BigDecimal> NON_NEGATIVE_AMOUNT =
            amount -> amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;

    public static final Predicate<Integer> POSITIVE_INTEGER =
            value -> value != null && value > 0;

    public static final Predicate<Integer> NON_NEGATIVE_INTEGER =
            value -> value != null && value >= 0;

    public static <T> void validate(T value, Predicate<T> predicate, String errorMessage) {
        if (!predicate.test(value)) {
            throw new ValidationException(errorMessage);
        }
    }

    public static void requireNotEmpty(String value, String fieldName) {
        validate(value, NOT_EMPTY, fieldName + " не может быть пустым");
    }

    public static void validateEmail(String email) {
        if (email != null && !email.isEmpty()) {
            validate(email, e -> EMAIL_PATTERN.matcher(e).matches(),
                    "Некорректный формат email");
        }
    }

    public static void validateSku(String sku) {
        requireNotEmpty(sku, "Артикул");
        validate(sku, s -> SKU_PATTERN.matcher(s).matches(),
                "Артикул может содержать только буквы, цифры и дефис");
        validate(sku, s -> s.length() >= 3 && s.length() <= 50,
                "Длина артикула должна быть от 3 до 50 символов");
    }

    public static void validateLogin(String login) {
        requireNotEmpty(login, "Логин");
        validate(login, l -> LOGIN_PATTERN.matcher(l).matches(),
                "Логин должен содержать 3-50 символов (буквы, цифры, подчеркивание)");
    }

    public static void validatePassword(String password) {
        requireNotEmpty(password, "Пароль");
        validate(password, p -> p.length() >= 6,
                "Пароль должен содержать минимум 6 символов");
    }

    public static void validatePrice(BigDecimal price, String fieldName) {
        validate(price, POSITIVE_AMOUNT, fieldName + " должна быть положительной");
    }

    public static void validateQuantity(Integer quantity, String fieldName) {
        validate(quantity, POSITIVE_INTEGER, fieldName + " должно быть положительным числом");
    }

    public static void validateLength(String value, int minLen, int maxLen, String fieldName) {
        if (value != null) {
            validate(value, v -> v.length() >= minLen && v.length() <= maxLen,
                    String.format("%s должен быть от %d до %d символов", fieldName, minLen, maxLen));
        }
    }

    public static void validateId(Integer id, String fieldName) {
        validate(id, POSITIVE_INTEGER, fieldName + " должен быть положительным числом");
    }
}
