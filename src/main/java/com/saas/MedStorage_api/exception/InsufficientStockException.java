package com.saas.MedStorage_api.exception;

public class InsufficientStockException extends BadRequestException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
