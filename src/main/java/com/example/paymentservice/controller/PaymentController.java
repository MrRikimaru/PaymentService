package com.example.paymentservice.controller;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestParam Long orderId,
            @RequestParam Long userId,
            @RequestParam BigDecimal amount) {
        Payment payment = paymentService.createPayment(orderId, userId, amount);
        return ResponseEntity.ok(payment);
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getPayments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status) {
        List<Payment> payments = paymentService.getPayments(userId, orderId, status);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<BigDecimal> getUserTotal(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        BigDecimal total = paymentService.getTotalForUser(userId, startDate, endDate);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/admin/total")
    public ResponseEntity<BigDecimal> getAllUsersTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        BigDecimal total = paymentService.getTotalForAllUsers(startDate, endDate);
        return ResponseEntity.ok(total);
    }
}