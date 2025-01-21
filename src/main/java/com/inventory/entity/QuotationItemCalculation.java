package com.inventory.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "quotation_item_calculations", indexes = {
    @Index(name = "idx_quotation_item_calculations_quotation_item_id", columnList = "quotation_item_id")
})
public class QuotationItemCalculation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_item_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_quotation_item_calculations_quotation_item_id"))
    private QuotationItem quotationItem;
    
    @Column(name = "feet", precision = 8, scale = 2, columnDefinition = "numeric(8,2) DEFAULT 0.00")
    private BigDecimal feet = BigDecimal.ZERO;
    
    @Column(name = "inch", precision = 8, scale = 2, columnDefinition = "numeric(8,2) DEFAULT 0.00")
    private BigDecimal inch = BigDecimal.ZERO;
    
    @Column(name = "nos", nullable = false)
    private Long nos = 0L;
    
    @Column(name = "running_feet", precision = 8, scale = 2, columnDefinition = "numeric(8,2) DEFAULT 0.00")
    private BigDecimal runningFeet = BigDecimal.ZERO;
    
    @Column(name = "sq_feet", precision = 8, scale = 2, columnDefinition = "numeric(8,2) DEFAULT 0.00")
    private BigDecimal sqFeet = BigDecimal.ZERO;
    
    @Column(name = "weight", precision = 8, scale = 2, columnDefinition = "numeric(8,2) DEFAULT 0.00")
    private BigDecimal weight = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quotation_item_calculations_client_id_client_id"))
    private Client client;
} 