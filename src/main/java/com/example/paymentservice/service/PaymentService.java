package com.example.paymentservice.service;



import com.example.paymentservice.dto.BankPaymentRequest;
import com.example.paymentservice.dto.CardPaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.BankPaymentDetails;
import com.example.paymentservice.entity.CardPaymentDetails;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.enums.PaymentStatus;
import com.example.paymentservice.entity.enums.PaymentType;
import com.example.paymentservice.repository.BankPaymentRepository;
import com.example.paymentservice.repository.CardPaymentRepository;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardPaymentRepository cardRepo;
    private final BankPaymentRepository bankRepo;
    private final PaymentProducer paymentProducer;

    @Transactional
    public PaymentResponse createCardPayment(CardPaymentRequest request) {
        // TODO: Add Luhn validation
        Payment payment = Payment.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentType(PaymentType.CARD)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        CardPaymentDetails details = CardPaymentDetails.builder()
                .payment(payment)
                .cardNumber(maskCard(request.getCardNumber()))
                .cardHolderName(request.getCardHolderName())
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .cvv(request.getCvv())
                .build();

        cardRepo.save(details);


        // Publish Kafka event
        paymentProducer.sendPaymentEvent(payment.getId());
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    @Transactional
    public PaymentResponse createBankPayment(BankPaymentRequest request) {
        Payment payment = Payment.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentType(PaymentType.BANK)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        BankPaymentDetails details = BankPaymentDetails.builder()
                .payment(payment)
                .accountNumber(request.getAccountNumber())
                .bsb(request.getBsb())
                .accountHolderName(request.getAccountHolderName())
                .bankName(request.getBankName())
                .build();

        bankRepo.save(details);

        // TODO: Publish Kafka event for async fraud check
        // ✅ Publish Kafka event
        paymentProducer.sendPaymentEvent(payment.getId());
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .createdAt(payment.getCreatedAt())
                .build();
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

