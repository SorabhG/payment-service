package com.example.paymentservice.repository;


import com.example.paymentservice.entity.CardPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardPaymentRepository extends JpaRepository<CardPaymentDetails, Long> {}

