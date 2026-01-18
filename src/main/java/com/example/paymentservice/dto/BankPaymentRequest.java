package com.example.paymentservice.dto;


import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankPaymentRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Invalid account number")
    private String accountNumber;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "BSB must be 6 digits")
    private String bsb;

    @NotBlank
    private String accountHolderName;

    private String bankName;
}

