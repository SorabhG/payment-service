package com.example.paymentservice.service;


import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.enums.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
@RequiredArgsConstructor
@Slf4j

public class PaymentConsumer {

    private final PaymentRepository paymentRepository;
    private final FraudService fraudService;
    @Value("${payment.consumer.processing-delay-ms:0}")
    private long processingDelayMs;


    @KafkaListener(
            topics = "payments",
            groupId = "payment-group"
    )
    @Transactional
    public void consumePayment(UUID paymentId) {
        //This delay is for demonstration, not production.
        if (processingDelayMs > 0) {
            log.info("Delaying processing by {} ms", processingDelayMs);
            try {
                Thread.sleep(processingDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("Received payment event for paymentId={}", paymentId);

        // Idempotency check
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment {} already processed with status {}. Ignoring.", paymentId, payment.getStatus());
            return;
        }

        boolean isFraudulent = fraudService.checkFraud(payment);

        if (isFraudulent) {
            payment.setStatus(PaymentStatus.FAILED);
            throw new IllegalArgumentException("Fraud detected");
        }
        payment.setStatus(
                PaymentStatus.SUCCESS
        );


        log.info("Payment {} processed with status {}", paymentId, payment.getStatus());
    }
}
