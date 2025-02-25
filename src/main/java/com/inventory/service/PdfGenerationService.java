package com.inventory.service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.properties.AreaBreakType;
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
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Text;

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
            
            // Add content
            addHeader(document, quotationData);
            addPageFooter(pdf, document, 1);
            
            addQuotationDetails(document, quotationData);
            addItemsTable(document, (List<Map<String, Object>>) quotationData.get("items"), quotationData);
            addPageFooter(pdf, document, 2);
            
            addBankDetailsAndTerms(document);
            addPageFooter(pdf, document, 3);
            
            addLastPage(document);
            addPageFooter(pdf, document, 4);
            
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
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("quotation/Title.jpg");
            if (imageStream == null) {
                throw new FileNotFoundException("Image not found: quotation/Title.jpg");
            }
            ImageData imageData = ImageDataFactory.create(imageStream.readAllBytes());
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
            table.addCell(new Cell().add(convertHtmlToParagraph(item.get("productName").toString())));
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
        Cell totalLoading = new Cell()
            .add(new Paragraph("Loading Charge"))
            .setBorder(Border.NO_BORDER);
        Cell totalLoadingValueCell = new Cell()
            .add(new Paragraph(quotationData.get("loadingCharge").toString()))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT);
        totalsTable.addCell(totalLoading);
        totalsTable.addCell(totalLoadingValueCell);

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
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
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
        BigDecimal grandTotal = ((BigDecimal) quotationData.get("totalAmount")).setScale(0, RoundingMode.HALF_UP);;
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
            .setFontColor(TEXT_PRIMARY)
            .setBold()
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
        Table table = new Table(new float[]{2, 2, 2})
            .useAllAvailableWidth()
            .setMarginTop(5);
        
        // Add headers with specific colors
        Cell feetHeader = new Cell()
            .add(new Paragraph("Feet"))
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(5);

        Cell inchHeader = new Cell()
            .add(new Paragraph("Inch"))
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(5);
        
        Cell nosHeader = new Cell()
            .add(new Paragraph("Nos"))
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(5);
        
        table.addHeaderCell(feetHeader);
        table.addHeaderCell(inchHeader);
        table.addHeaderCell(nosHeader);
        
        // Add data rows with matching background colors
        for (Map<String, Object> calc : calculations) {
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("feet"))))
                .setBackgroundColor(new DeviceRgb(230, 185, 184)));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("inch"))))
                .setBackgroundColor(new DeviceRgb(141, 180, 227)));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("nos"))))
                .setBackgroundColor(new DeviceRgb(252, 213, 180)));
        }
        
        return table;
    }
    
    private Table createMMCalculationTable(List<Map<String, Object>> calculations) {
        Table table = new Table(new float[]{2, 2, 2})
            .useAllAvailableWidth()
            .setMarginTop(5);
        
        // Add headers with specific colors
        Cell mmHeader = new Cell()
            .add(new Paragraph("MM"))
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(5);
        
        Cell rFeetHeader = new Cell()
            .add(new Paragraph("R.Feet"))
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(5);
        
        Cell nosHeader = new Cell()
            .add(new Paragraph("Nos"))
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(5);
        
        table.addHeaderCell(mmHeader);
        table.addHeaderCell(rFeetHeader);
        table.addHeaderCell(nosHeader);
        
        // Add data rows with matching background colors
        for (Map<String, Object> calc : calculations) {
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("mm"))))
                .setBackgroundColor(new DeviceRgb(230, 185, 184)));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("runningFeet"))))
                .setBackgroundColor(new DeviceRgb(141, 180, 227)));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("nos"))))
                .setBackgroundColor(new DeviceRgb(252, 213, 180)));
        }
        
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
    
    private void addBankDetailsAndTerms(Document document) {
        // Start new page
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        
        // GST Number
        document.add(new Paragraph("GST No: 24AALFC2707P1Z8")
            .setFontColor(TEXT_PRIMARY)
            .setFontSize(12)
            .setMarginBottom(20));
        
        // Bank Details Section
        document.add(new Paragraph("BANK DETAILS:")
            .setFontColor(new DeviceRgb(41, 84, 153))  // Blue color
            .setBold()
            .setFontSize(14)
            .setMarginBottom(10));
        
        document.add(new Paragraph("CENTRAL BANK OF INDIA")
            .setFontColor(new DeviceRgb(230, 108, 1))  // Orange color
            .setBold()
            .setFontSize(12));
            
        Table bankTable = new Table(2).useAllAvailableWidth();
        addBankDetail(bankTable, "A/C NO:", "3592903798");
        addBankDetail(bankTable, "IFSC CODE:", "CBIN0280569");
        addBankDetail(bankTable, "BRANCH:", "BHUPENDRAROAD,RAJKOT");
        document.add(bankTable);
        
        // Terms and Conditions Section
        document.add(new Paragraph("\nTERMS AND CONDITIONS:")
            .setFontColor(new DeviceRgb(207, 89, 86))  // Red color
            .setBold()
            .setFontSize(14)
            .setMarginTop(20)
            .setMarginBottom(10));
        
        // Add terms
        addTerm(document, "1.", "Customer will be billed after indicating acceptance of this quote.", new DeviceRgb(66, 133, 244));
        addTerm(document, "2.", "Payment 50% Advance And 50% before goods Dispatched.", new DeviceRgb(66, 133, 244));
        addTerm(document, "3.", "Transport Transaction Extra", new DeviceRgb(66, 133, 244));
        addTerm(document, "4.", "The Responsibility Of All the Material Will Be With That Company.\nThere Will Be No Responsibility Of The Distributor I.E. Captain Steel.", new DeviceRgb(66, 133, 244));
        addTerm(document, "5.", "SUBJECT TO GONDAL JURISDICTION.", new DeviceRgb(66, 133, 244));
        addTerm(document, "6.", "THIS QUOTATION IS VALID FOR TWO DAYS.", new DeviceRgb(66, 133, 244));
    }

    private void addBankDetail(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label))
            .setBold()
            .setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value))
            .setBorder(Border.NO_BORDER));
    }

    private void addTerm(Document document, String number, String text, Color color) {
        document.add(new Paragraph(number + " " + text)
            .setFontColor(color)
            .setBold()
            .setMarginBottom(5));
    }
    
    private void addLastPage(Document document) {
        // Start new page
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        
        try {
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("quotation/Quotation_last_page.jpg");
            if (imageStream == null) {
                throw new FileNotFoundException("Image not found: quotation/Quotation_last_page.jpg");
            }
            ImageData imageData = ImageDataFactory.create(imageStream.readAllBytes());
            Image img = new Image(imageData);
            
            // Get page dimensions
            float pageWidth = document.getPdfDocument().getDefaultPageSize().getWidth();
            float pageHeight = document.getPdfDocument().getDefaultPageSize().getHeight();
            
            // Set image to fill the entire page
            img.setFixedPosition(0, 0);  // Start from top-left corner
            img.scaleToFit(pageWidth, pageHeight);
            img.setMargins(0, 0, 0, 0);  // Remove all margins
            
            document.add(img);
        } catch (Exception e) {
            log.error("Error loading last page image", e);
            e.printStackTrace();
        }
    }

    private void addPageFooter(PdfDocument pdfDoc, Document document, int pageNumber) {
        float footerY = 20;  // Distance from bottom
        float pageWidth = pdfDoc.getDefaultPageSize().getWidth();
        
        // Create HR line table
        Table lineTable = new Table(1)
            .useAllAvailableWidth()
            .setFixedPosition(36, footerY + 15, pageWidth - 72);  // Position above footer text
        
        lineTable.addCell(new Cell()
            .setHeight(0.5f)
            .setBackgroundColor(TEXT_PRIMARY)
            .setBorder(Border.NO_BORDER));
        
        // Create footer table with single column for centered content
        Table footerTable = new Table(1)
            .useAllAvailableWidth()
            .setFixedPosition(36, footerY, pageWidth - 72);
        
        // Contact information cell (center-aligned)
        Cell contactCell = new Cell()
            .add(new Paragraph("CAPTAIN STEEL [ CONTECT NO.9879109091 / 8980392009 / 7574879091 / 9879109121 ]")
                .setFontSize(8)
                .setFontColor(TEXT_PRIMARY))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.CENTER);
        
        footerTable.addCell(contactCell);
        
        // Add both tables to document
        document.add(lineTable);
        document.add(footerTable);
    }

    // Helper method to convert HTML to formatted paragraph
    private Paragraph convertHtmlToParagraph(String html) {
        Paragraph paragraph = new Paragraph();
        
        // Remove any null or empty strings
        if (html == null || html.trim().isEmpty()) {
            return paragraph;
        }

        // First handle HTML tags
        String[] parts = html.split("(<b>|</b>)");
        boolean isBold = false;

        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                // Handle spacing between words
                String formattedPart = part;
//                part.replaceAll("(?<=\\w)(?=[A-Z])", " ")  // Add space between camelCase
//                        .replaceAll("_", " ")                      // Replace underscores with spaces
//                        .replaceAll("\\s+", " ")                   // Normalize multiple spaces
//                        .trim(); // Remove leading/trailing spaces
                
                Text text = new Text(formattedPart);
                if (isBold) {
                    text.setBold();
                }
                paragraph.add(text);
                
                // Add a space between parts if not the last part
//                if (isBold && !isLastPart(parts, part)) {
//                    paragraph.add(new Text(" "));
//                }
                
                isBold = !isBold;
            }
        }
        
        return paragraph;
    }

    private boolean isLastPart(String[] parts, String currentPart) {
        return currentPart.equals(parts[parts.length - 1]);
    }
} 