package com.inventory.service;

import com.inventory.exception.ValidationException;
import com.inventory.model.PdfGenerationStatus;
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
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.time.ZoneId;
@Slf4j
@Service
//@RequiredArgsConstructor
public class AttendancePdfService {
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(20, 88, 129);    // #145881
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(23, 163, 223); // #17a3df
    private static final DeviceRgb BACKGROUND = new DeviceRgb(245, 249, 252);     // #f5f9fc
    private static final DeviceRgb ACCENT1 = new DeviceRgb(79, 195, 247);        // #4fc3f7
    private static final DeviceRgb ACCENT2 = new DeviceRgb(29, 182, 246);        // #29b6f6
    private static final DeviceRgb TEXT_PRIMARY = new DeviceRgb(44, 62, 80);     // #2c3e50
    private static final DeviceRgb TEXT_SECONDARY = new DeviceRgb(96, 125, 139);  // #607d8b
    private static final DeviceRgb SUPPORTING = new DeviceRgb(144, 164, 174);    // #90a4ae
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(IST);
    
    // Cache for managing concurrent PDF generations with request tracking
    private final ConcurrentHashMap<String, PdfGenerationStatus> processingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public AttendancePdfService() {
        // Schedule cleanup of stale requests every 15 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleRequests, 15, 15, TimeUnit.MINUTES);
    }

