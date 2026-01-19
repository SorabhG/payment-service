package com.example.paymentservice.validator;


import com.example.paymentservice.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RequestValidator {

    public void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Missing required header: Idempotency-Key");
        }
    }

    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
    }
}

