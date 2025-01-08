package com.inventory.entity;

import com.inventory.enums.QuotationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

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
    
    @Column(name = "quote_date", nullable = false, columnDefinition = "DATE")
    private LocalDate quoteDate = LocalDate.now();

    @Column(name = "quote_number")
    private String quoteNumber;
    
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
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