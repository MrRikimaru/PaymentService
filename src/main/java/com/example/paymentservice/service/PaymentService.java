package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequestDTO;
import com.example.paymentservice.dto.PaymentResponseDTO;
import com.example.paymentservice.dto.PaymentTotalDTO;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.event.PaymentEvent;
import com.example.paymentservice.kafka.PaymentKafkaProducer;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberService randomNumberService;
    private final PaymentKafkaProducer paymentKafkaProducer;

    public PaymentResponseDTO createPayment(PaymentRequestDTO paymentRequestDTO) {
        // Determine payment status using external API
        String status = determinePaymentStatusFromExternalApi();

        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(paymentRequestDTO.getOrderId())
                .userId(paymentRequestDTO.getUserId())
                .status(status)
                .timestamp(Instant.now())
                .paymentAmount(paymentRequestDTO.getAmount())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with status: {}, ID: {}", status, savedPayment.getId());

        // Send payment event to Kafka
        sendPaymentEventToKafka(savedPayment);

        return paymentMapper.toResponseDTO(savedPayment);
    }

    private void sendPaymentEventToKafka(Payment payment) {
        try {
            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("CREATE_PAYMENT")
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .status(payment.getStatus())
                    .amount(payment.getPaymentAmount())
                    .timestamp(payment.getTimestamp())
                    .build();

            log.info("Sending payment event for order: {}, status: {}",
                    payment.getOrderId(), payment.getStatus());

            paymentKafkaProducer.sendPaymentEvent(paymentEvent);
        } catch (Exception e) {
            log.error("Failed to send payment event to Kafka for order {}: {}",
                    payment.getOrderId(), e.getMessage(), e);
        }
    }

    // Get Payments by user_id or order_id or status
    public List<PaymentResponseDTO> getPayments(Long userId, Long orderId, String status) {
        List<Payment> payments;

        if (userId != null && orderId != null && status != null) {
            payments = paymentRepository.findByUserIdAndOrderIdAndStatus(userId, orderId, status);
        } else if (userId != null && orderId != null) {
            payments = paymentRepository.findByUserIdAndOrderId(userId, orderId);
        } else if (userId != null && status != null) {
            payments = paymentRepository.findByUserIdAndStatus(userId, status);
        } else if (orderId != null && status != null) {
            payments = paymentRepository.findByOrderIdAndStatus(orderId, status);
        } else if (userId != null) {
            payments = paymentRepository.findByUserId(userId);
        } else if (orderId != null) {
            payments = paymentRepository.findByOrderId(orderId);
        } else if (status != null) {
            payments = paymentRepository.findByStatus(status);
        } else {
            payments = paymentRepository.findAll();
        }

        return payments.stream()
                .map(paymentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Get total sum of payments for date range for current user
    public PaymentTotalDTO getTotalForUser(Long userId, Instant startDate, Instant endDate) {
        List<Payment> payments = paymentRepository.findByUserIdAndTimestampBetween(userId, startDate, endDate);
        BigDecimal total = payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PaymentTotalDTO(total, (long) payments.size());
    }


    // Get total sum of payments for date range for all users
    public PaymentTotalDTO getTotalForAllUsers(Instant startDate, Instant endDate) {
        List<Payment> payments = paymentRepository.findByTimestampBetween(startDate, endDate);
        BigDecimal total = payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PaymentTotalDTO(total, (long) payments.size());
    }

    // Private method to determine payment status from external API
    private String determinePaymentStatusFromExternalApi() {
        try {
            Integer randomNumber = randomNumberService.getRandomNumber().block();

            if (randomNumber != null) {
                // If number is even -> SUCCESS, otherwise -> FAILED
                return (randomNumber % 2 == 0) ? "SUCCESS" : "FAILED";
            }
        } catch (Exception e) {
            log.error("Error determining payment status: {}", e.getMessage());
        }

        // Fallback: return FAILED if API call fails
        return "FAILED";
    }
}