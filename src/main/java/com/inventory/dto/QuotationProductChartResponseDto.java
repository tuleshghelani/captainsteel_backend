package com.inventory.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationProductChartResponseDto {
    private Long productId;
    private String productName;
    private String productType;
    private Long quotationCount; // Number of quotations containing this product
    private Long itemCount; // Total quantity across all quotations
    private BigDecimal totalAmount; // Total amount from quotations containing this product
}

