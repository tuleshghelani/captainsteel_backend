package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeDto {
    private Long id;
    private String name;
    private String mobileNumber;
    private String email;
    private String address;
    private String designation;
    private String department;
    private String status;
    private Long clientId;
    // Search and pagination parameters
    private String search;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
} 