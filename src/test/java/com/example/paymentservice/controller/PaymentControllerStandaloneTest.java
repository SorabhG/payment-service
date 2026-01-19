package com.example.paymentservice.controller;

import com.example.paymentservice.dto.BankPaymentRequest;
import com.example.paymentservice.dto.CardPaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.enums.PaymentStatus;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.validator.RequestValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerStandaloneTest {

    private MockMvc mockMvc;
    private PaymentService paymentService;
    private RequestValidator requestValidator;
    private ObjectMapper objectMapper;

    private UUID paymentId;
    private PaymentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        paymentService = Mockito.mock(PaymentService.class);
        requestValidator = Mockito.mock(RequestValidator.class);
        objectMapper = new ObjectMapper();

        PaymentController controller = new PaymentController(paymentService, requestValidator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        paymentId = UUID.randomUUID();
        sampleResponse = PaymentResponse.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.SUCCESS.name())
                .amount(BigDecimal.valueOf(100))
                .currency("USD")
                .build();
    }

    @Test
    void testCreateCardPayment() throws Exception {
        CardPaymentRequest request = new CardPaymentRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("USD");
        request.setCardHolderName("John Doe");
        request.setCardNumber("4111111111111111");
        request.setCvv("123");

        Mockito.doNothing().when(requestValidator).validateIdempotencyKey(anyString());
        Mockito.doNothing().when(requestValidator).validateAmount(any());
        when(paymentService.createCardPayment(any(), anyString())).thenReturn(sampleResponse);

        mockMvc.perform(post("/payments/card")
                        .header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testCreateBankPayment() throws Exception {
        BankPaymentRequest request = new BankPaymentRequest();
        request.setAmount(BigDecimal.valueOf(200));
        request.setCurrency("AUD");
        request.setBsb("123456");
        request.setAccountNumber("987654321");
        request.setAccountHolderName("John Doe");

        Mockito.doNothing().when(requestValidator).validateIdempotencyKey(anyString());
        Mockito.doNothing().when(requestValidator).validateAmount(any());
        when(paymentService.createBankPayment(any(), anyString())).thenReturn(sampleResponse);

        mockMvc.perform(post("/payments/bank")
                        .header("Idempotency-Key", "bank-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testGetPaymentById() throws Exception {
        when(paymentService.getPaymentById(paymentId)).thenReturn(sampleResponse);

        mockMvc.perform(get("/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testGetAllPayments() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(Collections.singletonList(sampleResponse));

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    void testUpdatePaymentStatus() throws Exception {
        when(paymentService.updatePaymentStatus(eq(paymentId), eq(PaymentStatus.SUCCESS)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/payments/{id}", paymentId)
                        .param("status", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testDeletePayment() throws Exception {
        PaymentResponse cancelledResponse = PaymentResponse.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.CANCELLED.name())
                .amount(BigDecimal.valueOf(100))
                .currency("AUD")
                .build();

        when(paymentService.cancelPayment(paymentId)).thenReturn(cancelledResponse);

        mockMvc.perform(patch("/payments/{id}/cancel", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
