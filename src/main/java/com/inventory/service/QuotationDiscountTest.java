package com.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class QuotationDiscountTest {
    
    public static BigDecimal calculateQuotationDiscount(BigDecimal taxAmount, BigDecimal quotationDiscount) {
        if (quotationDiscount == null || quotationDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Ensure discount doesn't exceed 100%
        if (quotationDiscount.compareTo(BigDecimal.valueOf(100)) > 0) {
            quotationDiscount = BigDecimal.valueOf(100);
        }
        
        return taxAmount.multiply(quotationDiscount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    public static void main(String[] args) {
        // Test case: taxAmount = 500, quotationDiscount = 70%
        BigDecimal taxAmount = new BigDecimal("500");
        BigDecimal quotationDiscount = new BigDecimal("70");
        
        BigDecimal discountAmount = calculateQuotationDiscount(taxAmount, quotationDiscount);
        BigDecimal finalTaxAmount = taxAmount.subtract(discountAmount);
        
        System.out.println("Original tax amount: " + taxAmount);
        System.out.println("Quotation discount: " + quotationDiscount + "%");
        System.out.println("Discount amount: " + discountAmount);
        System.out.println("Final tax amount: " + finalTaxAmount);
        
        // Expected output:
        // Original tax amount: 500
        // Quotation discount: 70%
        // Discount amount: 350.00
        // Final tax amount: 150.00
    }
}