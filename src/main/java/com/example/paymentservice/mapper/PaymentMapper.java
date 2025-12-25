package com.example.paymentservice.mapper;

import com.example.paymentservice.dto.PaymentRequestDTO;
import com.example.paymentservice.dto.PaymentResponseDTO;
import com.example.paymentservice.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toEntity(PaymentRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Payment.builder()
                .orderId(dto.getOrderId())
                .userId(dto.getUserId())
                .paymentAmount(dto.getAmount())
                .build();
    }

    public PaymentResponseDTO toResponseDTO(Payment entity) {
        if (entity == null) {
            return null;
        }

        return PaymentResponseDTO.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .timestamp(entity.getTimestamp())
                .paymentAmount(entity.getPaymentAmount())
                .build();
    }

    public PaymentRequestDTO toRequestDTO(Payment entity) {
        if (entity == null) {
            return null;
        }

        return PaymentRequestDTO.builder()
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .amount(entity.getPaymentAmount())
                .build();
    }
}