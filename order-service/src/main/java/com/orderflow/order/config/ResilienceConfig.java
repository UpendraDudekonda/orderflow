package com.orderflow.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ResilienceConfig {

    @Bean
    public Object circuitBreakerEventLogger(
            CircuitBreakerRegistry registry) {

        registry.circuitBreaker("productService")
                .getEventPublisher()
                .onStateTransition(event ->
                        log.warn("CircuitBreaker state transition: {}",
                                event));

        return new Object();
    }
}