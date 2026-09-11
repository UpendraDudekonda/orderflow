package com.orderflow.inventory.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.Builder;
import lombok.Getter;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ErrorResponse>
    handleInventoryNotFound(
            InventoryNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(404)
                                .error("Not Found")
                                .message(ex.getMessage())
                                .build()
                );
    }


    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse>
    handleInsufficientStock(
            InsufficientStockException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(409)
                                .error("Insufficient Stock")
                                .message(ex.getMessage())
                                .build()
                );
    }


    @ExceptionHandler(ConcurrentStockUpdateException.class)
    public ResponseEntity<ErrorResponse>
    handleConcurrentUpdate(
            ConcurrentStockUpdateException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(409)
                                .error("Concurrent Update")
                                .message(ex.getMessage())
                                .build()
                );
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse>
    handleIllegalArgument(
            IllegalArgumentException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(400)
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .build()
                );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>
    handleValidation(
            MethodArgumentNotValidException ex) {

        String message =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(400)
                                .error("Validation Error")
                                .message(message)
                                .build()
                );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>
    handleGeneric(Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(500)
                                .error("Internal Server Error")
                                .message("An unexpected error occurred")
                                .build()
                );
    }


    @Getter
    @Builder
    public static class ErrorResponse {

        private LocalDateTime timestamp;

        private int status;

        private String error;

        private String message;
    }
}