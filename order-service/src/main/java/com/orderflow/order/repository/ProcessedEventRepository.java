package com.orderflow.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderflow.order.entity.ProcessedEvent;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, Long> {

    Optional<ProcessedEvent> findByEventIdAndConsumerName(
            String eventId,
            String consumerName
    );
}