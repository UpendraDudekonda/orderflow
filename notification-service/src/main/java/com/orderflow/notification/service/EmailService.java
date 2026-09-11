package com.orderflow.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.mail.from}")
    private String from;

    @Value("${notification.mail.to}")
    private String to;

    public void sendPaymentSuccessEmail(
            Long orderId,
            Long paymentId,
            String amount,
            String currency,
            String transactionReference) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(to);

        message.setSubject(
                "OrderFlow - Payment Successful - Order #" + orderId
        );

        message.setText(
                """
                Hello,

                Your payment was successful.

                Order ID: %d
                Payment ID: %d
                Amount: %s %s
                Transaction Reference: %s

                Your order has been confirmed.

                Thank you for using OrderFlow.
                """.formatted(
                        orderId,
                        paymentId,
                        amount,
                        currency,
                        transactionReference
                )
        );

        mailSender.send(message);

        log.info(
                "Payment success email sent: orderId={}, paymentId={}",
                orderId,
                paymentId
        );
    }

    public void sendPaymentFailedEmail(
            Long orderId,
            Long paymentId,
            String amount,
            String currency,
            String reason) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(to);

        message.setSubject(
                "OrderFlow - Payment Failed - Order #" + orderId
        );

        message.setText(
                """
                Hello,

                Unfortunately, your payment failed.

                Order ID: %d
                Payment ID: %d
                Amount: %s %s
                Reason: %s

                Please try again.

                Thank you for using OrderFlow.
                """.formatted(
                        orderId,
                        paymentId,
                        amount,
                        currency,
                        reason
                )
        );

        mailSender.send(message);

        log.info(
                "Payment failure email sent: orderId={}, paymentId={}",
                orderId,
                paymentId
        );
    }
}