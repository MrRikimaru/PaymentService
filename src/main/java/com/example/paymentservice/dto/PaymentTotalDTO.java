package com.example.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTotalDTO {
    private BigDecimal totalAmount;
    private Long count;

    public PaymentTotalDTO(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        this.count = 1L;
    }
}