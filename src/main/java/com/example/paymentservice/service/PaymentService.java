package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequestDTO;
import com.example.paymentservice.dto.PaymentResponseDTO;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
    private final WebClient webClient;

    // External API URL for random number generation
    private static final String RANDOM_NUMBER_API = "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new";

    // 1. Create Payment with external API call
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

        return paymentMapper.toResponseDTO(savedPayment);
    }

    // 2. Get Payments by user_id or order_id or status
    public List<PaymentResponseDTO> getPayments(Long userId, Long orderId, String status) {
        List<Payment> payments;

        if (userId != null) {
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

    // 3. Get total sum of payments for date range for current user
    public BigDecimal getTotalForUser(Long userId, Instant startDate, Instant endDate) {
        List<Payment> payments = paymentRepository.findByUserIdAndTimestampBetween(userId, startDate, endDate);
        return payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 4. Get total sum of payments for date range for all users
    public BigDecimal getTotalForAllUsers(Instant startDate, Instant endDate) {
        List<Payment> payments = paymentRepository.findByTimestampBetween(startDate, endDate);
        return payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Private method to determine payment status from external API
    private String determinePaymentStatusFromExternalApi() {
        try {
            // Call external API to get random number
            String randomNumberStr = webClient.get()
                    .uri(RANDOM_NUMBER_API)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (randomNumberStr != null) {
                int randomNumber = Integer.parseInt(randomNumberStr.trim());
                log.debug("Random number from API: {}", randomNumber);

                // If number is even -> SUCCESS, otherwise -> FAILED
                return (randomNumber % 2 == 0) ? "SUCCESS" : "FAILED";
            }
        } catch (Exception e) {
            log.error("Error calling external API for random number: {}", e.getMessage());
        }

        // Fallback: return FAILED if API call fails
        return "FAILED";
    }
}