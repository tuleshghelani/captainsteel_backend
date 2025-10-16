//package com.inventory.service;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.math.BigDecimal;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class PdfGenerationServiceTest {
//
//    @Test
//    void testGenerateQuotationPdf() {
//        PdfGenerationService pdfGenerationService = new PdfGenerationService();
//
//        // Create test data
//        Map<String, Object> quotationData = new HashMap<>();
//        quotationData.put("customerName", "Test Customer");
//        quotationData.put("quoteNumber", "QT001");
//        quotationData.put("quoteDate", "2023-01-01");
//        quotationData.put("validUntil", "2023-01-31");
//        quotationData.put("contactNumber", "1234567890");
//        quotationData.put("loadingCharge", "500");
//        quotationData.put("totalAmount", new BigDecimal("11800"));
//
//        List<Map<String, Object>> items = new ArrayList<>();
//        Map<String, Object> item1 = new HashMap<>();
//        item1.put("productName", "Test Product 1");
//        item1.put("quantity", "2");
//        item1.put("measurement", "kg");
//        item1.put("unitPrice", "5000");
//        item1.put("discountPrice", "9000");
//        item1.put("productType", "REGULAR");
//        item1.put("calculationType", "SQ_FEET");
//
//        List<Map<String, Object>> calculations = new ArrayList<>();
//        Map<String, Object> calc1 = new HashMap<>();
//        calc1.put("feet", "10");
//        calc1.put("inch", "5");
//        calc1.put("nos", "2");
//        calc1.put("sqFeet", "25.5");
//        calculations.add(calc1);
//
//        item1.put("calculations", calculations);
//        items.add(item1);
//
//        quotationData.put("items", items);
//
//        // Generate PDF
//        byte[] pdfBytes = pdfGenerationService.generateQuotationPdf(quotationData);
//
//        // Verify PDF was generated
//        assertNotNull(pdfBytes);
//        assertTrue(pdfBytes.length > 0);
//    }
//}