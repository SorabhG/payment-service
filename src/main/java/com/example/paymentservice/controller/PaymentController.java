package com.example.paymentservice.controller;


import com.example.paymentservice.dto.BankPaymentRequest;
import com.example.paymentservice.dto.CardPaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.enums.PaymentStatus;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.validator.RequestValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RequestValidator requestValidator;
    // CREATE – Card
    @PreAuthorize("hasAuthority('SCOPE_payment.write')")
    @PostMapping("/card")
    public ResponseEntity<PaymentResponse> createCardPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid CardPaymentRequest request) throws BadRequestException {
        // Delegate validation
        requestValidator.validateIdempotencyKey(idempotencyKey);
        requestValidator.validateAmount(request.getAmount());
        return ResponseEntity.ok(paymentService.createCardPayment(request,idempotencyKey));
    }

    // CREATE – Bank
    // CREATE – Bank Payment
    @PreAuthorize("hasAuthority('SCOPE_payment.write')")
    @PostMapping("/bank")
    public ResponseEntity<PaymentResponse> createBankPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid BankPaymentRequest request) {
        requestValidator.validateIdempotencyKey(idempotencyKey);
        requestValidator.validateAmount(request.getAmount());
        return ResponseEntity.ok(paymentService.createBankPayment(request, idempotencyKey));
    }
    // READ – by ID
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    // READ – all payments
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }


    // UPDATE – payment status (admin / internal)
    @PutMapping("/{id}")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable UUID id,
            @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, status));
    }

    // DELETE – soft delete
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> deletePayment(@PathVariable UUID id) {
        paymentService.cancelPayment(id);
        return ResponseEntity.noContent().build();
    }
}
