package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class SaleRequestDto {
    private Long id;
    private Long customerId;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date saleDate;
    private String invoiceNumber;
    private List<SaleItemDto> products;
    private List<String> coilNumbers;
    private Boolean isBlack;
}