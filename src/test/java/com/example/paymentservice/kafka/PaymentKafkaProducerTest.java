package com.example.paymentservice.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentservice.event.PaymentEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class PaymentKafkaProducerTest {

    @Mock private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @InjectMocks private PaymentKafkaProducer paymentKafkaProducer;

    @Test
    @SuppressWarnings("unchecked")
    void sendPaymentEvent_ShouldSendToKafkaSuccessfully() {
        // Arrange
        PaymentEvent event =
                PaymentEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("CREATE_PAYMENT")
                        .paymentId("test-payment-123")
                        .orderId(1001L)
                        .userId(501L)
                        .status("SUCCESS")
                        .amount(new BigDecimal("99.99"))
                        .timestamp(Instant.now())
                        .build();

        SendResult<String, PaymentEvent> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, PaymentEvent>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("payment-events"), eq("1001"), any(PaymentEvent.class)))
                .thenReturn(future);

        // Act
        paymentKafkaProducer.sendPaymentEvent(event);

        // Assert
        verify(kafkaTemplate, times(1))
                .send(eq("payment-events"), eq("1001"), any(PaymentEvent.class));
    }

    @Test
    void sendPaymentEvent_ShouldHandleException() {
        // Arrange
        PaymentEvent event =
                PaymentEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("CREATE_PAYMENT")
                        .paymentId("test-payment-123")
                        .orderId(1001L)
                        .userId(501L)
                        .status("SUCCESS")
                        .amount(new BigDecimal("99.99"))
                        .timestamp(Instant.now())
                        .build();

        CompletableFuture<SendResult<String, PaymentEvent>> future =
                CompletableFuture.failedFuture(new RuntimeException("Kafka error"));

        when(kafkaTemplate.send(eq("payment-events"), eq("1001"), any(PaymentEvent.class)))
                .thenReturn(future);

        // Act
        paymentKafkaProducer.sendPaymentEvent(event);

        // Assert - Should not throw exception, should be logged
        verify(kafkaTemplate, times(1))
                .send(eq("payment-events"), eq("1001"), any(PaymentEvent.class));
    }
}
