package com.example.paymentservice.service;


import com.example.paymentservice.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class FraudService {

    private final Random random = new Random();

    public boolean checkFraud(Payment payment) {
        // Simulate fraud logic
        boolean fraud = random.nextInt(10) < 2; // 20% fraud rate

        log.info("Fraud check for paymentId={} result={}",
                payment.getId(), fraud ? "FRAUD" : "CLEAN");

        return fraud;
    }
}

