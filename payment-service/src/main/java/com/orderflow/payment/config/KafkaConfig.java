package com.orderflow.payment.config;

import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // =========================================================
    // BUSINESS TOPICS
    // =========================================================

    @Bean
    public NewTopic paymentRequestedTopic() {

        return TopicBuilder
                .name("payment.requested")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentSucceededTopic() {

        return TopicBuilder
                .name("payment.succeeded")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {

        return TopicBuilder
                .name("payment.failed")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // =========================================================
    // DEAD LETTER TOPIC
    // =========================================================

    @Bean
    public NewTopic paymentRequestedDltTopic() {

        return TopicBuilder
                .name("payment.requested.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}