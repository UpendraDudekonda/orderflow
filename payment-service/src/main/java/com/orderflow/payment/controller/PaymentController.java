package com.orderflow.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import com.orderflow.payment.dto.PaymentResponse;
import com.orderflow.payment.entity.Payment;
import com.orderflow.payment.exception.PaymentNotFoundException;
import com.orderflow.payment.repository.PaymentRepository;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long orderId) {

        Payment payment =
                paymentRepository
                        .findByOrderId(orderId)
                        .orElseThrow(
                                () -> new PaymentNotFoundException(
                                        "Payment not found for order: "
                                                + orderId
                                )
                        );

        return ResponseEntity.ok(
                PaymentResponse.from(payment)
        );
    }
}