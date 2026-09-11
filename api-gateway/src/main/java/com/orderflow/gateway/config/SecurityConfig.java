package com.orderflow.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.orderflow.gateway.security.JwtAuthenticationFilter;
import com.orderflow.gateway.security.JwtService;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService) {

        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )

                .authorizeExchange(exchange -> exchange

                        /*
                         * Public Authentication APIs
                         */
                        .pathMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout"
                        ).permitAll()

                        /*
                         * Public Actuator endpoints
                         *
                         * Prometheus needs unauthenticated access
                         * so it can scrape metrics.
                         */
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()

                        /*
                         * Public Product GET APIs
                         */
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/products",
                                "/api/products/search",
                                "/api/products/*"
                        ).permitAll()

                        /*
                         * Product write APIs - ADMIN only
                         */
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/products"
                        ).hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.PUT,
                                "/api/products/*"
                        ).hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/products/*"
                        ).hasRole("ADMIN")

                        /*
                         * Order APIs - authenticated users
                         */
                        .pathMatchers(
                                "/api/orders/**"
                        ).authenticated()

                        /*
                         * Inventory APIs - authenticated users
                         */
                        .pathMatchers(
                                "/api/inventory/**"
                        ).authenticated()

                        /*
                         * Payment APIs - authenticated users
                         */
                        .pathMatchers(
                                "/api/payments/**"
                        ).authenticated()

                        /*
                         * Everything else requires authentication
                         */
                        .anyExchange().authenticated()
                )

                .build();
    }
}