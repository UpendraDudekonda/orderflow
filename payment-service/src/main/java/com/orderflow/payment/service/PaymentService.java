package com.orderflow.payment.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.orderflow.payment.entity.Payment;
import com.orderflow.payment.entity.PaymentStatus;
import com.orderflow.payment.event.PaymentFailedEvent;
import com.orderflow.payment.event.PaymentRequestedEvent;
import com.orderflow.payment.event.PaymentSucceededEvent;
import com.orderflow.payment.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final OutboxService outboxService;

    @Transactional
    public void processPayment(
            PaymentRequestedEvent event) {

        log.info(
                "Processing payment request for order {}",
                event.getOrderId()
        );

        /*
         * Business-level idempotency check
         *
         * Prevents the same payment request from
         * creating another payment.
         */
        if (paymentRepository
                .findByIdempotencyKey(
                        event.getIdempotencyKey()
                )
                .isPresent()) {

            log.info(
                    "Payment request already processed. idempotencyKey={}",
                    event.getIdempotencyKey()
            );

            return;
        }

        /*
         * Create payment
         */
        Payment payment =
                Payment.builder()
                        .orderId(event.getOrderId())
                        .userId(event.getUserId())
                        .amount(event.getAmount())
                        .currency(event.getCurrency())
                        .status(PaymentStatus.PROCESSING)
                        .idempotencyKey(
                                event.getIdempotencyKey()
                        )
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        Payment savedPayment =
                paymentRepository.save(payment);

        /*
         * Simulate payment gateway
         */
        boolean paymentSuccessful =
                simulatePaymentGateway(event);

        if (paymentSuccessful) {

            handleSuccessfulPayment(
                    savedPayment
            );

        } else {

            handleFailedPayment(
                    savedPayment,
                    "Payment gateway rejected the payment"
            );
        }
    }

    private boolean simulatePaymentGateway(
            PaymentRequestedEvent event) {

        /*
         * Temporary payment gateway simulation.
         *
         * FAIL_TEST is used to simulate
         * payment failure.
         *
         * Later this can be replaced with
         * Razorpay / Stripe / another provider.
         */

        if ("FAIL_TEST".equalsIgnoreCase(
                event.getPaymentMethod())) {

            return false;
        }

        return true;
    }

    private void handleSuccessfulPayment(
            Payment payment) {

        /*
         * Update payment status
         */
        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        payment.setTransactionReference(
                "TXN-" + UUID.randomUUID()
        );

        paymentRepository.save(payment);

        /*
         * Create PaymentSucceededEvent
         */
        PaymentSucceededEvent event =
                PaymentSucceededEvent.builder()
                        .eventId(
                                UUID.randomUUID().toString()
                        )
                        .paymentId(payment.getId())
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .transactionReference(
                                payment.getTransactionReference()
                        )
                        .occurredAt(
                                LocalDateTime.now()
                        )
                        .build();

        /*
         * Save event to Outbox.
         *
         * This happens inside the same
         * database transaction as the payment update.
         *
         * Outbox Publisher will later publish
         * this event to Kafka.
         */
        outboxService.saveEvent(
                event.getEventId(),
                "Payment",
                String.valueOf(payment.getId()),
                "PaymentSucceededEvent",
                "payment.succeeded",
                event
        );

        log.info(
                "Payment succeeded and outbox event created for order {}",
                payment.getOrderId()
        );
    }

    private void handleFailedPayment(
            Payment payment,
            String reason) {

        /*
         * Update payment status
         */
        payment.setStatus(
                PaymentStatus.FAILED
        );

        paymentRepository.save(payment);

        /*
         * Create PaymentFailedEvent
         */
        PaymentFailedEvent event =
                PaymentFailedEvent.builder()
                        .eventId(
                                UUID.randomUUID().toString()
                        )
                        .paymentId(payment.getId())
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .reason(reason)
                        .occurredAt(
                                LocalDateTime.now()
                        )
                        .build();

        /*
         * Save failed-payment event to Outbox.
         *
         * Outbox Publisher will publish it
         * to payment.failed.
         */
        outboxService.saveEvent(
                event.getEventId(),
                "Payment",
                String.valueOf(payment.getId()),
                "PaymentFailedEvent",
                "payment.failed",
                event
        );

        log.warn(
                "Payment failed and outbox event created for order {}: {}",
                payment.getOrderId(),
                reason
        );
    }
}