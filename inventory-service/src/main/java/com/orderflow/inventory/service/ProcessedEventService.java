package com.orderflow.inventory.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderflow.inventory.entity.ProcessedEvent;
import com.orderflow.inventory.repository.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isProcessed(String eventId, String consumerName) {
        return processedEventRepository
                .findByEventIdAndConsumerName(eventId, consumerName)
                .isPresent();
    }

    @Transactional
    public void markAsProcessed(String eventId, String consumerName) {

        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .consumerName(consumerName)
                .processedAt(LocalDateTime.now())
                .build();

        processedEventRepository.save(processedEvent);
    }
}