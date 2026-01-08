package com.example.paymentservice.kafka;

import com.example.paymentservice.event.PaymentEvent;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void sendPaymentEvent(PaymentEvent event) {
        try {
            String key = String.valueOf(event.getOrderId());

            log.info("Sending payment event to Kafka. Key: '{}', Event: {}", key, event);

            CompletableFuture<SendResult<String, PaymentEvent>> future =
                    kafkaTemplate.send("payment-events", key, event);

            future.whenComplete(
                    (result, ex) -> {
                        if (ex == null) {
                            log.info(
                                    "Payment event sent successfully. Key: '{}', Partition: {}, Offset: {}, Topic: {}",
                                    key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset(),
                                    result.getRecordMetadata().topic());
                        } else {
                            log.error(
                                    "Failed to send payment event to Kafka. Key: '{}', Error: {}",
                                    key,
                                    ex.getMessage(),
                                    ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Exception while sending payment event to Kafka: {}", e.getMessage(), e);
        }
    }
}
