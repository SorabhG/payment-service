package com.example.paymentservice.config;

import com.example.paymentservice.exception.PaymentNotFoundException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {
// This configuration ensures:
// - Transient failures are retried with backoff
// - Permanent failures are sent directly to DLQ
// - Poison messages never block the consumer

    @Bean
    public DefaultErrorHandler errorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate) {

        // Send failed messages to DLQ
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) ->
                                new TopicPartition("payments-dlq", record.partition())
                );

        // Retry 3 times with 2s delay
        FixedBackOff backOff = new FixedBackOff(2000L, 3L);

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, backOff);

        // ❗ Important: do NOT retry permanent failures
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                PaymentNotFoundException.class
        );

        return errorHandler;
    }
}

