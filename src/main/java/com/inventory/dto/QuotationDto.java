package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String contactNumber;
    private String address;
    
    // Getters (in case Lombok is not working)
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getQuoteNumber() { return quoteNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDate getQuoteDate() { return quoteDate; }
    public LocalDate getValidUntil() { return validUntil; }
    public String getRemarks() { return remarks; }
    public String getTermsConditions() { return termsConditions; }
    public List<QuotationItemRequestDto> getItems() { return items; }
    public String getSearch() { return search; }
    public Integer getCurrentPage() { return currentPage; }
    public Integer getPerPageRecord() { return perPageRecord; }
    public String getSortBy() { return sortBy; }
    public String getSortDir() { return sortDir; }
    public Long getClientId() { return clientId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getContactNumber() { return contactNumber; }
    public String getAddress() { return address; }
}