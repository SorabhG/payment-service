package com.example.paymentservice.dto;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private UUID paymentId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime createdAt;
}

