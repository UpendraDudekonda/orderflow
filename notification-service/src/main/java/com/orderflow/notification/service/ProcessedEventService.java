package com.orderflow.notification.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderflow.notification.entity.ProcessedEvent;
import com.orderflow.notification.repository.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository repository;

    public boolean isProcessed(
            String eventId,
            String consumerName) {

        return repository.existsByEventIdAndConsumerName(
                eventId,
                consumerName
        );
    }

    @Transactional
    public void markAsProcessed(
            String eventId,
            String consumerName) {

        repository.save(
            ProcessedEvent.builder()
                    .eventId(eventId)
                    .consumerName(consumerName)
                    .processedAt(LocalDateTime.now())
                    .build()
        );
    }
}