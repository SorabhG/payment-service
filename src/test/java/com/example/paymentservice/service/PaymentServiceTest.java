package com.example.paymentservice.service;


import com.example.paymentservice.dto.BankPaymentRequest;
import com.example.paymentservice.dto.CardPaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.CardPaymentDetails;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.enums.PaymentStatus;
import com.example.paymentservice.entity.enums.PaymentType;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProducer paymentProducer;

    @InjectMocks
    private PaymentService paymentService;

    private CardPaymentRequest validCardPaymentRequest;
    private BankPaymentRequest validBankRequest;

    private static final String CARD_IDEMPOTENCY_KEY = "idem-123";
    private static final String BANK_IDEMPOTENCY_KEY = "bank-idem-1";

    @BeforeEach
    void setUp() {
        validCardPaymentRequest = CardPaymentRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("AUD")
                .cardNumber("4111111111111111")
                .cardHolderName("John Doe")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        validBankRequest = BankPaymentRequest.builder()
                .amount(new BigDecimal("250.00"))
                .currency("AUD")
                .accountNumber("12345678")
                .bsb("062000")
                .accountHolderName("Jane Doe")
                .bankName("Commonwealth Bank")
                .build();
    }

    // ----------------------
    // Helper Methods
    // ----------------------
    private Payment buildPayment(UUID id, PaymentType type, PaymentStatus status, BigDecimal amount) {
        Payment payment = Payment.builder()
                .id(id)
                .amount(amount)
                .currency("AUD")
                .paymentType(type)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        if (type == PaymentType.CARD) {
            payment.setCardPaymentDetails(CardPaymentDetails.builder()
                    .cardNumber("4111111111111111")
                    .cardHolderName("John Doe")
                    .expiryMonth(12)
                    .expiryYear(2028)
                    .cvv("123")
                    .payment(payment)
                    .build());
        }
        return payment;
    }

    private void assertPaymentResponse(PaymentResponse response, Payment payment) {
        assertEquals(payment.getId(), response.getPaymentId());
        assertEquals(payment.getStatus().name(), response.getStatus());
        assertEquals(payment.getAmount(), response.getAmount());
        assertEquals(payment.getCurrency(), response.getCurrency());
    }

    // ----------------------
    // Card Payment Tests
    // ----------------------
    @Nested
    class CardPaymentTests {

        @Test
        void createCardPayment_shouldCreatePaymentAndPublishEvent_whenNewRequest() {
            when(paymentRepository.findByIdempotencyKey(CARD_IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            PaymentResponse response = paymentService.createCardPayment(validCardPaymentRequest, CARD_IDEMPOTENCY_KEY);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment savedPayment = captor.getValue();

            assertEquals(PaymentType.CARD, savedPayment.getPaymentType());
            assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
            assertNotNull(savedPayment.getCardPaymentDetails());
            assertEquals(savedPayment, savedPayment.getCardPaymentDetails().getPayment());

            verify(paymentProducer).sendPaymentEvent(savedPayment.getId());
            assertPaymentResponse(response, savedPayment);
        }

        @Test
        void createCardPayment_shouldReturnExistingPayment_whenIdempotencyKeyExists() {
            Payment existing = buildPayment(UUID.randomUUID(), PaymentType.CARD, PaymentStatus.PENDING, new BigDecimal("50.00"));
            when(paymentRepository.findByIdempotencyKey(CARD_IDEMPOTENCY_KEY)).thenReturn(Optional.of(existing));

            PaymentResponse response = paymentService.createCardPayment(validCardPaymentRequest, CARD_IDEMPOTENCY_KEY);

            verify(paymentRepository, never()).save(any());
            verify(paymentProducer, never()).sendPaymentEvent(any());
            assertPaymentResponse(response, existing);
        }

        @Test
        void createCardPayment_shouldReturnExistingPayment_whenRaceConditionOccurs() {
            Payment existing = buildPayment(UUID.randomUUID(), PaymentType.CARD, PaymentStatus.PENDING, new BigDecimal("75.00"));
            when(paymentRepository.findByIdempotencyKey(CARD_IDEMPOTENCY_KEY))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(existing));
            when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

            PaymentResponse response = paymentService.createCardPayment(validCardPaymentRequest, CARD_IDEMPOTENCY_KEY);

            verify(paymentProducer, never()).sendPaymentEvent(any());
            assertPaymentResponse(response, existing);
        }
    }

    // ----------------------
    // Bank Payment Tests
    // ----------------------
    @Nested
    class BankPaymentTests {

        @Test
        void createBankPayment_shouldCreatePaymentAndPublishEvent_whenNewRequest() {
            when(paymentRepository.findByIdempotencyKey(BANK_IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            PaymentResponse response = paymentService.createBankPayment(validBankRequest, BANK_IDEMPOTENCY_KEY);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment saved = captor.getValue();

            assertEquals(PaymentType.BANK, saved.getPaymentType());
            assertEquals(PaymentStatus.PENDING, saved.getStatus());
            assertNotNull(saved.getBankPaymentDetails());
            assertEquals(saved, saved.getBankPaymentDetails().getPayment());

            verify(paymentProducer).sendPaymentEvent(saved.getId());
            assertPaymentResponse(response, saved);
        }

        @Test
        void createBankPayment_shouldReturnExistingPayment_whenIdempotencyKeyExists() {
            Payment existing = buildPayment(UUID.randomUUID(), PaymentType.BANK, PaymentStatus.PENDING, new BigDecimal("250.00"));
            when(paymentRepository.findByIdempotencyKey(BANK_IDEMPOTENCY_KEY)).thenReturn(Optional.of(existing));

            PaymentResponse response = paymentService.createBankPayment(validBankRequest, BANK_IDEMPOTENCY_KEY);

            verify(paymentRepository, never()).save(any());
            verify(paymentProducer, never()).sendPaymentEvent(any());
            assertPaymentResponse(response, existing);
        }

        @Test
        void createBankPayment_shouldReturnExistingPayment_whenRaceConditionOccurs() {
            Payment existing = buildPayment(UUID.randomUUID(), PaymentType.BANK, PaymentStatus.PENDING, new BigDecimal("250.00"));
            when(paymentRepository.findByIdempotencyKey(BANK_IDEMPOTENCY_KEY))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(existing));
            when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

            PaymentResponse response = paymentService.createBankPayment(validBankRequest, BANK_IDEMPOTENCY_KEY);

            verify(paymentProducer, never()).sendPaymentEvent(any());
            assertPaymentResponse(response, existing);
        }
    }

    // ----------------------
    // Cancel Payment Tests
    // ----------------------
    @Nested
    class CancelPaymentTests {

        @ParameterizedTest(name = "Should cancel payment when status = {0}")
        @EnumSource(value = PaymentStatus.class, names = {"PENDING", "FAILED"})
        void cancelPayment_shouldCancelPayment_whenStatusAllows(PaymentStatus initialStatus) {
            Payment payment = buildPayment(UUID.randomUUID(), PaymentType.CARD, initialStatus, new BigDecimal("100.00"));
            when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.cancelPayment(payment.getId());

            assertEquals("CANCELLED", response.getStatus());
            verify(paymentRepository).save(payment);
            assertNotNull(payment.getUpdatedAt());
        }

        @Test
        void cancelPayment_shouldThrowException_whenPaymentIsSuccessful() {
            Payment payment = buildPayment(UUID.randomUUID(), PaymentType.CARD, PaymentStatus.SUCCESS, new BigDecimal("200.00"));
            when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> paymentService.cancelPayment(payment.getId()));
            assertEquals("Completed payments cannot be cancelled", ex.getMessage());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        void cancelPayment_shouldThrowException_whenPaymentNotFound() {
            UUID id = UUID.randomUUID();
            when(paymentRepository.findById(id)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> paymentService.cancelPayment(id));
            assertEquals("Payment not found", ex.getMessage());
            verify(paymentRepository, never()).save(any());
        }
    }

    // ----------------------
    // Update Payment Status Tests
    // ----------------------
    @Nested
    class UpdatePaymentStatusTests {

        @ParameterizedTest(name = "Should update payment status to {0}")
        @EnumSource(PaymentStatus.class)
        void updatePaymentStatus_shouldUpdateStatus_whenPaymentExists(PaymentStatus newStatus) {
            Payment payment = buildPayment(UUID.randomUUID(), PaymentType.CARD, PaymentStatus.PENDING, new BigDecimal("150.00"));
            when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.updatePaymentStatus(payment.getId(), newStatus);

            assertEquals(newStatus.name(), response.getStatus());
            assertEquals(newStatus, payment.getStatus());
            assertNotNull(payment.getUpdatedAt());
            verify(paymentRepository).save(payment);
        }

        @Test
        void updatePaymentStatus_shouldThrowException_whenPaymentNotFound() {
            UUID id = UUID.randomUUID();
            when(paymentRepository.findById(id)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> paymentService.updatePaymentStatus(id, PaymentStatus.REFUND));
            assertEquals("Payment not found", ex.getMessage());
            verify(paymentRepository, never()).save(any());
        }
    }

    // ----------------------
    // Read Payment Tests
    // ----------------------
    @Nested
    class ReadPaymentTests {

        @Test
        void getPaymentById_shouldReturnPaymentResponse_whenPaymentExists() {
            Payment payment = buildPayment(UUID.randomUUID(), PaymentType.CARD, PaymentStatus.SUCCESS, new BigDecimal("300.00"));
            when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.getPaymentById(payment.getId());
            assertPaymentResponse(response, payment);
        }

        @Test
        void getPaymentById_shouldThrowException_whenPaymentNotFound() {
            UUID id = UUID.randomUUID();
            when(paymentRepository.findById(id)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> paymentService.getPaymentById(id));
            assertEquals("Payment not found", ex.getMessage());
        }

        @Test
        void getAllPayments_shouldReturnAllPayments() {
            Payment p1 = buildPayment(UUID.randomUUID(), PaymentType.CARD, PaymentStatus.PENDING, new BigDecimal("100.00"));
            Payment p2 = buildPayment(UUID.randomUUID(), PaymentType.BANK, PaymentStatus.SUCCESS, new BigDecimal("200.00"));
            when(paymentRepository.findAll()).thenReturn(List.of(p1, p2));

            List<PaymentResponse> responses = paymentService.getAllPayments();
            assertEquals(2, responses.size());
            assertPaymentResponse(responses.get(0), p1);
            assertPaymentResponse(responses.get(1), p2);
        }

        @Test
        void getAllPayments_shouldReturnEmptyList_whenNoPaymentsExist() {
            when(paymentRepository.findAll()).thenReturn(List.of());
            List<PaymentResponse> responses = paymentService.getAllPayments();
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
        }
    }
}
