package com.orderflow.order.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.orderflow.order.event.PaymentRequestedEvent;


@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String PAYMENT_REQUESTED_TOPIC =
            "payment.requested";

    private final KafkaTemplate<String, Object>
            kafkaTemplate;

    public void publishPaymentRequested(
            PaymentRequestedEvent event) {

        kafkaTemplate.send(
                PAYMENT_REQUESTED_TOPIC,
                event.getOrderId().toString(),
                event
        );
    }
}