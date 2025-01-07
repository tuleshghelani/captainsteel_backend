package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationRequestDto {
    private Long quotationId;
    private Long customerId;
    private String customerName;
    private String quoteNumber;
    private String remarks;
    private String termsConditions;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate quoteDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validUntil;
    
    private List<QuotationItemRequestDto> items;
} 