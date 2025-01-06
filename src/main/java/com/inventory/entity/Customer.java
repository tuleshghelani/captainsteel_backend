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
@Table(name = "customer", indexes = {
    @Index(name = "idx_customer_name", columnList = "name"),
    @Index(name = "idx_customer_mobile", columnList = "mobile"),
    @Index(name = "idx_customer_email", columnList = "email"),
    @Index(name = "idx_customer_remaining_payment_amount", columnList = "remaining_payment_amount"),
    @Index(name = "idx_customer_client_id", columnList = "client_id")
})
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 256)
    private String name;
    
    @Column(name = "gst", length = 15)
    private String gst;
    
    @Column(name = "address", length = 512)
    private String address;
    
    @Column(name = "mobile", length = 15)
    private String mobile;

    @Column(name = "coating_unit_price", precision = 19, scale = 2)
    private BigDecimal coatingUnitPrice;
    
    @Column(name = "remaining_payment_amount", precision = 19, scale = 2)
    private BigDecimal remainingPaymentAmount;
    
    @Column(name = "next_action_date")
    private OffsetDateTime nextActionDate;
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_customer_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";
    
    @Column(name = "email", length = 256)
    private String email;
    
    @Column(name = "remarks", length = 1000)
    private String remarks;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_customer_client_id_client_id"))
    private Client client;
} 