package com.inventory.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "product", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_category_name", columnNames = {"category_id", "name"})
    },
    indexes = {
        @Index(name = "idx_product_name", columnList = "name"),
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_remaining_quantity", columnList = "remaining_quantity"),
        @Index(name = "idx_product_category_id", columnList = "category_id"),
        @Index(name = "idx_product_client_id", columnList = "client_id")
    }
)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 256)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_product_category_id_category_id"))
    private Category category;

    @Column(name = "purchase_amount", precision = 19, scale = 2, columnDefinition = "numeric(19,2) ")
    private BigDecimal purchaseAmount = BigDecimal.valueOf(0.00);

    @Column(name = "sale_amount", precision = 19, scale = 2, columnDefinition = "numeric(19,2) ")
    private BigDecimal saleAmount = BigDecimal.valueOf(0.00);
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_product_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";

    @Column(name = "remaining_quantity", columnDefinition = "bigint DEFAULT 0")
    private Long remainingQuantity = 0L;

    @Column(name = "blocked_quantity", columnDefinition = "bigint DEFAULT 0")
    private Long blockedQuantity = 0L;

    @Column(name = "total_remaining_quantity", columnDefinition = "bigint DEFAULT 0")
    private Long totalRemainingQuantity = 0L;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "minimum_stock", precision = 19, scale = 2)
    private BigDecimal minimumStock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_product_client_id_client_id"))
    private Client client;
}