package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryDto {
    private Long id;
    private String name;
    private String search;
    private String status;
    private Long remainingQuantity;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
    private Long clientId;
}