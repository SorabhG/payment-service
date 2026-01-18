package com.example.paymentservice.entity;

import com.example.paymentservice.entity.enums.PaymentStatus;
import com.example.paymentservice.entity.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ✅ New field for idempotency
    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    // ✅ One-to-one relationship with CardPaymentDetails
    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private CardPaymentDetails cardPaymentDetails;

    // ✅ One-to-one relationship with BankPaymentDetails
    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private BankPaymentDetails bankPaymentDetails;

    // Future: add PayPalPaymentDetails etc.
}
