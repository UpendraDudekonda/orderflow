package com.orderflow.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSucceededEvent {

    private String eventId;

    private Long paymentId;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private String currency;

    private String transactionReference;

    private LocalDateTime occurredAt;
}