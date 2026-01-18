package com.example.paymentservice.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardPaymentDetails {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false)
    private String cardNumber; // Store masked version

    @Column(nullable = false)
    private String cardHolderName;

    @Column(nullable = false)
    private Integer expiryMonth;

    @Column(nullable = false)
    private Integer expiryYear;

    @Column(nullable = false)
    private String cvv;
}

