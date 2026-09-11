package com.orderflow.inventory.exception;

public class ConcurrentStockUpdateException
        extends RuntimeException {

    public ConcurrentStockUpdateException(String message) {
        super(message);
    }
}