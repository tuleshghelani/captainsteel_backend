package com.inventory.entity;

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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
@Table(name = "\"user_master\"", indexes = {
    @Index(name = "idx_user_master_email", columnList = "email"),
    @Index(name = "idx_user_master_client_id", columnList = "client_id")
}, uniqueConstraints={
        @UniqueConstraint( name = "uk_user_master_email",  columnNames ={"email"})
})
public class UserMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @Column(name = "password", nullable = false, length = 256)
    private String password;
    
    @Column(name = "first_name", length = 64)
    private String firstName;
    
    @Column(name = "last_name", length = 64)
    private String lastName;
    
    @Column(name = "jwt_token", length = 256)
    private String jwtToken;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";
    
    @Column(name = "email", length = 64)
    private String email;
    
    @Column(name = "refresh_token", length = 64)
    private String refreshToken;
    
    @Column(name = "refresh_token_expiry")
    private OffsetDateTime refreshTokenExpiry;
    
    @Column(name = "fail_login_count", nullable = false)
    private Integer failLoginCount = 0;
    
    @Column(name = "lock_time")
    private OffsetDateTime lockTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_user_master_client_id_client_id"))
    private Client client;
}