package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quotation_items", indexes = {
    @Index(name = "idx_quotation_items_quotation_id", columnList = "quotation_id"),
    @Index(name = "idx_quotation_items_product_id", columnList = "product_id"),
    @Index(name = "idx_quotation_items_client_id", columnList = "client_id")
})
public class QuotationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_quotation_items_quotation_id_quotation_id"))
    private Quotation quotation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_quotation_items_product_id_product_id"))
    private Product product;
    
    @Column(name = "quantity", nullable = false)
    private Long quantity;
    
    @Column(name = "unit_price", precision = 19, scale = 2, columnDefinition = "NUMERIC(19, 2) DEFAULT 0.00")
    private BigDecimal unitPrice = BigDecimal.ZERO;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2, columnDefinition = "NUMERIC(5, 2) DEFAULT 0.00")
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 19, scale = 2, columnDefinition = "NUMERIC(19, 2) DEFAULT 0.00")
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "discount_price", precision = 19, scale = 2, columnDefinition = "NUMERIC(19, 2) DEFAULT 0.00"    )
    private BigDecimal discountPrice = BigDecimal.ZERO;
    
    @Column(name = "tax_percentage", precision = 5, scale = 2, columnDefinition = "NUMERIC(5, 2) DEFAULT 0.00")
    private BigDecimal taxPercentage = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 19, scale = 2, columnDefinition = "NUMERIC(19, 2) DEFAULT 0.00")
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "final_price", precision = 19, scale = 2, columnDefinition = "NUMERIC(19, 2) DEFAULT 0.00")
    private BigDecimal finalPrice = BigDecimal.ZERO;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_quotation_items_client_id_client_id"))
    private Client client;
    
    @Version
    private Long version;
} 