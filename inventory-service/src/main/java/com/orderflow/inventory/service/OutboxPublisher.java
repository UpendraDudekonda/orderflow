package com.orderflow.inventory.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.orderflow.inventory.entity.OutboxEvent;
import com.orderflow.inventory.entity.OutboxStatus;
import com.orderflow.inventory.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop50ByStatusOrderByCreatedAtAsc(
                                OutboxStatus.PENDING
                        );

        for (OutboxEvent event : events) {

            try {

                kafkaTemplate
                        .send(
                                event.getTopic(),
                                event.getAggregateId(),
                                event.getPayload()
                        )
                        .get();

                event.setStatus(
                        OutboxStatus.PUBLISHED
                );

                event.setPublishedAt(
                        LocalDateTime.now()
                );

                outboxEventRepository.save(event);

                log.info(
                        "Inventory outbox event published: eventId={}, topic={}",
                        event.getEventId(),
                        event.getTopic()
                );

            } catch (Exception ex) {

                log.error(
                        "Failed to publish inventory outbox event: eventId={}, topic={}",
                        event.getEventId(),
                        event.getTopic(),
                        ex
                );
            }
        }
    }
}