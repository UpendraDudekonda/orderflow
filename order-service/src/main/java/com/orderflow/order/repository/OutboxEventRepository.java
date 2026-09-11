package com.orderflow.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderflow.order.entity.OutboxEvent;
import com.orderflow.order.entity.OutboxStatus;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent>
    findTop50ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );
}