package com.example.paymentservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {

    private final KafkaTemplate<String, UUID> kafkaTemplate;

    public void sendPaymentEvent(UUID paymentId) {

        kafkaTemplate.send("payments", paymentId)
                .thenAccept(result ->
                        log.info(
                                "Payment successfully posted to topic={}, partition={}, offset={}, paymentId={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                paymentId
                        )
                )
                .exceptionally(ex -> {
                    log.error("Failed to publish payment event, paymentId={}", paymentId, ex);
                    return null;
                });

    }
}

