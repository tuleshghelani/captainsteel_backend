package com.inventory.service;

import com.inventory.dto.ProductDto;
import com.inventory.exception.ValidationException;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductPdfService {
    private static final Color PRIMARY_COLOR = new DeviceRgb(23, 163, 222);
    private static final Color NEGATIVE_COLOR = new DeviceRgb(255, 192, 192);
    
    public byte[] generateProductListPdf(List<Map<String, Object>> products) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(baos))) {
            
            Document document = new Document(pdf, PageSize.A4.rotate());
            document.setMargins(20, 20, 20, 20);
            
            addHeader(document);
            addProductTable(document, products);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage());
        }
    }
    
    private void addHeader(Document document) {
        document.add(new Paragraph("Product List")
            .setFontSize(16)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));
    }
    
    private void addProductTable(Document document, List<Map<String, Object>> products) {
        Table table = new Table(new float[]{3, 2, 2, 1, 1, 2, 2, 2, 2, 2})
            .useAllAvailableWidth()
            .setMarginTop(10);
            
        // Add headers
        addTableHeader(table);
        
        // Add data rows
        for (Map<String, Object> product : products) {
            addTableRow(table, product);
        }
        
        document.add(table);
    }
    
    private void addTableHeader(Table table) {
        String[] headers = {
            "Name", "Category", "Description", "Min Stock", "Status",
            "Remaining Quantity", "Blocked Amount", "Total Remaining Amount",
            "Purchase Amount", "Sale Amount"
        };
        
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                .add(new Paragraph(header))
                .setBackgroundColor(PRIMARY_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5));
        }
    }
    
    private void addTableRow(Table table, Map<String, Object> product) {
        addCell(table, toString(product.get("name")));
        addCell(table, toString(product.get("categoryName")));
        addCell(table, toString(product.get("description")));
        addCell(table, toString(product.get("minimumStock")));
        addCell(table, toString(product.get("status")));
        
        // Add quantity cells with conditional formatting
        addQuantityCell(table, product.get("remainingQuantity"));
        addQuantityCell(table, product.get("blockedQuantity"));
        addQuantityCell(table, product.get("totalRemainingQuantity"));
        
        addCell(table, toString(product.get("purchaseAmount")));
        addCell(table, toString(product.get("saleAmount")));
    }
    
    private void addCell(Table table, String text) {
        table.addCell(new Cell()
            .add(convertHtmlToParagraph(text))
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5));
    }
    
    private void addQuantityCell(Table table, Object value) {
        Cell cell = new Cell()
            .add(new Paragraph(toString(value)))
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5);
            
        if (isNegativeValue(value)) {
            cell.setBackgroundColor(NEGATIVE_COLOR);
        }
        
        table.addCell(cell);
    }
    
    private boolean isNegativeValue(Object value) {
        if (value == null) return false;
        try {
            return new BigDecimal(value.toString()).compareTo(BigDecimal.ZERO) < 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private String toString(Object value) {
        return value != null ? value.toString() : "";
    }

    private Paragraph convertHtmlToParagraph(String html) {
        if (html == null || html.trim().isEmpty()) {
            return new Paragraph("");
        }

        Paragraph paragraph = new Paragraph();
        StringBuilder currentText = new StringBuilder();
        boolean isBold = false;

        int i = 0;
        while (i < html.length()) {
            if (html.startsWith("<b>", i)) {
                // Add any accumulated normal text
                if (currentText.length() > 0) {
                    paragraph.add(new Text(currentText.toString()));
                    currentText.setLength(0);
                }
                isBold = true;
                i += 3; // Skip "<b>"
            } else if (html.startsWith("</b>", i)) {
                // Add accumulated bold text
                if (currentText.length() > 0) {
                    paragraph.add(new Text(currentText.toString()).setBold());
                    currentText.setLength(0);
                }
                isBold = false;
                i += 4; // Skip "</b>"
            } else {
                currentText.append(html.charAt(i));
                i++;
            }
        }

        // Add any remaining text
        if (currentText.length() > 0) {
            if (isBold) {
                paragraph.add(new Text(currentText.toString()).setBold());
            } else {
                paragraph.add(new Text(currentText.toString()));
            }
        }

        return paragraph;
    }
} 