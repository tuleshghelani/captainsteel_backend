package com.inventory.dto;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class PurchaseRequestDto {
    private Long id;
    private Long customerId;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date purchaseDate;
    private String invoiceNumber;
    private List<PurchaseItemDto> products;
}