package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(String status);

    List<Payment> findByTimestampBetween(Instant start, Instant end);

    List<Payment> findByUserIdAndTimestampBetween(Long userId, Instant start, Instant end);
}