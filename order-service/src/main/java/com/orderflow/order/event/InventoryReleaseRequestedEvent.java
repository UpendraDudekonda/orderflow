package com.orderflow.order.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReleaseRequestedEvent {

    private String eventId;

    private Long orderId;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private LocalDateTime occurredAt;
}