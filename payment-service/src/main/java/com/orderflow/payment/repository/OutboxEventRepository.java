package com.orderflow.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderflow.payment.entity.OutboxEvent;
import com.orderflow.payment.entity.OutboxStatus;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );
}