package com.inventory.service;

import com.inventory.exception.ValidationException;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchSlipPdfService {
    private static final Color PRIMARY_COLOR = new DeviceRgb(23, 163, 222);
    private static final Color TEXT_PRIMARY = new DeviceRgb(44, 62, 80);
    private static final Color BORDER_COLOR = new DeviceRgb(222, 226, 230);

    private static final BigDecimal SQ_FEET_TO_METER = BigDecimal.valueOf(10.764);
    private static final BigDecimal MM_TO_METER = BigDecimal.valueOf(1000);

    public byte[] generateDispatchSlipPdf(Map<String, Object> dispatchData) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(baos))) {
            
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);
            
            // Add content
            addHeader(document, dispatchData);
            addPageFooter(pdf, document, 1);
            
            addDispatchDetails(document, dispatchData);
            addItemsTable(document, (List<Map<String, Object>>) dispatchData.get("items"), dispatchData);
            addPageFooter(pdf, document, 2);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating dispatch slip PDF", e);
            throw new ValidationException("Failed to generate dispatch slip PDF: " + e.getMessage());
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
            ImageData imageData = ImageDataFactory.create("quotation/Title.jpg");
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

        // Add Dispatch text
        document.add(new Paragraph("Dispatch Slip")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(24)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(10));
    }

    private void addDispatchDetails(Document document, Map<String, Object> data) {
        document.add(new Paragraph("\n"));

        // Dispatch Info Table
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
            document.add(new Paragraph("\nRemarks: ")
                    .setFontColor(TEXT_PRIMARY)
                    .setMarginTop(10));
        }
    }

    private void addItemsTable(Document document, List<Map<String, Object>> items, Map<String, Object> dispatchData) {
        Table table = new Table(new float[]{2, 4, 2})
                .useAllAvailableWidth()
                .setMarginTop(20);

        // Add simplified headers
        Stream.of("Sr. No.", "ITEM NAME", "QUANTITY")
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
            
            // Create item name cell with product name and optional remarks
            Paragraph itemNameParagraph = convertHtmlToParagraph(item, true);

            // Add nos in brackets if available and not null (after item name)
            Object nos = item.get("nos");
            if (nos != null && !nos.toString().trim().isEmpty()) {
                itemNameParagraph.add(new Text(" (" + nos.toString() + " nos)")
                        .setFontSize(10)
                        .setFontColor(new DeviceRgb(60, 60, 60)));
            }

            // Add item remarks if available
            String itemRemarks = item.get("itemRemarks") != null ? item.get("itemRemarks").toString().trim() : "";
            if (!itemRemarks.isEmpty()) {
                itemNameParagraph.add(new Text("\n(" + itemRemarks + ")")
                        .setFontSize(9)
                        .setItalic()
                        .setFontColor(new DeviceRgb(100, 100, 100)));
            }

            table.addCell(new Cell().add(itemNameParagraph));

            // Check if calculationBase is N (NOS) or calculationType is NOS
            String calculationBase = item.get("calculationBase") != null ? item.get("calculationBase").toString().trim() : "";
            String calculationType = item.get("calculationType") != null ? item.get("calculationType").toString().trim() : "";
            String displayMeasurement;

            if ("N".equals(calculationBase) || "NOS".equals(calculationType)) {
                // For NOS calculation base/type, show "NOS" instead of measurement
                displayMeasurement = "\nNOS";
            } else {
                // For other calculation types, show the measurement unit
                String measurement = item.get("measurement") != null ? item.get("measurement").toString().trim() : "";
                // Add "approx." prefix for kg measurements
                displayMeasurement = measurement.toLowerCase().equals("kg") ? "\n " + measurement + "(approx.)" : "\n" + measurement;
            }

            table.addCell(new Cell().add(new Paragraph(item.get("quantity").toString() + " " + displayMeasurement)));
        }

        document.add(table);

        // Add calculation details tables for each item
        for (Map<String, Object> item : items) {
                if (shouldShowCalculationDetails(item)) {
                        addCalculationDetailsTable(document, item);
                }
        }
    }

    private boolean shouldShowCalculationDetails(Map<String, Object> item) {
        String productType = (String) item.get("productType");
        String calculationBase = (String) item.get("calculationBase");
        String calculationType = (String) item.get("calculationType");
        System.out.println("item : " + item);
        // Don't show calculation details if calculationBase is N (NOS) or calculationType is NOS
        if ("N".equals(calculationBase) || "NOS".equals(calculationType)) {
            return false;
        }
        return "REGULAR".equals(productType) || "POLY_CARBONATE".equals(productType) || "POLY_CARBONATE_ROLL".equals(productType);
    }

    private void addCalculationDetailsTable(Document document, Map<String, Object> item) {
        String productNameHtml = item.get("productName").toString();
        Paragraph productNameParagraph = convertHtmlToParagraph(item, false);

        document.add(new Paragraph("\nCalculation Details for : ")
                .add(productNameParagraph)
                .setFontColor(TEXT_PRIMARY)
                .setMarginTop(10));

        List<Map<String, Object>> calculations = (List<Map<String, Object>>) item.get("calculations");
        if (calculations == null || calculations.isEmpty()) {
            return;
        }

        String productType = (String) item.get("productType");
        String calculationType = (String) item.get("calculationType");
        Table table;

        System.out.println("calculationType : " + calculationType);
        System.out.println("productType : " + productType);

        if ("POLY_CARBONATE_ROLL".equals(productType)) {
            table = createPolyCarbonateRollCalculationTable(calculations);
        } else if ("SQ_FEET".equals(calculationType)) {
            table = createSqFeetCalculationTable(calculations);
        } else if ("MM".equals(calculationType)) {
            table = createMMCalculationTable(calculations);
        } else {
            return;
        }

        document.add(table);
    }
    
    private Table createSqFeetCalculationTable(List<Map<String, Object>> calculations) {
        Table table = new Table(new float[]{2, 2, 2, 2, 2})
            .useAllAvailableWidth()
            .setMarginTop(5);
        
        // Add headers with specific colors
        Stream.of("Feet", "Inch", "Nos", "Meter", "Sq.Feet")
            .forEach(title -> {
                Cell header = new Cell()
                    .add(new Paragraph(title))
                    .setBackgroundColor(PRIMARY_COLOR)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(5);
                table.addHeaderCell(header);
            });
        
        // Add data rows with matching background colors
        for (Map<String, Object> calc : calculations) {
            BigDecimal sqFeet = toBigDecimal(calc.get("sqFeet"));
            BigDecimal meter = sqFeet.divide(SQ_FEET_TO_METER, 4, RoundingMode.HALF_UP);
            
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("feet"))).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(230, 185, 184))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("inch"))).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(141, 180, 227))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("nos"))).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(252, 213, 180))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(meter)).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(169, 208, 142))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(sqFeet)).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(187, 173, 219))
                .setTextAlignment(TextAlignment.CENTER)); 
        }
        
        return table;
    }
    
    private Table createMMCalculationTable(List<Map<String, Object>> calculations) {
        Table table = new Table(new float[]{2, 2, 2, 2, 2})
            .useAllAvailableWidth()
            .setMarginTop(5);
        
        // Add headers with specific colors
        Stream.of("MM", "R.Feet", "Nos", "Meter", "Sq.Feet")
            .forEach(title -> {
                Cell header = new Cell()
                    .add(new Paragraph(title))
                    .setBackgroundColor(PRIMARY_COLOR)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(5);
                table.addHeaderCell(header);
            });
        
        // Add data rows with matching background colors
        for (Map<String, Object> calc : calculations) {
            BigDecimal mm = toBigDecimal(calc.get("mm"));
            BigDecimal meter = mm.divide(MM_TO_METER, 4, RoundingMode.HALF_UP);
            BigDecimal sqFeet = toBigDecimal(calc.get("sqFeet"));
            
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("mm"))).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(230, 185, 184))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("runningFeet"))).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(141, 180, 227))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("nos"))).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(252, 213, 180))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(meter)).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(169, 208, 142))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(sqFeet)).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(187, 173, 219))
                .setTextAlignment(TextAlignment.CENTER));  
        }
        
        return table;
    }
    
    private Table createPolyCarbonateRollCalculationTable(List<Map<String, Object>> calculations) {
        Table table = new Table(new float[]{3, 3, 3})
            .useAllAvailableWidth()
            .setMarginTop(5);
        
        // Add headers with specific colors
        Stream.of("Length", "Width", "Total (sq. feet)")
            .forEach(title -> {
                Cell header = new Cell()
                    .add(new Paragraph(title))
                    .setBackgroundColor(PRIMARY_COLOR)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(5);
                table.addHeaderCell(header);
            });
        
        // Add data rows with matching background colors
        for (Map<String, Object> calc : calculations) {
            BigDecimal sqFeet = toBigDecimal(calc.get("sqFeet"));
            
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("length"))).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(230, 185, 184))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(calc.get("width"))).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(141, 180, 227))
                .setTextAlignment(TextAlignment.CENTER));
                
            table.addCell(new Cell()
                .add(new Paragraph(formatValue(sqFeet)).setFontSize(9))
                .setBackgroundColor(new DeviceRgb(187, 173, 219))
                .setTextAlignment(TextAlignment.CENTER));  
        }
        
        return table;
    }


    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private String formatValue(Object value) {
        return value != null ? value.toString() : "0";
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
    private Paragraph convertHtmlToParagraph(Map<String, Object> item, boolean isPrintImage) {
        Paragraph paragraph = new Paragraph();
        String html = item.get("productName").toString();

        // Remove any null or empty strings
        if (html == null || html.trim().isEmpty()) {
            return paragraph;
        }

        // Handle non-ACCESSORIES products (existing logic)
        String[] parts = html.split("(<b>|</b>)");
        boolean isBold = false;

        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                // Handle spacing between words
                String formattedPart = part;

                Text text = new Text(formattedPart);
                if (isBold) {
                    text.setBold();
                }
                paragraph.add(text);
                isBold = !isBold;
            }
        }

        // Handle ACCESSORIES product type
        String productType = (String) item.get("productType");
        if ("ACCESSORIES".equals(productType)) {
            // Add product name
//            paragraph.add(new Text(html));

            // Add accessories size information
            String accessoriesSize = (String) item.get("accessoriesSize");
            System.out.println("accessoriesSize inside convertHtmlToParagraph"+ accessoriesSize);
            if (accessoriesSize != null) {
                if ("C".equalsIgnoreCase(accessoriesSize)) {
                    // For Custom size, show "Custom" and weight
                    paragraph.add(new Text(" (Custom"));

                    // Add weight if available
                    Object weightObj = item.get("weight");
                    if (weightObj != null) {
                        paragraph.add(new Text(", " + weightObj.toString() + " "));
                    }
                    paragraph.add(new Text(")"));
                } else {
                    // For standard sizes, show size in inches
                    paragraph.add(new Text(" (" + accessoriesSize + "\")"));
                }
            }

            return paragraph;
        }

        if(isPrintImage) {
            // Add polycarbonate type image if applicable
            String polyCarbonateType = (String) item.get("polyCarbonateType");
            if (polyCarbonateType != null) {
                String imagePath = getPolyCarbonateImagePath(polyCarbonateType);
                if (imagePath != null) {
                    try {
                        paragraph.add(new Text("\n"));
                        InputStream imageStream = getClass().getClassLoader().getResourceAsStream(imagePath);
                        if (imageStream != null) {
                            ImageData imageData = ImageDataFactory.create(imageStream.readAllBytes());
                            Image img = new Image(imageData);
                            img.setWidth(100);
                            img.setHeight(20);
                            paragraph.add(img);
                            imageStream.close(); // Close the stream
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading polycarbonate image: " + imagePath + ", error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }

        return paragraph;
    }

    private String getPolyCarbonateImagePath(String polyCarbonateType) {
        return switch (polyCarbonateType.toUpperCase()) {
            case "SINGLE" -> "quotation/single.jpg";
            case "DOUBLE" -> "quotation/double.jpg";
            case "FULL_SHEET" -> "quotation/full_sheet.jpg";
            default -> null;
        };
    }
} 