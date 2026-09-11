package com.orderflow.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.orderflow.payment.entity.OutboxEvent;
import com.orderflow.payment.entity.OutboxStatus;
import com.orderflow.payment.repository.OutboxEventRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;

    @Transactional
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
                    "Outbox event saved: eventId={}, eventType={}, topic={}",
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