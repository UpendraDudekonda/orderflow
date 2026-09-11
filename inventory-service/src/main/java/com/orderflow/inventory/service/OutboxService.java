package com.orderflow.inventory.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.inventory.entity.OutboxEvent;
import com.orderflow.inventory.entity.OutboxStatus;
import com.orderflow.inventory.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void saveEvent(
            String eventId,
            String aggregateType,
            String aggregateId,
            Object event,
            String eventType,
            String topic) {

        try {

            String payload =
                    objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .eventId(eventId)
                            .aggregateType(aggregateType)
                            .aggregateId(aggregateId)
                            .eventType(eventType)
                            .topic(topic)
                            .payload(payload)
                            .status(OutboxStatus.PENDING)
                            .createdAt(LocalDateTime.now())
                            .build();

            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException ex) {

            throw new IllegalStateException(
                    "Failed to serialize outbox event",
                    ex
            );
        }
    }
}