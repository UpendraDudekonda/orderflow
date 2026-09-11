package com.orderflow.order.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
public class OrderCreatedEvent {

    private String eventId;

    private Long orderId;

    private Long userId;

    private BigDecimal totalAmount;

    private String currency;

    private LocalDateTime occurredAt;

    private List<OrderItemEvent> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEvent {

        private Long productId;

        private Integer quantity;

        private BigDecimal unitPrice;
    }
}