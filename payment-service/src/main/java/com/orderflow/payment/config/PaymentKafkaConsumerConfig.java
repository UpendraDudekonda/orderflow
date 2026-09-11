package com.orderflow.payment.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import org.springframework.kafka.support.serializer.JsonDeserializer;

import org.springframework.util.backoff.FixedBackOff;

import com.orderflow.payment.event.PaymentRequestedEvent;

@Configuration
public class PaymentKafkaConsumerConfig {

    // =========================================================
    // CONSUMER PROPERTIES
    // =========================================================

    private Map<String, Object> consumerProperties() {

        Map<String, Object> properties =
                new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "payment-service"
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return properties;
    }

    // =========================================================
    // PAYMENT CONSUMER FACTORY
    // =========================================================

    @Bean
    public ConsumerFactory<String, PaymentRequestedEvent>
            paymentRequestedConsumerFactory() {

        JsonDeserializer<PaymentRequestedEvent>
                deserializer =
                new JsonDeserializer<>(
                        PaymentRequestedEvent.class,
                        false
                );

        deserializer.addTrustedPackages(
                "com.orderflow.payment.event"
        );

        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(),
                new StringDeserializer(),
                deserializer
        );
    }

    // =========================================================
    // ERROR HANDLER + DLT
    // =========================================================

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate) {

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
         * Initial attempt
         *       ↓
         * Retry 1 → 2 seconds
         *       ↓
         * Retry 2 → 2 seconds
         *       ↓
         * Retry 3 → 2 seconds
         *       ↓
         * DLT
         */

        FixedBackOff backOff =
                new FixedBackOff(2000L, 3L);

        return new DefaultErrorHandler(
                recoverer,
                backOff
        );
    }

    // =========================================================
    // LISTENER CONTAINER FACTORY
    // =========================================================

    @Bean
    public ConcurrentKafkaListenerContainerFactory<
            String,
            PaymentRequestedEvent>
            paymentRequestedKafkaListenerContainerFactory(
                    ConsumerFactory<String, PaymentRequestedEvent>
                            paymentRequestedConsumerFactory,
                    CommonErrorHandler kafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<
                String,
                PaymentRequestedEvent>
                factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                paymentRequestedConsumerFactory
        );

        factory.setCommonErrorHandler(
                kafkaErrorHandler
        );

        return factory;
    }
}