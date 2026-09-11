package com.orderflow.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.notification.event.PaymentFailedEvent;
import com.orderflow.notification.service.EmailService;
import com.orderflow.notification.service.ProcessedEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailedConsumer {

    private static final String CONSUMER_NAME =
            "notification-payment-failed";

    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "payment.failed",
            groupId = "notification-service",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(String message) {

        PaymentFailedEvent event;

        try {

            event = objectMapper.readValue(
                    message,
                    PaymentFailedEvent.class
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to deserialize PaymentFailedEvent: {}",
                    message,
                    ex
            );

            throw new RuntimeException(
                    "Invalid PaymentFailedEvent JSON",
                    ex
            );
        }

        log.info(
                "Received PaymentFailedEvent: eventId={}, orderId={}, paymentId={}",
                event.getEventId(),
                event.getOrderId(),
                event.getPaymentId()
        );

        if (processedEventService.isProcessed(
                event.getEventId(),
                CONSUMER_NAME)) {

            log.info(
                    "Duplicate PaymentFailedEvent ignored: eventId={}",
                    event.getEventId()
            );

            return;
        }

        emailService.sendPaymentFailedEmail(
                event.getOrderId(),
                event.getPaymentId(),
                event.getAmount().toString(),
                event.getCurrency(),
                event.getReason()
        );

        processedEventService.markAsProcessed(
                event.getEventId(),
                CONSUMER_NAME
        );

        log.info(
                "Payment failure notification processed: orderId={}",
                event.getOrderId()
        );
    }
}