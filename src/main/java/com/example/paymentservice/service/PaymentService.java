package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // Create Payment
    public Payment createPayment(Long orderId, Long userId, BigDecimal amount) {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(orderId)
                .userId(userId)
                .status("PENDING")
                .timestamp(Instant.now())
                .paymentAmount(amount)
                .build();

        return paymentRepository.save(payment);
    }

    // Get Payments by user_id or order_id or status
    public List<Payment> getPayments(Long userId, Long orderId, String status) {
        if (userId != null) {
            return paymentRepository.findByUserId(userId);
        } else if (orderId != null) {
            return paymentRepository.findByOrderId(orderId);
        } else if (status != null) {
            return paymentRepository.findByStatus(status);
        }
        return paymentRepository.findAll();
    }

    // Get total sum of payments for date range for current user
    public BigDecimal getTotalForUser(Long userId, Instant startDate, Instant endDate) {
        List<Payment> payments = paymentRepository.findByUserIdAndTimestampBetween(userId, startDate, endDate);
        return payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Get total sum of payments for date range for all users (for admin)
    public BigDecimal getTotalForAllUsers(Instant startDate, Instant endDate) {
        List<Payment> payments = paymentRepository.findByTimestampBetween(startDate, endDate);
        return payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}