package com.inventory.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Date;

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
public class PurchaseDto {
    private Long id;
    private Long productId;
    private Long categoryId;
    private Long customerId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Long clientId;
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy", timezone = "IST")
    private OffsetDateTime purchaseDate;
    private String invoiceNumber;
    private BigDecimal otherExpenses;
    private Integer currentPage;
    private Integer perPageRecord;
    private String search;
    private String status;
    private BigDecimal discount;
    private BigDecimal discountAmount;
    private BigDecimal discountPrice;
    private int page;
    private int size;
    private Date startDate;
    private Date endDate;
}