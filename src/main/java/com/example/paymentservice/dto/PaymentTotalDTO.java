package com.example.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
public class PaymentTotalDTO {
    private BigDecimal totalAmount;
    private Long count;

    public PaymentTotalDTO(BigDecimal totalAmount, Long count) {
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.count = count != null ? count : 0L;
    }
}