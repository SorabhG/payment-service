package com.example.paymentservice.repository;


import com.example.paymentservice.entity.BankPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankPaymentRepository extends JpaRepository<BankPaymentDetails, Long> {}

