package com.orderflow.order.event;

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
public class InventoryReservationFailedEvent {

    private String eventId;

    private Long orderId;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private String reason;

    private LocalDateTime occurredAt;
}