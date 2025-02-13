package com.inventory.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.inventory.exception.ValidationException;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {
    private static final Color PRIMARY_COLOR = new DeviceRgb(23, 163, 222);
    private static final Color TEXT_PRIMARY = new DeviceRgb(44, 62, 80);
    private static final Color BORDER_COLOR = new DeviceRgb(222, 226, 230);
    
    public byte[] generateQuotationPdf(Map<String, Object> quotationData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream))) {
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);
            
            addHeader(document, quotationData);
            addQuotationDetails(document, quotationData);
            addItemsTable(document, (List<Map<String, Object>>) quotationData.get("items"), quotationData);
            addFooter(document, quotationData);
            
            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
    
    private void addHeader(Document document, Map<String, Object> data) {
        // Company name with border
        Table nameTable = new Table(1).useAllAvailableWidth();
        Cell nameCell = new Cell()
            .add(new Paragraph("CAPTAIN STEEL")
                .setFontSize(36)
                .setBold()
                .setFontColor(new DeviceRgb(0, 0, 0)))  // Black color
            .setBorder(Border.NO_BORDER)
            .setMarginBottom(30); // Add margin below company name
        nameTable.addCell(nameCell);
        document.add(nameTable);

        // Logo and details in separate table
        Table header = new Table(2).useAllAvailableWidth();
        
        // Left side - Details
        Cell detailsCell = new Cell();
        detailsCell.add(new Paragraph("Address :- Survey No.39/2, Plot No.4, Nr.MaekwellbSpining Mill,")
                        .setFontSize(10))
                   .add(new Paragraph("Sadak Pipliya, National Highway, Ta. Gondal, Dist. Rajkot.")
                        .setFontSize(10))
                   .add(new Paragraph("E-mail: captainsteel39@gmail.com")
                        .setFontSize(10))
                   .add(new Paragraph("Mo.No. 96627 12222 / 89803 92009")
                        .setFontSize(10))
                   .add(new Paragraph("GST NO.24AALFC2707P1Z8")
                        .setFontSize(11)
                        .setBold()
                        .setFontColor(PRIMARY_COLOR))
                   .setBorder(Border.NO_BORDER)
                   .setTextAlignment(TextAlignment.LEFT);
        
        // Right side - Logo image
        Cell logoCell = new Cell();
        try {
            ImageData imageData = ImageDataFactory.create("src/main/resources/quotation/Title.jpg");
            Image img = new Image(imageData);
            img.setWidth(200);
            img.setHeight(100);
            logoCell.add(img);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error loading logo image", e);
        }
        logoCell.setBorder(Border.NO_BORDER)
               .setTextAlignment(TextAlignment.RIGHT);
        
        header.addCell(detailsCell);
        header.addCell(logoCell);
        document.add(header);
        
        // Add horizontal line
        Table line = new Table(1).useAllAvailableWidth();
        line.addCell(new Cell()
            .setHeight(1)
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(Border.NO_BORDER));
        document.add(line);
        
        // Add Quotation text
        document.add(new Paragraph("Quotation")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(24)
            .setBold()
            .setFontColor(PRIMARY_COLOR)
            .setMarginTop(10));
    }
    
    private void addQuotationDetails(Document document, Map<String, Object> data) {
        document.add(new Paragraph("\n"));
        
        // Quotation Info Table
        Table infoTable = new Table(2).useAllAvailableWidth();
        
        // Left side - Quote details
        Cell quoteDetails = new Cell();
        quoteDetails.add(new Paragraph("To,").setBold())
                    .add(new Paragraph(data.get("customerName").toString()).setBold())
                    .add(new Paragraph("Quote Number: " + data.get("quoteNumber")))
                   .add(new Paragraph("Quote Date: " + data.get("quoteDate")))
                   .add(new Paragraph("Valid Until: " + data.get("validUntil")))
                   .add(new Paragraph("Mobile No. : " + (data.get("contactNumber") != null ? data.get("contactNumber") : "")))
                   .setBorder(Border.NO_BORDER);
    
        
        infoTable.addCell(quoteDetails);
        
        document.add(infoTable);
        
        // Add remarks if present
        if (data.get("remarks") != null) {
            document.add(new Paragraph("\nRemarks: " + data.get("remarks"))
                .setFontColor(TEXT_PRIMARY)
                .setMarginTop(10));
        }
    }
    
    private void addItemsTable(Document document, List<Map<String, Object>> items, Map<String, Object> quotationData) {
        // Create table with 5 columns instead of 8
        Table table = new Table(new float[]{2, 4, 2, 2, 2})
            .useAllAvailableWidth()
            .setMarginTop(20);
            
        // Add simplified headers
        Stream.of("Sr. No.", "ITEM NAME", "QUANTITY", "PRICE", "TOTAL AMOUNT")
            .forEach(title -> table.addHeaderCell(
                new Cell().add(new Paragraph(title))
                        .setBackgroundColor(PRIMARY_COLOR)
                        .setFontColor(ColorConstants.WHITE)
                        .setPadding(5)
            ));
            
        // Add items
        AtomicInteger counter = new AtomicInteger(1);
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Map<String, Object> item : items) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(counter.getAndIncrement()))));
            table.addCell(new Cell().add(new Paragraph(item.get("productName").toString())));
            table.addCell(new Cell().add(new Paragraph(item.get("quantity").toString())));
            table.addCell(new Cell().add(new Paragraph(item.get("unitPrice").toString())));
            table.addCell(new Cell().add(new Paragraph(item.get("discountPrice").toString())));
            
            totalAmount = totalAmount.add(new BigDecimal(item.get("discountPrice").toString()));
        }
        
        document.add(table);
        
        // Create summary table with 2 columns
        Table summaryTable = new Table(new float[]{3, 1})
            .useAllAvailableWidth()
            .setMarginTop(10);
        
        // Add empty cell on left side
        Cell leftCell = new Cell()
            // .add(new Paragraph("SGST 9% CGST 9%"))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.LEFT)
            .setFontSize(10);
        
        // Create right-side table for totals
        Table totalsTable = new Table(2)
            .useAllAvailableWidth();
        
        // Add total rows with borders
        Cell totalLabelCell = new Cell()
            .add(new Paragraph("TOTAL"))
            .setBorder(Border.NO_BORDER);
        Cell totalValueCell = new Cell()
            .add(new Paragraph(totalAmount.toString() + "/-"))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT);
        totalsTable.addCell(totalLabelCell);
        totalsTable.addCell(totalValueCell);
        
        // Calculate and add GST
        BigDecimal gstAmount = totalAmount.multiply(BigDecimal.valueOf(18))
                .divide(BigDecimal.valueOf(100), 0, BigDecimal.ROUND_HALF_UP);
        Cell gstLabelCell = new Cell()
            .add(new Paragraph("GST 18 % (SGST 9% CGST 9%)"))
            .setBorder(Border.NO_BORDER);
        Cell gstValueCell = new Cell()
            .add(new Paragraph(gstAmount.toString() + "/-"))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT);
        totalsTable.addCell(gstLabelCell);
        totalsTable.addCell(gstValueCell);
        
        // Add grand total
        BigDecimal grandTotal = totalAmount.add(gstAmount);
        Cell grandTotalLabelCell = new Cell()
            .add(new Paragraph("GRAND TOTAL"))
            .setBorder(Border.NO_BORDER)
            .setBold();
        Cell grandTotalValueCell = new Cell()
            .add(new Paragraph(grandTotal.toString() + "/-"))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBold();
        totalsTable.addCell(grandTotalLabelCell);
        totalsTable.addCell(grandTotalValueCell);
        
        // Add the tables to the main summary table
        summaryTable.addCell(leftCell);
        Cell rightCell = new Cell().add(totalsTable).setBorder(Border.NO_BORDER);
        summaryTable.addCell(rightCell);
        
        document.add(summaryTable);

        // Add calculation details tables for each item
        for (Map<String, Object> item : items) {
            if (shouldShowCalculationDetails(item)) {
                addCalculationDetailsTable(document, item);
            }
        }
    }

    private boolean shouldShowCalculationDetails(Map<String, Object> item) {
        String productType = (String) item.get("productType");
        System.out.println("item : " + item);
        return "REGULAR".equals(productType) || "POLY_CARBONATE".equals(productType);
    }
    
    private void addCalculationDetailsTable(Document document, Map<String, Object> item) {
        document.add(new Paragraph("\nCalculation Details for " + item.get("productName"))
            .setBold()
            .setFontColor(PRIMARY_COLOR)
            .setMarginTop(10));
            
        List<Map<String, Object>> calculations = (List<Map<String, Object>>) item.get("calculations");
        if (calculations == null || calculations.isEmpty()) {
            return;
        }

        String calculationType = (String) item.get("calculationType");
        Table table;

        System.out.println("calculationType : " + calculationType);
        
        if ("SQ_FEET".equals(calculationType)) {
            table = createSqFeetCalculationTable(calculations);
        } else if ("MM".equals(calculationType)) {
            table = createMMCalculationTable(calculations);
        } else {
            return;
        }
        
        document.add(table);
    }
    
    private Table createSqFeetCalculationTable(List<Map<String, Object>> calculations) {
        Table table = new Table(new float[]{2, 2, 2, 2, 2, 2})
            .useAllAvailableWidth()
            .setMarginTop(5);
            
        // Add headers
        Stream.of("Feet", "Inch", "Nos", "Running Feet", "Sq.Feet", "Weight")
            .forEach(title -> table.addHeaderCell(
                createHeaderCell(title)
            ));
            
        // Initialize totals
        BigDecimal totalFeet = BigDecimal.ZERO;
        BigDecimal totalInch = BigDecimal.ZERO;
        Long totalNos = 0L;
        BigDecimal totalRunningFeet = BigDecimal.ZERO;
        BigDecimal totalSqFeet = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        // Add data rows
        for (Map<String, Object> calc : calculations) {
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("feet")))));
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("inch")))));
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("nos")))));
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("runningFeet")))));
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("sqFeet")))));
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("weight")))));
            
            // Accumulate totals
            totalFeet = totalFeet.add(toBigDecimal(calc.get("feet")));
            totalInch = totalInch.add(toBigDecimal(calc.get("inch")));
            totalNos += toLong(calc.get("nos"));
            totalRunningFeet = totalRunningFeet.add(toBigDecimal(calc.get("runningFeet")));
            totalSqFeet = totalSqFeet.add(toBigDecimal(calc.get("sqFeet")));
            totalWeight = totalWeight.add(toBigDecimal(calc.get("weight")));
        }
        
        // Add total row
        table.addCell(createTotalCell(totalFeet.toString()));
        table.addCell(createTotalCell(totalInch.toString()));
        table.addCell(createTotalCell(String.valueOf(totalNos)));
        table.addCell(createTotalCell(totalRunningFeet.toString()));
        table.addCell(createTotalCell(totalSqFeet.toString()));
        table.addCell(createTotalCell(totalWeight.toString()));
        
        return table;
    }
    
    private Table createMMCalculationTable(List<Map<String, Object>> calculations) {
        Table table = new Table(new float[]{3, 2, 3, 3})
            .useAllAvailableWidth()
            .setMarginTop(5);
            
        // Add headers
        Stream.of("MM", "Nos", "R.Feet", "Weight")
            .forEach(title -> table.addHeaderCell(
                createHeaderCell(title)
            ));
            
        // Initialize totals
        BigDecimal totalMM = BigDecimal.ZERO;
        Long totalNos = 0L;
        BigDecimal totalRunningFeet = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        // Add data rows
        for (Map<String, Object> calc : calculations) {
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("mm")))));
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("nos")))));
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("runningFeet")))));
            table.addCell(new Cell().add(new Paragraph(formatValue(calc.get("weight")))));
            
            // Accumulate totals
            totalMM = totalMM.add(toBigDecimal(calc.get("mm")));
            totalNos += toLong(calc.get("nos"));
            totalRunningFeet = totalRunningFeet.add(toBigDecimal(calc.get("runningFeet")));
            totalWeight = totalWeight.add(toBigDecimal(calc.get("weight")));
        }
        
        // Add total row
        table.addCell(createTotalCell(totalMM.toString()));
        table.addCell(createTotalCell(String.valueOf(totalNos)));
        table.addCell(createTotalCell(totalRunningFeet.toString()));
        table.addCell(createTotalCell(totalWeight.toString()));
        
        return table;
    }
    
    private Cell createHeaderCell(String title) {
        return new Cell()
            .add(new Paragraph(title))
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(5);
    }
    
    private Cell createTotalCell(String value) {
        return new Cell()
            .add(new Paragraph(value))
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setBold()
            .setPadding(5);
    }
    
    private String formatValue(Object value) {
        return value != null ? value.toString() : "0";
    }
    
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }
    
    private Long toLong(Object value) {
        if (value == null) return 0L;
        return Long.parseLong(value.toString());
    }
    
    private void addItemRow(Table table, Map<String, Object> item, int counter) {
        table.addCell(new Cell().add(new Paragraph(String.valueOf(counter))));
        table.addCell(new Cell().add(new Paragraph(item.get("productName").toString())));
        table.addCell(new Cell().add(new Paragraph(item.get("quantity").toString())));
        table.addCell(new Cell().add(new Paragraph(item.get("unitPrice").toString())));
        table.addCell(new Cell().add(new Paragraph(item.get("discountAmount").toString())));
        table.addCell(new Cell().add(new Paragraph(item.get("taxPercentage").toString())));
        table.addCell(new Cell().add(new Paragraph(item.get("taxAmount").toString())));
        table.addCell(new Cell().add(new Paragraph(item.get("finalPrice").toString())));
    }
    
    private void addFooter(Document document, Map<String, Object> data) {
        document.add(new Paragraph("\n"));
        
        // Terms and Conditions
        document.add(new Paragraph("Terms & Conditions")
            .setFontColor(PRIMARY_COLOR)
            .setBold()
            .setMarginTop(20));
        document.add(new Paragraph(data.get("termsConditions").toString())
            .setFontColor(TEXT_PRIMARY)
            .setMarginTop(10));
            
        // Signatures
        Table signatures = new Table(2).useAllAvailableWidth().setMarginTop(50);
        signatures.addCell(new Cell().add(new Paragraph("For Company")).setBorder(Border.NO_BORDER));
        signatures.addCell(new Cell().add(new Paragraph("For Customer")).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
        document.add(signatures);
    }
} 