    public byte[] generatePdf(Map<String, Object> employeeData, List<Map<String, Object>> attendanceRecords,
    LocalDate startDate, LocalDate endDate) {
        String requestId = generateRequestId(employeeData);

        PdfGenerationStatus status = processingRequests.get(requestId);
        if (status != null && !status.isStale()) {
            throw new ValidationException("PDF generation already in progress for this request");
        }

        processingRequests.put(requestId, new PdfGenerationStatus());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(baos))) {
            
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);
            
            addHeader(document, startDate, endDate);
            addEmployeeDetails(document, employeeData);
            addAttendanceTable(document, attendanceRecords);
            addSummary(document, calculateSummary(attendanceRecords));
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage());
        } finally {
            processingRequests.remove(requestId);
        }
    }
    
    private void addHeader(Document document, LocalDate startDate, LocalDate endDate) {
        Table header = new Table(2).useAllAvailableWidth();
        
        Cell titleCell = new Cell()
            .add(new Paragraph("Salary Report")
                .setFontSize(28)
                .setFontColor(PRIMARY_COLOR)
                .setBold())
            .add(new Paragraph("Monthly Attendance & Salary Details")
                .setFontSize(14)
                .setFontColor(TEXT_SECONDARY))
            .setBorder(Border.NO_BORDER);
        
        Cell dateCell = new Cell()
            .add(new Paragraph("Period: " + DATE_FORMATTER.format(startDate) + 
                 " to " + DATE_FORMATTER.format(endDate))
                .setFontColor(SECONDARY_COLOR)
                .setFontSize(12))
            .add(new Paragraph("Generated on: " + DATE_FORMATTER.format(LocalDate.now()))
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(10))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT);
        
        header.addCell(titleCell);
        header.addCell(dateCell);
        
        // Add decorative line
        Table line = new Table(1).useAllAvailableWidth().setMarginTop(10);
        line.addCell(new Cell()
            .setHeight(2f)
            .setBackgroundColor(ACCENT1)
            .setBorder(Border.NO_BORDER));
        
        document.add(header);
        document.add(line);
    }
    
    private void addEmployeeDetails(Document document, Map<String, Object> employeeData) {
        Table details = new Table(2).useAllAvailableWidth().setMarginTop(20);

        if (employeeData.get("name") != null) {
            addDetailRow(details, "Name", employeeData.get("name").toString());
        }

        Object regularHours = employeeData.get("regularHours");
        if (regularHours != null) {
            addDetailRow(details, "Regular Hours", formatNumber(new BigDecimal(regularHours.toString())));
        }

        Object regularPay = employeeData.get("regularPay");
        if (regularPay != null) {
            addDetailRow(details, "Regular Pay", formatCurrency(new BigDecimal(regularPay.toString())));
        }

        Object overtimePay = employeeData.get("overtimePay");
        if (overtimePay != null) {
            addDetailRow(details, "Overtime Pay", formatCurrency(new BigDecimal(overtimePay.toString())));
        }

        if (employeeData.get("mobileNumber") != null) {
            addDetailRow(details, "Mobile", employeeData.get("mobileNumber").toString());
        }
        
        document.add(details);
    }
    
    private void addAttendanceTable(Document document, List<Map<String, Object>> records) {
        Table table = new Table(new float[]{1, 4, 2, 2, 1.2f, 1.2f, 2})
            .useAllAvailableWidth()
            .setMarginTop(20);

        addTableHeader(table, new String[]{
            "No", "Date", "Start Time", "End Time", 
            "Regular Hours", "Overtime Hours", "Total Pay"
        });

        int rowNum = 1;
        for (Map<String, Object> record : records) {
            addTableRow(table, rowNum++, record);
        }
        
        document.add(table);
    }
    
    private void cleanupStaleRequests() {
        processingRequests.entrySet().removeIf(entry -> entry.getValue().isStale());
    }
    
    private String generateRequestId(Map<String, Object> employeeData) {
        return employeeData.get("id") + "_" + System.currentTimeMillis();
    }
    
    private void addDetailRow(Table table, String label, String value) {
        Cell labelCell = new Cell()
            .add(new Paragraph(label))
            .setFontColor(TEXT_PRIMARY)
            .setBold()
            .setBorder(Border.NO_BORDER);
        
        Cell valueCell = new Cell()
            .add(new Paragraph(value))
            .setFontColor(TEXT_PRIMARY)
            .setBorder(Border.NO_BORDER);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private void addTableHeader(Table table, String[] headers) {
        for (String header : headers) {
            Cell cell = new Cell()
                .add(new Paragraph(header))
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(PRIMARY_COLOR)
                .setBold()
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
        
        // Add subtle alternating row colors
        table.setBackgroundColor(BACKGROUND, 0.3f);
    }
    
    private void addTableRow(Table table, int rowNum, Map<String, Object> record) {
        // Add row number
        table.addCell(createCell(String.valueOf(rowNum)));
        
        Object dateObj = record.get("attendance_date");
        LocalDate attendanceDate;
        if (dateObj instanceof java.sql.Date) {
            attendanceDate = ((java.sql.Date) dateObj).toLocalDate();
        } else if (dateObj instanceof Instant) {
            attendanceDate = ((Instant) dateObj).atZone(IST).toLocalDate();
        } else {
            throw new ValidationException("Unsupported date type: " + dateObj.getClass());
        }
        
        Cell dateCell = new Cell()
            .add(new Paragraph(DATE_FORMATTER.format(attendanceDate)))
            .setFontColor(TEXT_PRIMARY)
            .setPadding(5)
            .setTextAlignment(TextAlignment.LEFT)
            .setMinHeight(25f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
        
        table.addCell(dateCell);
        
        // Add start time with IST
        Object startTimeObj = record.get("start_date_time");
        String startTime;
        if (startTimeObj instanceof java.sql.Timestamp) {
            startTime = TIME_FORMATTER.format(((java.sql.Timestamp) startTimeObj).toInstant());
        } else if (startTimeObj instanceof Instant) {
            startTime = TIME_FORMATTER.format((TemporalAccessor) startTimeObj);
        } else {
            throw new ValidationException("Unsupported time type: " + startTimeObj.getClass());
        }
        table.addCell(createCell(startTime));

        // Add end time with IST
        Object endTimeObj = record.get("end_date_time");
        String endTime;
        if (endTimeObj instanceof java.sql.Timestamp) {
            endTime = TIME_FORMATTER.format(((java.sql.Timestamp) endTimeObj).toInstant());
        } else if (endTimeObj instanceof Instant) {
            endTime = TIME_FORMATTER.format((TemporalAccessor) endTimeObj);
        } else {
            throw new ValidationException("Unsupported time type: " + endTimeObj.getClass());
        }
        table.addCell(createCell(endTime));

        BigDecimal regularHours = (BigDecimal) record.get("regular_hours");
        BigDecimal regularPay = (BigDecimal) record.get("regular_pay");
        table.addCell(createCell(String.format("%s ", formatNumber(regularHours))));

        BigDecimal overtimeHours = (BigDecimal) record.get("overtime_hours");
        BigDecimal overtimePay = (BigDecimal) record.get("overtime_pay");
        table.addCell(createCell(String.format("%s ", formatNumber(overtimeHours))));

        table.addCell(createCell(formatCurrency((BigDecimal) record.get("total_pay"))));
    }
    
    private Cell createCell(String content) {
        return new Cell()
            .add(new Paragraph(content))
            .setFontColor(TEXT_PRIMARY)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
    }
    
    private void addSummary(Document document, Map<String, BigDecimal> summary) {
        document.add(new Paragraph("\n"));
        
        Table summaryTable = new Table(new float[]{3, 3, 3, 3, 3})
            .useAllAvailableWidth()
            .setMarginTop(20);
        
        // Add decorative header
        Cell summaryHeader = new Cell(1, 5)
            .add(new Paragraph("Monthly Summary")
                .setFontSize(14)
                .setBold())
            .setFontColor(ColorConstants.WHITE)
            .setBackgroundColor(SECONDARY_COLOR)
            .setPadding(10)
            .setTextAlignment(TextAlignment.CENTER);
        
        summaryTable.addCell(summaryHeader);
        
        // Add summary cells
        Cell regularHoursCell = new Cell()
            .add(new Paragraph("Regular Hours")
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(10))
            .add(new Paragraph(formatNumber(summary.get("totalRegularHours")))
                .setBold()
                .setFontColor(TEXT_PRIMARY))
            .add(new Paragraph(formatCurrency(summary.get("totalRegularPay")))
                .setFontColor(ACCENT2))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
        
        Cell overtimeHoursCell = new Cell()
            .add(new Paragraph("Overtime Hours")
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(10))
            .add(new Paragraph(formatNumber(summary.get("totalOvertimeHours")))
                .setBold()
                .setFontColor(TEXT_PRIMARY))
            .add(new Paragraph(formatCurrency(summary.get("totalOvertimePay")))
                .setFontColor(ACCENT2))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
        
        Cell totalPayCell = new Cell(1, 3)
            .add(new Paragraph("Grand Total")
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(12))
            .add(new Paragraph(formatCurrency(summary.get("grandTotal")))
                .setBold()
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
        
        // Add cells to table
        summaryTable.addCell(regularHoursCell);
        summaryTable.addCell(overtimeHoursCell);
        summaryTable.addCell(totalPayCell);
        
        // Table line = new Table(1).useAllAvailableWidth().setMarginTop(5);
        // line.addCell(new Cell()
        //     .setHeight(2f)
        //     .setBackgroundColor(ACCENT1)
        //     .setBorder(Border.NO_BORDER));
        
        document.add(summaryTable);
        // document.add(line);
    }
    
    private Map<String, BigDecimal> calculateSummary(List<Map<String, Object>> records) {
        BigDecimal totalRegularHours = BigDecimal.ZERO;
        BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        BigDecimal totalRegularPay = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;
        
        for (Map<String, Object> record : records) {
            totalRegularHours = totalRegularHours.add((BigDecimal) record.get("regular_hours"));
            totalOvertimeHours = totalOvertimeHours.add((BigDecimal) record.get("overtime_hours"));
            totalRegularPay = totalRegularPay.add((BigDecimal) record.get("regular_pay"));
            totalOvertimePay = totalOvertimePay.add((BigDecimal) record.get("overtime_pay"));
            grandTotal = grandTotal.add((BigDecimal) record.get("total_pay"));
        }
        
        return Map.of(
            "totalRegularHours", totalRegularHours,
            "totalOvertimeHours", totalOvertimeHours,
            "totalRegularPay", totalRegularPay,
            "totalOvertimePay", totalOvertimePay,
            "grandTotal", grandTotal
        );
    }
    
    private String formatCurrency(BigDecimal amount) {
        return String.format("₹%.2f", amount);
    }
    
    private String formatNumber(BigDecimal number) {
        String formatted = number.stripTrailingZeros().toPlainString();
        return formatted;
    }
} 