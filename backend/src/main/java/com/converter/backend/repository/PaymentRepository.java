package com.converter.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.converter.backend.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
