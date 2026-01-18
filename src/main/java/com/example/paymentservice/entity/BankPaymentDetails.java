package com.example.paymentservice.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankPaymentDetails {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false, length = 6)
    private String bsb;

    @Column(nullable = false)
    private String accountHolderName;

    private String bankName;
}
