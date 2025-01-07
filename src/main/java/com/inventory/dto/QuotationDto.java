package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private String quoteNumber;
    private BigDecimal totalAmount;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate quoteDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validUntil;
    
    private String remarks;
    private String termsConditions;
    private List<QuotationItemRequestDto> items;
    
    // Search parameters
    private String search;
    private Integer currentPage = 0;
    private Integer perPageRecord = 10;
//    private Integer currentPage;
//    private Integer perPageRecord;
    private String sortBy = "id";
    private String sortDir = "desc";
    private Long clientId;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
} 