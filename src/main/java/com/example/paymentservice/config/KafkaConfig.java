package com.example.paymentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String PAYMENT_TOPIC = "payments";
    public static final String PAYMENT_DLQ_TOPIC = "payments_dlq";

    /**
     * Ensures the topic exists.
     * Created automatically on application startup by KafkaAdmin.
     */
    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name(PAYMENT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
    @Bean
    public NewTopic paymentsDlqTopic() {
        return TopicBuilder.name(PAYMENT_DLQ_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
