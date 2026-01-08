package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentRequestDTO;
import com.example.paymentservice.dto.PaymentResponseDTO;
import com.example.paymentservice.dto.PaymentTotalDTO;
import com.example.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @Valid @RequestBody PaymentRequestDTO paymentRequestDTO) {
        PaymentResponseDTO payment = paymentService.createPayment(paymentRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> getPayments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status) {
        List<PaymentResponseDTO> payments = paymentService.getPayments(userId, orderId, status);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<PaymentTotalDTO> getUserTotal(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        PaymentTotalDTO total = paymentService.getTotalForUser(userId, startDate, endDate);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/admin/total")
    public ResponseEntity<PaymentTotalDTO> getAllUsersTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        PaymentTotalDTO total = paymentService.getTotalForAllUsers(startDate, endDate);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByUserId(@PathVariable Long userId) {
        List<PaymentResponseDTO> payments = paymentService.getPayments(userId, null, null);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByOrderId(
            @PathVariable Long orderId) {
        List<PaymentResponseDTO> payments = paymentService.getPayments(null, orderId, null);
        return ResponseEntity.ok(payments);
    }
}
