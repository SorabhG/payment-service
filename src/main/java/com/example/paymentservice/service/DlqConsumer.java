package com.example.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
public class DlqConsumer {

    // Store DLQ messages temporarily for replay/demo
    private final ConcurrentLinkedQueue<UUID> dlqQueue = new ConcurrentLinkedQueue<>();

    @KafkaListener(
            topics = "payments-dlq",
            groupId = "payment-dlq-group"
    )
    public void consumeDlq(ConsumerRecord<String, UUID> record) {
        UUID paymentId = record.value();

        // Log for demo purposes
        log.error("DLQ message received: paymentId={}, partition={}, offset={}",
                paymentId, record.partition(), record.offset());

        // Store in memory for manual replay or inspection
        dlqQueue.add(paymentId);
    }

    // Simple method to replay DLQ messages
    public void replayMessages() {
        while (!dlqQueue.isEmpty()) {
            UUID paymentId = dlqQueue.poll();
            log.info("Replaying DLQ message: paymentId={}", paymentId);
            // In real-world: send back to main topic
            // kafkaTemplate.send("payments", paymentId);
        }
    }

    // Expose current DLQ messages for monitoring/demo
    public ConcurrentLinkedQueue<UUID> getDlqQueue() {
        return dlqQueue;
    }
}
