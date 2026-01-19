package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FraudServiceTest {

    private FraudService fraudService;

    @BeforeEach
    void setUp() {
        fraudService = new FraudService();
    }

    @Test
    void shouldReturnFalse_whenAmountIsLessThan15000() {
        // given
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setAmount(BigDecimal.valueOf(10000));

        // when
        boolean result = fraudService.checkFraud(payment);

        // then
        assertFalse(result, "Payment below 15000 should NOT be fraudulent");
    }

    @Test
    void shouldReturnFalse_whenAmountIsExactly15000() {
        // given
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setAmount(BigDecimal.valueOf(15000));

        // when
        boolean result = fraudService.checkFraud(payment);

        // then
        assertFalse(result, "Payment equal to 15000 should NOT be fraudulent");
    }

    @Test
    void shouldReturnTrue_whenAmountIsGreaterThan15000() {
        // given
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setAmount(BigDecimal.valueOf(15000.01));

        // when
        boolean result = fraudService.checkFraud(payment);

        // then
        assertTrue(result, "Payment above 15000 should be fraudulent");
    }
}
