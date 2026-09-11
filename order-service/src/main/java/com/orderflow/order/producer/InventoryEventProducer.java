package com.orderflow.order.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.orderflow.order.event.InventoryReleaseRequestedEvent;


@RequiredArgsConstructor
public class InventoryEventProducer {

    private static final String
            INVENTORY_RELEASE_REQUESTED_TOPIC =
            "inventory.release.requested";

    private final KafkaTemplate<String, Object>
            kafkaTemplate;

    public void publishInventoryReleaseRequested(
            InventoryReleaseRequestedEvent event) {

        kafkaTemplate.send(
                INVENTORY_RELEASE_REQUESTED_TOPIC,
                event.getOrderId().toString(),
                event
        );
    }
}