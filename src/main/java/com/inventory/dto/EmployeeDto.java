package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeDto {
    private Long id;
    private String name;
    private String mobileNumber;
    private String email;
    private String address;
    private String designation;
    private String department;
    private String wageType;
    private BigDecimal regularHours;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    
    private BigDecimal regularPay;
    private BigDecimal overtimePay;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime createdAt;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime updatedAt;
    
    private String status;
    private Long clientId;
    private Long createdById;
    
    // Search and pagination parameters
    private String search;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
} 