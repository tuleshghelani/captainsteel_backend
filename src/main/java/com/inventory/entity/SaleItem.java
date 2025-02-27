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
@Table(name = "sale_items", indexes = {
    @Index(name = "idx_sale_items_sale_id", columnList = "sale_id"),
    @Index(name = "idx_sale_items_product_id", columnList = "product_id")
})
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sale_items_sale_id"))
    private Sale sale;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sale_items_product_id"))
    private Product product;
    
    @Column(name = "quantity", nullable = false, columnDefinition = "numeric(12,3) ")
    private BigDecimal quantity = BigDecimal.ZERO;
    
    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;

    @Column(name = "coil_number")
    private String coilNumber;
    
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "discount_percentage", precision = 6, scale = 4, columnDefinition = "decimal(6,4) DEFAULT 0")
    private BigDecimal discountPercentage;
    
    @Column(name = "discount_amount", precision = 19, scale = 2, columnDefinition = "decimal(19,2) DEFAULT 0")
    private BigDecimal discountAmount;
    
    @Column(name = "final_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPrice;
    
//    @Column(name = "remaining_quantity", columnDefinition = "numeric(12,3) DEFAULT 0.000")
//    private BigDecimal remainingQuantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_sale_item_client_id_client_id"))
    private Client client;
} 