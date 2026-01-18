package com.retail.exception;


public class DatabaseException extends RetailException {
    
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    
    public static DatabaseException connectionLost() {
        return new DatabaseException("Нет связи с сервером базы данных. Проверьте подключение.");
    }
}
