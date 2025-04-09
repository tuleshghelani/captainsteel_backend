package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sale", indexes = {
    @Index(name = "idx_sale_invoice_number", columnList = "invoice_number"),
    @Index(name = "idx_sale_sale_date", columnList = "sale_date"),
    @Index(name = "idx_sale_customer_id", columnList = "customer_id"),
    @Index(name = "idx_sale_client_id", columnList = "client_id")
})
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_sale_customer_id_customer_id"))
    private Customer customer;
    
    @Column(name = "sale_date", nullable = false)
    private Date saleDate;

    @Column(name = "total_sale_amount", precision = 19, scale = 2, columnDefinition = "decimal(19,2) DEFAULT 0")
    private BigDecimal totalSaleAmount;
    
    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "number_of_items")
    private Integer numberOfItems;

    @Column(name = "coil_numbers", columnDefinition = "jsonb default '[]'")
    @Type(value = com.vladmihalcea.hibernate.type.json.JsonType.class)
    private List<String> coilNumbers = new ArrayList<>();

    @Column(name = "is_black", columnDefinition = "bool default false")
    private Boolean isBlack = false;
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(nullable = false, length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_sale_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_sale_updated_by_user_master_id"))
    private UserMaster updatedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_sale_client_id_client_id"))
    private Client client;
    
//    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<SaleItem> saleItems = new ArrayList<>();
}