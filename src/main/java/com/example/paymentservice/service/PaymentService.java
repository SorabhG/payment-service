package com.example.paymentservice.service;


import com.example.paymentservice.dto.BankPaymentRequest;
import com.example.paymentservice.dto.CardPaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.BankPaymentDetails;
import com.example.paymentservice.entity.CardPaymentDetails;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.enums.PaymentStatus;
import com.example.paymentservice.entity.enums.PaymentType;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;


    @Transactional
    public PaymentResponse createCardPayment(CardPaymentRequest request, String idempotencyKey) {

        // 🔁 Check if this request was already processed
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        // Build parent Payment entity
        Payment payment = Payment.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentType(PaymentType.CARD)
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Build child CardPaymentDetails entity
        CardPaymentDetails cardDetails = CardPaymentDetails.builder()
                .cardNumber(request.getCardNumber())
                .cardHolderName(request.getCardHolderName())
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .cvv(request.getCvv())
                .payment(payment) // 🔑 link to parent
                .build();

        // Attach child to parent for cascading
        payment.setCardPaymentDetails(cardDetails);

        try {
            // Save parent, cascade saves child
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            // Handle race condition for idempotency key
            Payment existingPayment = paymentRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();
            return mapToResponse(existingPayment);
        }

        // ✅ Publish Kafka event
        paymentProducer.sendPaymentEvent(payment.getId());
        return mapToResponse(payment);
    }

    @Transactional
    public PaymentResponse createBankPayment(BankPaymentRequest request, String idempotencyKey) {

        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        Payment payment = Payment.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentType(PaymentType.BANK)
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BankPaymentDetails bankDetails = BankPaymentDetails.builder()
                .accountNumber(request.getAccountNumber())
                .bsb(request.getBsb())
                .accountHolderName(request.getAccountHolderName())
                .bankName(request.getBankName())
                .payment(payment)
                .build();

        payment.setBankPaymentDetails(bankDetails);

        try {
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            Payment existingPayment = paymentRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();
            return mapToResponse(existingPayment);
        }
        paymentProducer.sendPaymentEvent(payment.getId());
        return mapToResponse(payment);
    }


    public PaymentResponse getPaymentById(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        return mapToResponse(payment);
    }

    //@Transactional
    public PaymentResponse cancelPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Completed payments cannot be cancelled");
        }
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
        // Optional Kafka event
        //paymentProducer.sendPaymentCancelledEvent(payment.getId());
        return mapToResponse(payment);
    }
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public PaymentResponse updatePaymentStatus(UUID id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(status);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return mapToResponse(payment);
    }

    private String maskCard(String cardNumber) {
        // Keep last 4 digits only
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
    // Mapper to Response DTO
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .createdAt(payment.getCreatedAt())
                .build();
    }

}

