package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Long clientId;
}