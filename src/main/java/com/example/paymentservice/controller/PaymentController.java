package com.example.paymentservice.controller;


import com.example.paymentservice.dto.BankPaymentRequest;
import com.example.paymentservice.dto.CardPaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.enums.PaymentStatus;
import com.example.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    // CREATE – Card
    @PreAuthorize("hasAuthority('SCOPE_payment.write')")
    @PostMapping("/card")
    public ResponseEntity<PaymentResponse> createCardPayment(
            @RequestBody @Valid CardPaymentRequest request) {
        return ResponseEntity.ok(paymentService.createCardPayment(request));
    }

    // CREATE – Bank
    @PostMapping("/bank")
    public ResponseEntity<PaymentResponse> createBankPayment(
            @RequestBody @Valid BankPaymentRequest request) {
        return ResponseEntity.ok(paymentService.createBankPayment(request));
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable UUID id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
