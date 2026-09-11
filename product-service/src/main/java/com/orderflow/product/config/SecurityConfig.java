package com.orderflow.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Product browsing is public.
                         */
                        .requestMatchers(
                                "/api/products",
                                "/api/products/search",
                                "/api/products/*"
                        ).permitAll()

                        /*
                         * Actuator.
                         */
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/actuator/metrics"
                        ).permitAll()

                        /*
                         * Product creation/update/delete
                         * requires authentication.
                         */
                        .anyRequest().authenticated()
                )

                .build();
    }
}