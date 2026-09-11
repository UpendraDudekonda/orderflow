package com.orderflow.payment.producer;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.orderflow.payment.entity.OutboxEvent;
import com.orderflow.payment.entity.OutboxStatus;
import com.orderflow.payment.repository.OutboxEventRepository;

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

                markAsPublished(event);

                log.info(
                        "Outbox event published: eventId={}, topic={}",
                        event.getEventId(),
                        event.getTopic()
                );

            } catch (Exception e) {

                log.error(
                        "Failed to publish outbox event: eventId={}",
                        event.getEventId(),
                        e
                );
            }
        }
    }

    @Transactional
    protected void markAsPublished(
            OutboxEvent event) {

        event.setStatus(
                OutboxStatus.PUBLISHED
        );

        event.setPublishedAt(
                LocalDateTime.now()
        );

        outboxEventRepository.save(event);
    }
}