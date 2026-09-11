package com.orderflow.inventory.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.inventory.event.InventoryReservationFailedEvent;
import com.orderflow.inventory.event.InventoryReservedEvent;
import com.orderflow.inventory.event.OrderCreatedEvent;
import com.orderflow.inventory.event.OrderCreatedEvent.OrderItemEvent;
import com.orderflow.inventory.exception.InsufficientStockException;
import com.orderflow.inventory.service.InventoryService;
import com.orderflow.inventory.service.OutboxService;
import com.orderflow.inventory.service.ProcessedEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private static final String CONSUMER_NAME =
            "inventory-order-created";

    private final InventoryService inventoryService;
    private final ProcessedEventService processedEventService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order.created",
            groupId = "inventory-service",
            containerFactory = "inventoryKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeOrderCreated(String message) {

        // =========================================================
        // DESERIALIZE RAW JSON MESSAGE
        // =========================================================

        OrderCreatedEvent event;

        try {

            event = objectMapper.readValue(
                    message,
                    OrderCreatedEvent.class
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to deserialize OrderCreatedEvent: {}",
                    message,
                    ex
            );

            throw new RuntimeException(
                    "Invalid OrderCreatedEvent JSON",
                    ex
            );
        }

        log.info(
                "Received OrderCreatedEvent: eventId={}, orderId={}",
                event.getEventId(),
                event.getOrderId()
        );

        // =========================================================
        // IDEMPOTENCY CHECK
        // =========================================================

        if (processedEventService.isProcessed(
                event.getEventId(),
                CONSUMER_NAME)) {

            log.info(
                    "Duplicate event detected. Ignoring eventId={}, orderId={}",
                    event.getEventId(),
                    event.getOrderId()
            );

            return;
        }

        // =========================================================
        // RESERVE INVENTORY
        // =========================================================

        for (OrderItemEvent item : event.getItems()) {

            try {

                inventoryService.reserveStock(
                        item.getProductId(),
                        item.getQuantity()
                );

                log.info(
                        "Inventory reserved successfully: orderId={}, productId={}, quantity={}",
                        event.getOrderId(),
                        item.getProductId(),
                        item.getQuantity()
                );

                // =================================================
                // CREATE INVENTORY RESERVED EVENT
                // =================================================

                InventoryReservedEvent reservedEvent =
                        InventoryReservedEvent.builder()
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
                                        item.getProductId()
                                )
                                .quantity(
                                        item.getQuantity()
                                )
                                .occurredAt(
                                        LocalDateTime.now()
                                )
                                .build();

                // =================================================
                // SAVE EVENT TO OUTBOX
                // =================================================

                outboxService.saveEvent(
                        reservedEvent.getEventId(),
                        "ORDER",
                        event.getOrderId().toString(),
                        reservedEvent,
                        "InventoryReservedEvent",
                        "inventory.reserved"
                );

            } catch (InsufficientStockException ex) {

                log.warn(
                        "Insufficient inventory: orderId={}, productId={}, quantity={}",
                        event.getOrderId(),
                        item.getProductId(),
                        item.getQuantity()
                );

                // =================================================
                // CREATE INVENTORY FAILED EVENT
                // =================================================

                InventoryReservationFailedEvent failedEvent =
                        InventoryReservationFailedEvent.builder()
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
                                        item.getProductId()
                                )
                                .quantity(
                                        item.getQuantity()
                                )
                                .reason(
                                        ex.getMessage()
                                )
                                .occurredAt(
                                        LocalDateTime.now()
                                )
                                .build();

                // =================================================
                // SAVE FAILURE EVENT TO OUTBOX
                // =================================================

                outboxService.saveEvent(
                        failedEvent.getEventId(),
                        "ORDER",
                        event.getOrderId().toString(),
                        failedEvent,
                        "InventoryReservationFailedEvent",
                        "inventory.failed"
                );

            } catch (Exception ex) {

                log.error(
                        "Unexpected inventory processing failure: orderId={}, productId={}",
                        event.getOrderId(),
                        item.getProductId(),
                        ex
                );

                throw ex;
            }
        }

        // =========================================================
        // MARK EVENT AS PROCESSED
        // =========================================================

        processedEventService.markAsProcessed(
                event.getEventId(),
                CONSUMER_NAME
        );

        log.info(
                "OrderCreatedEvent processed successfully: eventId={}, orderId={}",
                event.getEventId(),
                event.getOrderId()
        );
    }
}