package com.inventory.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "purchase", indexes = {
    @Index(name = "idx_purchase_invoice_number", columnList = "invoice_number"),
    @Index(name = "idx_purchase_purchase_date", columnList = "purchase_date"),
    @Index(name = "idx_purchase_customer_id", columnList = "customer_id"),
    @Index(name = "idx_purchase_client_id", columnList = "client_id")
})
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_purchase_customer_id_customer_id"))
    private Customer customer;
    
    @Column(name = "purchase_date", nullable = false)
    private Date purchaseDate;

    @Column(name = "total_purchase_amount", precision = 19, scale = 2, columnDefinition = "decimal(19,2) DEFAULT 0")
    private BigDecimal totalPurchaseAmount;
    
    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "number_of_items")
    private Integer numberOfItems;

    @Column(name = "coil_numbers", columnDefinition = "jsonb default '[]'")
    @Type(value = com.vladmihalcea.hibernate.type.json.JsonType.class)
    private List<String> coilNumbers = new ArrayList<>();
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(nullable = false, length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_purchase_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_purchase_updated_by_user_master_id"))
    private UserMaster updatedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_purchase_client_id_client_id"))
    private Client client;
    
//    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<PurchaseItem> purchaseItems = new ArrayList<>();
}