package com.orderflow.order.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.orderflow.order.dto.ProductResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProductClientFallbackFactory
        implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {

        log.error(
                "Product Service unavailable. Fallback activated. Reason: {}",
                cause.getMessage()
        );

        return new ProductClient() {

            @Override
            public ProductResponse getProductById(Long id) {

                log.warn(
                        "Product Service fallback invoked for productId={}",
                        id
                );

                throw new IllegalStateException(
                        "Product Service is temporarily unavailable. "
                        + "Please try again later."
                );
            }
        };
    }
}