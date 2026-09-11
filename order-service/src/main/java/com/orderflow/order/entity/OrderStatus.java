package com.orderflow.order.entity;

public enum OrderStatus {

    CREATED,

    INVENTORY_RESERVED,

    PAYMENT_PENDING,

    CONFIRMED,

    INVENTORY_FAILED,

    PAYMENT_FAILED,

    CANCELLED
}