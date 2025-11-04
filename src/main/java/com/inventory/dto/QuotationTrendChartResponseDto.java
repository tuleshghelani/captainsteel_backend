package com.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationTrendChartResponseDto {
    private String period; // e.g., "2024-01", "2024-01-15"
    private Long count;
    private BigDecimal totalAmount;
    private List<StatusData> statusData; // Breakdown by status for each period
    
    @Data
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusData {
        private String status;
        private String statusLabel;
        private Long count;
        private BigDecimal totalAmount;
    }
}

