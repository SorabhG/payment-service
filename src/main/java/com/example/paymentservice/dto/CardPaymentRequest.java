package com.example.paymentservice.dto;


import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardPaymentRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotBlank
    @Pattern(regexp = "\\d{13,19}", message = "Invalid card number")
    private String cardNumber;

    @NotBlank
    private String cardHolderName;

    @Min(1)
    @Max(12)
    private Integer expiryMonth;

    @Min(2023)
    @Max(2050)
    private Integer expiryYear;

    @NotBlank
    @Pattern(regexp = "\\d{3,4}")
    private String cvv;
}

