package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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
public class QuotationItemDetailDto {
    // Quotation Item fields
    private Long id;
    private Long productId;
    private String productName;
    private String productType;
    private String calculationType;
    private String calculationBase;
    private BigDecimal weight;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal discountPrice;
    private BigDecimal taxAmount;
    private BigDecimal finalPrice;
    private BigDecimal loadingCharge;
    private String accessoriesSize;
    private Integer nos;
    private String itemRemarks;
    private Boolean isProduction;
    private String quotationItemStatus;
    private BigDecimal quotationDiscountAmount;
    
    // Quotation fields
    private Long quotationId;
    private String quoteNumber;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate quoteDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validUntil;
    
    private String quotationStatus;
    private String customerName;
    private Long customerId;
    private String contactNumber;
    private String address;
    private BigDecimal quotationTotalAmount;
    private BigDecimal quotationDiscount;
    
    // Getters and setters
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductType() { return productType; }
    public String getCalculationType() { return calculationType; }
    public String getCalculationBase() { return calculationBase; }
    public BigDecimal getWeight() { return weight; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getDiscountPrice() { return discountPrice; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public BigDecimal getLoadingCharge() { return loadingCharge; }
    public String getAccessoriesSize() { return accessoriesSize; }
    public Integer getNos() { return nos; }
    public String getItemRemarks() { return itemRemarks; }
    public Boolean getIsProduction() { return isProduction; }
    public String getQuotationItemStatus() { return quotationItemStatus; }
    public BigDecimal getQuotationDiscountAmount() { return quotationDiscountAmount; }
    
    public Long getQuotationId() { return quotationId; }
    public String getQuoteNumber() { return quoteNumber; }
    public LocalDate getQuoteDate() { return quoteDate; }
    public LocalDate getValidUntil() { return validUntil; }
    public String getQuotationStatus() { return quotationStatus; }
    public String getCustomerName() { return customerName; }
    public Long getCustomerId() { return customerId; }
    public String getContactNumber() { return contactNumber; }
    public String getAddress() { return address; }
    public BigDecimal getQuotationTotalAmount() { return quotationTotalAmount; }
    public BigDecimal getQuotationDiscount() { return quotationDiscount; }
}