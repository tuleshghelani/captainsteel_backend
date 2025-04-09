package com.inventory.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.inventory.enums.QuotationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quotation", indexes = {
    @Index(name = "idx_quotation_customer_id", columnList = "customer_id"),
    @Index(name = "idx_quotation_status", columnList = "status"),
    @Index(name = "idx_quotation_quote_date", columnList = "quote_date"),
    @Index(name = "idx_quotation_client_id", columnList = "client_id"),
    @Index(name = "idx_quotation_quote_number", columnList = "quote_number")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_quotation_quote_number", columnNames = "quote_number")
})
public class Quotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id",
        foreignKey = @ForeignKey(name = "fk_quotation_customer_id_customer_id"))
    private Customer customer;

    @Column(name = "customer_name")
    private String customerName;
    
    @Column(name = "contact_number", length = 20)
    private String contactNumber;
    
    @Column(name = "address", columnDefinition="varchar")
    private String address;
    
    @Column(name = "quote_date", nullable = false, columnDefinition = "DATE")
    private LocalDate quoteDate = LocalDate.now();

    @Column(name = "quote_number")
    private String quoteNumber;
    
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discounted_price", precision = 19, scale = 2)
    private BigDecimal discountedPrice = BigDecimal.ZERO;
    
    @Column(name = "loading_charge", precision = 17, scale = 2, columnDefinition = "NUMERIC(17, 2) DEFAULT 0.00"    )
    private BigDecimal loadingCharge = BigDecimal.ZERO;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuotationStatus status = QuotationStatus.Q;
    
    @Column(name = "valid_until", columnDefinition = "DATE")
    private LocalDate validUntil;
    
    @Column(name = "remarks", length = 1000)
    private String remarks;
    
    @Column(name = "terms_conditions", length = 2000)
    private String termsConditions;
    
    @Column(name = "created_at", nullable = false, length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", 
        foreignKey = @ForeignKey(name = "fk_quotation_created_by_user_master_id"))
    private UserMaster createdBy;
        
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_quotation_updated_by_user_master_id"))
    private UserMaster updatedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_quotation_client_id"))
    private Client client;
    
    @Version
    private Long version;
} 