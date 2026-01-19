package com.example.paymentservice.validator;
import com.example.paymentservice.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RequestValidatorTest {

    private RequestValidator requestValidator;

    @BeforeEach
    void setUp() {
        requestValidator = new RequestValidator();
    }

    // ---------- validateIdempotencyKey ----------

    @Test
    void validateIdempotencyKey_shouldPass_whenKeyIsValid() {
        assertDoesNotThrow(() ->
                requestValidator.validateIdempotencyKey("valid-idempotency-key")
        );
    }

    @ParameterizedTest(name = "Should throw exception for invalid idempotencyKey = \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", " ", "   ", "\t"})
    void validateIdempotencyKey_shouldThrowException_whenKeyIsInvalid(String idempotencyKey) {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> requestValidator.validateIdempotencyKey(idempotencyKey)
        );

        assertEquals("Missing required header: Idempotency-Key", exception.getMessage());
    }

    // ---------- validateAmount ----------

    @Test
    void validateAmount_shouldPass_whenAmountIsPositive() {
        assertDoesNotThrow(() ->
                requestValidator.validateAmount(new BigDecimal("1.00"))
        );
    }

    @ParameterizedTest(name = "Should throw exception for invalid amount = {0}")
    @NullSource
    @ValueSource(strings = {"0", "-1", "-10.50"})
    void validateAmount_shouldThrowException_whenAmountIsInvalid(String amount) {
        BigDecimal value = (amount == null) ? null : new BigDecimal(amount);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> requestValidator.validateAmount(value)
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }
}
