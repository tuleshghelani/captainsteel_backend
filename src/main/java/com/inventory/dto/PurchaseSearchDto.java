package com.inventory.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
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