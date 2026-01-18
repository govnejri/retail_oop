package com.retail.exception;


public class RetailException extends RuntimeException {
    
    public RetailException(String message) {
        super(message);
    }

    public RetailException(String message, Throwable cause) {
        super(message, cause);
    }
}
