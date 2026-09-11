package com.orderflow.order.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.orderflow.order.event.OrderCreatedEvent;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String ORDER_CREATED_TOPIC =
            "order.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(
            OrderCreatedEvent event) {

        kafkaTemplate.send(
                ORDER_CREATED_TOPIC,
                event.getOrderId().toString(),
                event
        );
    }
}