package com.orderflow.payment.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.orderflow.payment.event.PaymentFailedEvent;
import com.orderflow.payment.event.PaymentSucceededEvent;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String PAYMENT_SUCCEEDED_TOPIC =
            "payment.succeeded";

    private static final String PAYMENT_FAILED_TOPIC =
            "payment.failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentSucceeded(
            PaymentSucceededEvent event) {

        kafkaTemplate.send(
                PAYMENT_SUCCEEDED_TOPIC,
                event.getOrderId().toString(),
                event
        );
    }

    public void publishPaymentFailed(
            PaymentFailedEvent event) {

        kafkaTemplate.send(
                PAYMENT_FAILED_TOPIC,
                event.getOrderId().toString(),
                event
        );
    }
}