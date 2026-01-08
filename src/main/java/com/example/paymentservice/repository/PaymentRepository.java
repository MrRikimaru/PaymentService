package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(String status);

    List<Payment> findByUserIdAndOrderId(Long userId, Long orderId);

    List<Payment> findByUserIdAndStatus(Long userId, String status);

    List<Payment> findByOrderIdAndStatus(Long orderId, String status);

    List<Payment> findByUserIdAndOrderIdAndStatus(Long userId, Long orderId, String status);

    List<Payment> findByTimestampBetween(Instant start, Instant end);

    @Query("{ 'userId' : ?0, 'timestamp' : { $gte: ?1, $lte: ?2 } }")
    List<Payment> findByUserIdAndTimestampBetween(Long userId, Instant startDate, Instant endDate);
}
