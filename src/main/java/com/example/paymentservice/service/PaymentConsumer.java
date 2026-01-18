package com.example.paymentservice.service;


import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.enums.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final PaymentRepository paymentRepository;
    private final FraudService fraudService;

    @KafkaListener(
            topics = "payments",
            groupId = "payment-group"
    )
    public void consumePayment(UUID paymentId) {
        log.info("Received payment event for paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        log.info("Payment retried from DB  for paymentId={}", payment);
        boolean isFraudulent = fraudService.checkFraud(payment);

        payment.setStatus(
                isFraudulent ? PaymentStatus.FAILED : PaymentStatus.SUCCESS
        );

        paymentRepository.save(payment);

        log.info("Payment {} processed with status {}", paymentId, payment.getStatus());
    }
}
