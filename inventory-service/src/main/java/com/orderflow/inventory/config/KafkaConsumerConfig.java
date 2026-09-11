package com.orderflow.inventory.config;

import org.apache.kafka.common.TopicPartition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate) {

        /*
         * When message processing fails:
         *
         * Initial attempt
         *       ↓
         * Retry 1 → after 2 seconds
         *       ↓
         * Retry 2 → after 2 seconds
         *       ↓
         * Retry 3 → after 2 seconds
         *       ↓
         * DLT
         */

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + ".DLT",
                                        record.partition()
                                )
                );

        /*
         * 2000L = 2 seconds
         * 3L    = 3 retries
         */

        FixedBackOff backOff =
                new FixedBackOff(
                        2000L,
                        3L
                );

        return new DefaultErrorHandler(
                recoverer,
                backOff
        );
    }
}