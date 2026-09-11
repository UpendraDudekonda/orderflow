package com.orderflow.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.orderflow.payment.entity.ProcessedEvent;
import com.orderflow.payment.repository.ProcessedEventRepository;

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

    @Transactional
    public void markAsProcessed(
            String eventId,
            String consumerName) {

        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .consumerName(consumerName)
                        .processedAt(LocalDateTime.now())
                        .build()
        );
    }
}