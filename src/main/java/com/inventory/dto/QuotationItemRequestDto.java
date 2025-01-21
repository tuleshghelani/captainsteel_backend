package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationItemRequestDto {
    private Long productId;
    private Long quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private List<QuotationItemCalculationDto> calculations;
} 