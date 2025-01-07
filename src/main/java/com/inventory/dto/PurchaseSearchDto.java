package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseSearchDto {
    private String invoiceNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long productId;
    private Long clientId;
    private Integer currentPage = 0;
    private Integer perPageRecord = 10;
    private String sortBy = "purchaseDate";
    private String sortDir = "desc";
} 