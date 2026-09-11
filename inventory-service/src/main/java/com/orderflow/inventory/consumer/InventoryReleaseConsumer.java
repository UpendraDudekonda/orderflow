package com.orderflow.inventory.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.inventory.event.InventoryReleaseRequestedEvent;
import com.orderflow.inventory.event.InventoryReleasedEvent;
import com.orderflow.inventory.service.InventoryService;
import com.orderflow.inventory.service.OutboxService;
import com.orderflow.inventory.service.ProcessedEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReleaseConsumer {

    private static final String CONSUMER_NAME =
            "inventory-release";

    private final InventoryService inventoryService;
    private final OutboxService outboxService;
    private final ProcessedEventService processedEventService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "inventory.release.requested",
            groupId = "inventory-service-release",
            containerFactory =
                    "inventoryReleaseKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(String message) {

        // =========================================================
        // DESERIALIZE RAW JSON
        // =========================================================

        InventoryReleaseRequestedEvent event;

        try {

            event = objectMapper.readValue(
                    message,
                    InventoryReleaseRequestedEvent.class
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to deserialize InventoryReleaseRequestedEvent: {}",
                    message,
                    ex
            );

            throw new RuntimeException(
                    "Invalid InventoryReleaseRequestedEvent JSON",
                    ex
            );
        }

        log.info(
                "Received inventory release request: eventId={}, orderId={}, productId={}, quantity={}",
                event.getEventId(),
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity()
        );

        // =========================================================
        // IDEMPOTENCY CHECK
        // =========================================================

        if (processedEventService.isProcessed(
                event.getEventId(),
                CONSUMER_NAME)) {

            log.info(
                    "Duplicate release event detected. Ignoring eventId={}, orderId={}",
                    event.getEventId(),
                    event.getOrderId()
            );

            return;
        }

        // =========================================================
        // RELEASE INVENTORY
        // =========================================================

        inventoryService.releaseStock(
                event.getProductId(),
                event.getQuantity()
        );

        log.info(
                "Inventory released successfully: orderId={}, productId={}, quantity={}",
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity()
        );

        // =========================================================
        // CREATE INVENTORY RELEASED EVENT
        // =========================================================

        InventoryReleasedEvent releasedEvent =
                InventoryReleasedEvent.builder()
                        .eventId(
                                UUID.randomUUID().toString()
                        )
                        .orderId(
                                event.getOrderId()
                        )
                        .userId(
                                event.getUserId()
                        )
                        .productId(
                                event.getProductId()
                        )
                        .quantity(
                                event.getQuantity()
                        )
                        .occurredAt(
                                LocalDateTime.now()
                        )
                        .build();

        // =========================================================
        // SAVE TO OUTBOX
        // =========================================================

        outboxService.saveEvent(
                releasedEvent.getEventId(),
                "ORDER",
                event.getOrderId().toString(),
                releasedEvent,
                "InventoryReleasedEvent",
                "inventory.released"
        );

        // =========================================================
        // MARK EVENT AS PROCESSED
        // =========================================================

        processedEventService.markAsProcessed(
                event.getEventId(),
                CONSUMER_NAME
        );

        log.info(
                "Inventory release processed successfully: orderId={}, productId={}, quantity={}",
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity()
        );
    }
}