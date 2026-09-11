package com.orderflow.order.event;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReleasedEvent {

    private String eventId;

    private Long orderId;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private LocalDateTime occurredAt;
}