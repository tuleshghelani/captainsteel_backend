package com.inventory.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationItemCalculationDto {
    private Long id;
    private Long quotationItemId;
    private BigDecimal feet;
    private BigDecimal mm;
    private BigDecimal inch;
    private Long nos;
    private BigDecimal runningFeet;
    private BigDecimal sqFeet;
    private BigDecimal weight;
    private BigDecimal meter;
    private Long clientId;
} 