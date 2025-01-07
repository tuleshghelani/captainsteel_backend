package com.inventory.service;

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
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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
            addItemsTable(document, (List<Map<String, Object>>) quotationData.get("items"));
            addFooter(document, quotationData);
            
            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
    
    private void addHeader(Document document, Map<String, Object> data) {
        Table header = new Table(2).useAllAvailableWidth();
        
        // Company Logo and Details
        Cell logoCell = new Cell();
        logoCell.add(new Paragraph("Company Logo"))
               .setFontColor(PRIMARY_COLOR)
               .setFontSize(24)
               .setBorder(Border.NO_BORDER);
        
        Cell companyDetails = new Cell();
        companyDetails.add(new Paragraph("Captain Steel"))
                     .add(new Paragraph("Pipaliya, Rajkot"))
                     .add(new Paragraph("Phone: 123456789"))
                     .setBorder(Border.NO_BORDER)
                     .setTextAlignment(TextAlignment.RIGHT);
        
        header.addCell(logoCell);
        header.addCell(companyDetails);
        document.add(header);
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
    
    private void addItemsTable(Document document, List<Map<String, Object>> items) {
        Table table = new Table(new float[]{2, 4, 2, 2, 2, 2, 2, 2})
            .useAllAvailableWidth()
            .setMarginTop(20);
            
        // Add headers
        Stream.of("Sr.", "Product", "Qty", "Price", "Discount", "Tax %", "Tax", "Total")
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
            addItemRow(table, item, counter.getAndIncrement());
            totalAmount = totalAmount.add(new BigDecimal(item.get("finalPrice").toString()));
        }
        
        // Add total row
        table.addCell(new Cell(1, 6).add(new Paragraph("Total Amount"))
            .setTextAlignment(TextAlignment.RIGHT)
            .setBold()
            .setPadding(5));
        table.addCell(new Cell().add(new Paragraph(""))); // Empty cell for tax column
        table.addCell(new Cell().add(new Paragraph(totalAmount.toString()))
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setBold()
            .setPadding(5));
        
        document.add(table);
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