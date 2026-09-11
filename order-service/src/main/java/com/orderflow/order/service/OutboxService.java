package com.orderflow.order.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.orderflow.order.entity.OutboxEvent;
import com.orderflow.order.entity.OutboxStatus;
import com.orderflow.order.repository.OutboxEventRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;

    public void saveEvent(
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String topic,
            Object event) {

        try {

            String payload =
                    objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .eventId(eventId)
                            .aggregateType(aggregateType)
                            .aggregateId(aggregateId)
                            .eventType(eventType)
                            .payload(payload)
                            .topic(topic)
                            .status(OutboxStatus.PENDING)
                            .createdAt(LocalDateTime.now())
                            .build();

            outboxEventRepository.save(outboxEvent);

            log.info(
                    "Order outbox event saved: eventId={}, eventType={}, topic={}",
                    eventId,
                    eventType,
                    topic
            );

        } catch (JsonProcessingException e) {

            throw new RuntimeException(
                    "Failed to serialize outbox event",
                    e
            );
        }
    }
}