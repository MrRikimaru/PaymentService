package com.example.paymentservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentservice.dto.PaymentRequestDTO;
import com.example.paymentservice.dto.PaymentResponseDTO;
import com.example.paymentservice.dto.PaymentTotalDTO;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.event.PaymentEvent;
import com.example.paymentservice.kafka.PaymentKafkaProducer;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock private PaymentRepository paymentRepository;

    @Mock private PaymentMapper paymentMapper;

    @Mock private RandomNumberService randomNumberService;

    @Mock private PaymentKafkaProducer paymentKafkaProducer;

    @InjectMocks private PaymentService paymentService;

    private PaymentRequestDTO paymentRequestDTO;
    private Payment payment;
    private PaymentResponseDTO paymentResponseDTO;

    @BeforeEach
    void setUp() {
        paymentRequestDTO =
                PaymentRequestDTO.builder()
                        .orderId(1001L)
                        .userId(501L)
                        .amount(new BigDecimal("99.99"))
                        .build();

        payment =
                Payment.builder()
                        .id("test-payment-id")
                        .orderId(1001L)
                        .userId(501L)
                        .status("SUCCESS")
                        .timestamp(Instant.now())
                        .paymentAmount(new BigDecimal("99.99"))
                        .build();

        paymentResponseDTO =
                PaymentResponseDTO.builder()
                        .id("test-payment-id")
                        .orderId(1001L)
                        .userId(501L)
                        .status("SUCCESS")
                        .timestamp(Instant.now())
                        .paymentAmount(new BigDecimal("99.99"))
                        .build();
    }

    @Test
    void createPayment_ShouldReturnSuccess_WhenRandomNumberIsEven() {
        // Arrange
        when(randomNumberService.getRandomNumber()).thenReturn(Mono.just(42));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(paymentResponseDTO);

        // Act
        PaymentResponseDTO result = paymentService.createPayment(paymentRequestDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentKafkaProducer, times(1)).sendPaymentEvent(any(PaymentEvent.class));
    }

    @Test
    void createPayment_ShouldReturnFailed_WhenRandomNumberIsOdd() {
        // Arrange
        when(randomNumberService.getRandomNumber()).thenReturn(Mono.just(41));
        Payment failedPayment =
                Payment.builder()
                        .id(payment.getId())
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .status("FAILED")
                        .timestamp(payment.getTimestamp())
                        .paymentAmount(payment.getPaymentAmount())
                        .build();

        PaymentResponseDTO failedResponse =
                PaymentResponseDTO.builder()
                        .id(paymentResponseDTO.getId())
                        .orderId(paymentResponseDTO.getOrderId())
                        .userId(paymentResponseDTO.getUserId())
                        .status("FAILED")
                        .timestamp(paymentResponseDTO.getTimestamp())
                        .paymentAmount(paymentResponseDTO.getPaymentAmount())
                        .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(failedPayment);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(failedResponse);

        // Act
        PaymentResponseDTO result = paymentService.createPayment(paymentRequestDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentKafkaProducer, times(1)).sendPaymentEvent(any(PaymentEvent.class));
    }

    @Test
    void createPayment_ShouldReturnFailed_WhenRandomNumberServiceFails() {
        // Arrange
        when(randomNumberService.getRandomNumber())
                .thenReturn(Mono.error(new RuntimeException("API Error")));
        Payment failedPayment =
                Payment.builder()
                        .id(payment.getId())
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .status("FAILED")
                        .timestamp(payment.getTimestamp())
                        .paymentAmount(payment.getPaymentAmount())
                        .build();

        PaymentResponseDTO failedResponse =
                PaymentResponseDTO.builder()
                        .id(paymentResponseDTO.getId())
                        .orderId(paymentResponseDTO.getOrderId())
                        .userId(paymentResponseDTO.getUserId())
                        .status("FAILED")
                        .timestamp(paymentResponseDTO.getTimestamp())
                        .paymentAmount(paymentResponseDTO.getPaymentAmount())
                        .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(failedPayment);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(failedResponse);

        // Act
        PaymentResponseDTO result = paymentService.createPayment(paymentRequestDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void getPayments_ShouldReturnAllPayments_WhenNoFilters() {
        // Arrange
        List<Payment> payments = List.of(payment);
        when(paymentRepository.findAll()).thenReturn(payments);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(paymentResponseDTO);

        // Act
        List<PaymentResponseDTO> result = paymentService.getPayments(null, null, null);

        // Assert
        assertThat(result).hasSize(1);
        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    void getPayments_ShouldFilterByUserId() {
        // Arrange
        List<Payment> payments = List.of(payment);
        when(paymentRepository.findByUserId(501L)).thenReturn(payments);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(paymentResponseDTO);

        // Act
        List<PaymentResponseDTO> result = paymentService.getPayments(501L, null, null);

        // Assert
        assertThat(result).hasSize(1);
        verify(paymentRepository, times(1)).findByUserId(501L);
    }

    @Test
    void getPayments_ShouldFilterByOrderId() {
        // Arrange
        List<Payment> payments = List.of(payment);
        when(paymentRepository.findByOrderId(1001L)).thenReturn(payments);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(paymentResponseDTO);

        // Act
        List<PaymentResponseDTO> result = paymentService.getPayments(null, 1001L, null);

        // Assert
        assertThat(result).hasSize(1);
        verify(paymentRepository, times(1)).findByOrderId(1001L);
    }

    @Test
    void getPayments_ShouldFilterByStatus() {
        // Arrange
        List<Payment> payments = List.of(payment);
        when(paymentRepository.findByStatus("SUCCESS")).thenReturn(payments);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(paymentResponseDTO);

        // Act
        List<PaymentResponseDTO> result = paymentService.getPayments(null, null, "SUCCESS");

        // Assert
        assertThat(result).hasSize(1);
        verify(paymentRepository, times(1)).findByStatus("SUCCESS");
    }

    @Test
    void getPayments_ShouldFilterByUserIdAndOrderId() {
        // Arrange
        List<Payment> payments = List.of(payment);
        when(paymentRepository.findByUserIdAndOrderId(501L, 1001L)).thenReturn(payments);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(paymentResponseDTO);

        // Act
        List<PaymentResponseDTO> result = paymentService.getPayments(501L, 1001L, null);

        // Assert
        assertThat(result).hasSize(1);
        verify(paymentRepository, times(1)).findByUserIdAndOrderId(501L, 1001L);
    }

    @Test
    void getPayments_ShouldFilterByUserIdAndStatus() {
        // Arrange
        List<Payment> payments = List.of(payment);
        when(paymentRepository.findByUserIdAndStatus(501L, "SUCCESS")).thenReturn(payments);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(paymentResponseDTO);

        // Act
        List<PaymentResponseDTO> result = paymentService.getPayments(501L, null, "SUCCESS");

        // Assert
        assertThat(result).hasSize(1);
        verify(paymentRepository, times(1)).findByUserIdAndStatus(501L, "SUCCESS");
    }

    @Test
    void getPayments_ShouldFilterByOrderIdAndStatus() {
        // Arrange
        List<Payment> payments = List.of(payment);
        when(paymentRepository.findByOrderIdAndStatus(1001L, "SUCCESS")).thenReturn(payments);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(paymentResponseDTO);

        // Act
        List<PaymentResponseDTO> result = paymentService.getPayments(null, 1001L, "SUCCESS");

        // Assert
        assertThat(result).hasSize(1);
        verify(paymentRepository, times(1)).findByOrderIdAndStatus(1001L, "SUCCESS");
    }

    @Test
    void getPayments_ShouldFilterByAllParameters() {
        // Arrange
        List<Payment> payments = List.of(payment);
        when(paymentRepository.findByUserIdAndOrderIdAndStatus(501L, 1001L, "SUCCESS"))
                .thenReturn(payments);
        when(paymentMapper.toResponseDTO(any(Payment.class))).thenReturn(paymentResponseDTO);

        // Act
        List<PaymentResponseDTO> result = paymentService.getPayments(501L, 1001L, "SUCCESS");

        // Assert
        assertThat(result).hasSize(1);
        verify(paymentRepository, times(1)).findByUserIdAndOrderIdAndStatus(501L, 1001L, "SUCCESS");
    }

    @Test
    void getTotalForUser_ShouldCalculateCorrectTotal() {
        // Arrange
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        Payment payment1 =
                Payment.builder()
                        .id("1")
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .status(payment.getStatus())
                        .timestamp(payment.getTimestamp())
                        .paymentAmount(new BigDecimal("50.00"))
                        .build();

        Payment payment2 =
                Payment.builder()
                        .id("2")
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .status(payment.getStatus())
                        .timestamp(payment.getTimestamp())
                        .paymentAmount(new BigDecimal("75.50"))
                        .build();

        List<Payment> payments = Arrays.asList(payment1, payment2);

        when(paymentRepository.findByUserIdAndTimestampBetween(
                        eq(501L), any(Instant.class), any(Instant.class)))
                .thenReturn(payments);

        // Act
        PaymentTotalDTO result = paymentService.getTotalForUser(501L, startDate, endDate);

        // Assert
        assertThat(result.getTotalAmount()).isEqualByComparingTo("125.50");
        assertThat(result.getCount()).isEqualTo(2);
    }

    @Test
    void getTotalForAllUsers_ShouldCalculateCorrectTotal() {
        // Arrange
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        Payment payment1 =
                Payment.builder()
                        .id("1")
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .status(payment.getStatus())
                        .timestamp(payment.getTimestamp())
                        .paymentAmount(new BigDecimal("100.00"))
                        .build();

        Payment payment2 =
                Payment.builder()
                        .id("2")
                        .orderId(payment.getOrderId())
                        .userId(payment.getUserId())
                        .status(payment.getStatus())
                        .timestamp(payment.getTimestamp())
                        .paymentAmount(new BigDecimal("200.00"))
                        .build();

        List<Payment> payments = Arrays.asList(payment1, payment2);

        when(paymentRepository.findByTimestampBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(payments);

        // Act
        PaymentTotalDTO result = paymentService.getTotalForAllUsers(startDate, endDate);

        // Assert
        assertThat(result.getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(result.getCount()).isEqualTo(2);
    }

    @Test
    void getTotalForUser_ShouldReturnZero_WhenNoPaymentsFound() {
        // Arrange
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        when(paymentRepository.findByUserIdAndTimestampBetween(
                        eq(501L), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        // Act
        PaymentTotalDTO result = paymentService.getTotalForUser(501L, startDate, endDate);

        // Assert
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getCount()).isEqualTo(0);
    }
}
