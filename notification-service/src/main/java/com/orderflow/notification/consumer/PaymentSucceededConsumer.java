package com.orderflow.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.notification.event.PaymentSucceededEvent;
import com.orderflow.notification.service.EmailService;
import com.orderflow.notification.service.ProcessedEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSucceededConsumer {

    private static final String CONSUMER_NAME =
            "notification-payment-succeeded";

    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "payment.succeeded",
            groupId = "notification-service",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(String message) {

        PaymentSucceededEvent event;

        try {

            event = objectMapper.readValue(
                    message,
                    PaymentSucceededEvent.class
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to deserialize PaymentSucceededEvent: {}",
                    message,
                    ex
            );

            throw new RuntimeException(
                    "Invalid PaymentSucceededEvent JSON",
                    ex
            );
        }

        log.info(
                "Received PaymentSucceededEvent: eventId={}, orderId={}, paymentId={}",
                event.getEventId(),
                event.getOrderId(),
                event.getPaymentId()
        );

        if (processedEventService.isProcessed(
                event.getEventId(),
                CONSUMER_NAME)) {

            log.info(
                    "Duplicate PaymentSucceededEvent ignored: eventId={}",
                    event.getEventId()
            );

            return;
        }

        emailService.sendPaymentSuccessEmail(
                event.getOrderId(),
                event.getPaymentId(),
                event.getAmount().toString(),
                event.getCurrency(),
                event.getTransactionReference()
        );

        processedEventService.markAsProcessed(
                event.getEventId(),
                CONSUMER_NAME
        );

        log.info(
                "Payment success notification processed: orderId={}",
                event.getOrderId()
        );
    }
}