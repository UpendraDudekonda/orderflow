package com.orderflow.inventory.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;

import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import org.springframework.kafka.listener.CommonErrorHandler;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // =========================================================
    // TOPICS
    // =========================================================

    @Bean
    public NewTopic inventoryReservedTopic() {

        return TopicBuilder
                .name("inventory.reserved")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryFailedTopic() {

        return TopicBuilder
                .name("inventory.failed")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReleasedTopic() {

        return TopicBuilder
                .name("inventory.released")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedDltTopic() {

        return TopicBuilder
                .name("order.created.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReleaseRequestedDltTopic() {

        return TopicBuilder
                .name("inventory.release.requested.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // =========================================================
    // ORDER.CREATED CONSUMER
    // =========================================================

    @Bean
    public ConsumerFactory<String, String>
    inventoryConsumerFactory() {

        Map<String, Object> props =
                new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "inventory-service"
        );

        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new DefaultKafkaConsumerFactory<>(
                props
        );
    }

    // =========================================================
    // LISTENER FACTORY
    // =========================================================

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    inventoryKafkaListenerContainerFactory(
            ConsumerFactory<String, String>
                    inventoryConsumerFactory,
            CommonErrorHandler kafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String>
                factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                inventoryConsumerFactory
        );

        factory.setCommonErrorHandler(
                kafkaErrorHandler
        );

        return factory;
    }

    // =========================================================
    // PRODUCER
    // =========================================================

    @Bean
    public ProducerFactory<String, String>
    inventoryProducerFactory() {

        Map<String, Object> props =
                new HashMap<>();

        props.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        props.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        props.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        return new DefaultKafkaProducerFactory<>(
                props
        );
    }

    @Bean
    @Primary
    public KafkaTemplate<String, String>
    inventoryKafkaTemplate(
            ProducerFactory<String, String>
                    inventoryProducerFactory) {

        return new KafkaTemplate<>(
                inventoryProducerFactory
        );
    }
}