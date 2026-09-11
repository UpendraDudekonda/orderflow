package com.orderflow.payment.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.orderflow.payment.event.PaymentRequestedEvent;
import com.orderflow.payment.service.PaymentService;
import com.orderflow.payment.service.ProcessedEventService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestedConsumer {

    private static final String CONSUMER_NAME =
            "payment-requested";

    private final PaymentService paymentService;

    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "payment.requested",
            groupId = "payment-service",
            containerFactory =
                    "paymentRequestedKafkaListenerContainerFactory"
    )
    public void consume(
            PaymentRequestedEvent event) {

        log.info(
                "Received PaymentRequestedEvent: eventId={}, orderId={}, amount={}",
                event.getEventId(),
                event.getOrderId(),
                event.getAmount()
        );

        /*
         * Kafka event idempotency check
         */
        if (processedEventService.isProcessed(
                event.getEventId(),
                CONSUMER_NAME)) {

            log.info(
                    "Duplicate payment event detected. Ignoring eventId={}, orderId={}",
                    event.getEventId(),
                    event.getOrderId()
            );

            return;
        }

        /*
         * Process payment
         *
         * If processing throws a technical exception,
         * we DO NOT mark the event as processed.
         *
         * Kafka can therefore retry it.
         */
        paymentService.processPayment(event);

        /*
         * Mark event as processed only after
         * successful business processing.
         */
        processedEventService.markAsProcessed(
                event.getEventId(),
                CONSUMER_NAME
        );

        log.info(
                "Payment event processed successfully. eventId={}, orderId={}",
                event.getEventId(),
                event.getOrderId()
        );
    }
}