package com.orderflow.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

import com.orderflow.payment.entity.Payment;
import com.orderflow.payment.entity.PaymentStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private String currency;

    private PaymentStatus status;

    private String transactionReference;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PaymentResponse from(
            Payment payment) {

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .transactionReference(
                        payment.getTransactionReference()
                )
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}