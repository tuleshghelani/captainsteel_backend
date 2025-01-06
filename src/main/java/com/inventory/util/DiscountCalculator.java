package com.inventory.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DiscountCalculator {
    private static final int DECIMAL_PLACES = 2;
    
    public static BigDecimal calculateDiscountAmount(BigDecimal baseAmount, BigDecimal discountPercentage) {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return baseAmount.multiply(discountPercentage.divide(BigDecimal.valueOf(100), DECIMAL_PLACES, RoundingMode.HALF_UP))
            .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
    }
    
    public static BigDecimal calculateDiscountedPrice(BigDecimal baseAmount, BigDecimal discountAmount) {
        return baseAmount.subtract(discountAmount).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
    }
    
    public static BigDecimal calculateTotalAmount(BigDecimal discountedPrice, BigDecimal otherExpenses) {
        if (otherExpenses == null || otherExpenses.compareTo(BigDecimal.ZERO) <= 0) {
            return discountedPrice;
        }
        return discountedPrice.add(otherExpenses).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
    }
} 