package com.inventory.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PurchaseItemDto {
    private Long productId;
    private Long quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private Long remainingQuantity;
    private Long clientId;

} 