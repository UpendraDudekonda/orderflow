package com.orderflow.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestedEvent {

    private String eventId;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private String currency;

    private String idempotencyKey;

    private String paymentMethod;

    private LocalDateTime occurredAt;
}