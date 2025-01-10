package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance", indexes = {
    @Index(name = "idx_attendance_employee_id", columnList = "employee_id"),
    @Index(name = "idx_attendance_date", columnList = "start_date_time"),
    @Index(name = "idx_attendance_client_id", columnList = "client_id")
})
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @Column(name = "start_date_time", nullable = false)
    private OffsetDateTime startDateTime;
    
    @Column(name = "end_date_time", nullable = false)
    private OffsetDateTime endDateTime;
    
    @Column(name = "regular_hours", precision = 4, scale = 2)
    private BigDecimal regularHours;
    
    @Column(name = "overtime_hours", precision = 4, scale = 2)
    private BigDecimal overtimeHours;
    
    @Column(name = "regular_pay", precision = 8, scale = 2)
    private BigDecimal regularPay;
    
    @Column(name = "overtime_pay", precision = 8, scale = 2)
    private BigDecimal overtimePay;
    
    @Column(name = "total_pay", precision = 8, scale = 2)
    private BigDecimal totalPay;
    
    @Column(name = "remarks", length = 255)
    private String remarks;
    
    @Column(name = "created_at", nullable = false, length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserMaster createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_attendance_client_id_client_id"))
    private Client client;
} 