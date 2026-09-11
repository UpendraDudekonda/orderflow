package com.orderflow.order.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.orderflow.order.entity.ProcessedEvent;
import com.orderflow.order.repository.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isProcessed(
            String eventId,
            String consumerName) {

        return processedEventRepository
                .findByEventIdAndConsumerName(
                        eventId,
                        consumerName
                )
                .isPresent();
    }

    public void markAsProcessed(
            String eventId,
            String consumerName) {

        ProcessedEvent processedEvent =
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .consumerName(consumerName)
                        .processedAt(LocalDateTime.now())
                        .build();

        processedEventRepository.save(processedEvent);
    }
}