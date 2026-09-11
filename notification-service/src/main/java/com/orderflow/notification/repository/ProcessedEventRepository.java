package com.orderflow.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderflow.notification.entity.ProcessedEvent;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventIdAndConsumerName(
            String eventId,
            String consumerName);
}