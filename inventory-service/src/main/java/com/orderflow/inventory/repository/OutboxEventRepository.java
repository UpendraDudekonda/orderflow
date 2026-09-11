package com.orderflow.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderflow.inventory.entity.OutboxEvent;
import com.orderflow.inventory.entity.OutboxStatus;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );
}