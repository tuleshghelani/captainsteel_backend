package com.inventory.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
public class CoatingPriceDto {
    private Long id;
    private BigDecimal coatingUnitPrice;
    private Long clientId;
} 