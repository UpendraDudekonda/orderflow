package com.orderflow.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderflow.order.entity.OrderItem;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {
}