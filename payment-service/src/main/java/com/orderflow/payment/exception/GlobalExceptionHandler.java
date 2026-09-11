package com.orderflow.payment.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            PaymentNotFoundException.class
    )
    public ResponseEntity<?> handleNotFound(
            PaymentNotFoundException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        Map.of(
                                "timestamp",
                                LocalDateTime.now(),
                                "status",
                                404,
                                "error",
                                "PAYMENT_NOT_FOUND",
                                "message",
                                exception.getMessage()
                        )
                );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        Map.of(
                                "timestamp", LocalDateTime.now(),
                                "status", 500,
                                "error", "Internal Server Error",
                                "message", "An unexpected error occurred"
                        )
                );
    }
}