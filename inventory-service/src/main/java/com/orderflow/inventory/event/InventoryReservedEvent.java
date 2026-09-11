package com.orderflow.inventory.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservedEvent {

    private String eventId;

    private Long orderId;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private LocalDateTime occurredAt;
}