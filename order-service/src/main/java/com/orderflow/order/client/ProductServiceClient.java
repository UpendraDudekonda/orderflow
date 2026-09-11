package com.orderflow.order.client;

import org.springframework.stereotype.Service;

import com.orderflow.order.dto.ProductResponse;
import com.orderflow.order.exception.ServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceClient {

    private final ProductClient productClient;

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "fallback")
    public ProductResponse getProductById(Long id) {
        return productClient.getProductById(id);
    }

    private ProductResponse fallback(Long id, Throwable throwable) {

        throw new ServiceUnavailableException(
            "Product Service is temporarily unavailable. Please try again later.",
            throwable
        );
    }
}