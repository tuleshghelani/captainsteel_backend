package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employee", indexes = {
    @Index(name = "idx_employee_name", columnList = "name"),
    @Index(name = "idx_employee_mobile", columnList = "mobile_number"),
    @Index(name = "idx_employee_status", columnList = "status"),
    @Index(name = "idx_employee_client_id", columnList = "client_id")
})
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 256)
    private String name;
    
    @Column(name = "mobile_number", length = 15)
    private String mobileNumber;
    
    @Column(name = "email", length = 256)
    private String email;
    
    @Column(name = "address", length = 512)
    private String address;
    
    @Column(name = "designation", length = 100)
    private String designation;
    
    @Column(name = "department", length = 100)
    private String department;
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_employee_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_employee_client_id_client_id"))
    private Client client;
} 