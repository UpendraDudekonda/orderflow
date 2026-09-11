package com.orderflow.order.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.event.PaymentFailedEvent;
import com.orderflow.order.event.PaymentSucceededEvent;
import com.orderflow.order.service.OrderService;
import com.orderflow.order.service.ProcessedEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;
    private final ProcessedEventService processedEventService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "payment.succeeded",
            groupId = "order-service",
            containerFactory =
            "orderKafkaListenerContainerFactory"
    )
    public void consumePaymentSucceeded(String message) {

        try {

            PaymentSucceededEvent event =
                    objectMapper.readValue(
                            message,
                            PaymentSucceededEvent.class
                    );

            final String consumerName = "order-payment-succeeded";

            log.info(
                    "Received PaymentSucceededEvent: eventId={}, orderId={}, paymentId={}",
                    event.getEventId(),
                    event.getOrderId(),
                    event.getPaymentId()
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

            orderService.handlePaymentSucceeded(
                    event.getOrderId()
            );

            processedEventService.markAsProcessed(
                    event.getEventId(),
                    consumerName
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process payment.succeeded message",
                    e
            );

            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
            topics = "payment.failed",
            groupId = "order-service",
            containerFactory =
            "orderKafkaListenerContainerFactory"
    )
    public void consumePaymentFailed(String message) {

        try {

            PaymentFailedEvent event =
                    objectMapper.readValue(
                            message,
                            PaymentFailedEvent.class
                    );

            final String consumerName = "order-payment-failed";

            log.warn(
                    "Received PaymentFailedEvent: eventId={}, orderId={}, reason={}",
                    event.getEventId(),
                    event.getOrderId(),
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

            orderService.handlePaymentFailed(
                    event.getOrderId()
            );

            processedEventService.markAsProcessed(
                    event.getEventId(),
                    consumerName
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process payment.failed message",
                    e
            );

            throw new RuntimeException(e);
        }
    }
}