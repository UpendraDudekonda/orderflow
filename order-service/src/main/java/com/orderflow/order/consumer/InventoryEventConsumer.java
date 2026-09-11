package com.orderflow.order.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.event.InventoryReleasedEvent;
import com.orderflow.order.event.InventoryReservationFailedEvent;
import com.orderflow.order.event.InventoryReservedEvent;
import com.orderflow.order.service.OrderService;
import com.orderflow.order.service.ProcessedEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final OrderService orderService;
    private final ProcessedEventService processedEventService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "inventory.reserved",
            groupId = "order-service",
            containerFactory = "orderKafkaListenerContainerFactory"
    )
    public void consumeInventoryReserved(String message) {

        try {

            InventoryReservedEvent event =
                    objectMapper.readValue(
                            message,
                            InventoryReservedEvent.class
                    );

            final String consumerName =
                    "order-inventory-reserved";

            log.info(
                    "Received InventoryReservedEvent: eventId={}, orderId={}, productId={}, quantity={}",
                    event.getEventId(),
                    event.getOrderId(),
                    event.getProductId(),
                    event.getQuantity()
            );

            if (processedEventService.isProcessed(
                    event.getEventId(),
                    consumerName)) {

                log.info(
                        "Duplicate event detected. Ignoring eventId={}, orderId={}",
                        event.getEventId(),
                        event.getOrderId()
                );

                return;
            }

            orderService.handleInventoryReserved(
                    event.getOrderId()
            );

            processedEventService.markAsProcessed(
                    event.getEventId(),
                    consumerName
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process inventory.reserved message",
                    e
            );

            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
            topics = "inventory.failed",
            groupId = "order-service",
            containerFactory = "orderKafkaListenerContainerFactory"
    )
    public void consumeInventoryFailed(String message) {

        try {

            InventoryReservationFailedEvent event =
                    objectMapper.readValue(
                            message,
                            InventoryReservationFailedEvent.class
                    );

            final String consumerName =
                    "order-inventory-failed";

            log.warn(
                    "Received InventoryReservationFailedEvent: eventId={}, orderId={}, productId={}, reason={}",
                    event.getEventId(),
                    event.getOrderId(),
                    event.getProductId(),
                    event.getReason()
            );

            if (processedEventService.isProcessed(
                    event.getEventId(),
                    consumerName)) {

                log.info(
                        "Duplicate event detected. Ignoring eventId={}, orderId={}",
                        event.getEventId(),
                        event.getOrderId()
                );

                return;
            }

            orderService.handleInventoryFailed(
                    event.getOrderId()
            );

            processedEventService.markAsProcessed(
                    event.getEventId(),
                    consumerName
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process inventory.failed message",
                    e
            );

            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
            topics = "inventory.released",
            groupId = "order-service",
            containerFactory = "orderKafkaListenerContainerFactory"
    )
    public void consumeInventoryReleased(String message) {

        try {

            InventoryReleasedEvent event =
                    objectMapper.readValue(
                            message,
                            InventoryReleasedEvent.class
                    );

            final String consumerName =
                    "order-inventory-released";

            log.info(
                    "Received InventoryReleasedEvent: eventId={}, orderId={}, productId={}, quantity={}",
                    event.getEventId(),
                    event.getOrderId(),
                    event.getProductId(),
                    event.getQuantity()
            );

            if (processedEventService.isProcessed(
                    event.getEventId(),
                    consumerName)) {

                log.info(
                        "Duplicate event detected. Ignoring eventId={}, orderId={}",
                        event.getEventId(),
                        event.getOrderId()
                );

                return;
            }

            orderService.handleInventoryReleased(
                    event.getOrderId()
            );

            processedEventService.markAsProcessed(
                    event.getEventId(),
                    consumerName
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process inventory.released message",
                    e
            );

            throw new RuntimeException(e);
        }
    }
}