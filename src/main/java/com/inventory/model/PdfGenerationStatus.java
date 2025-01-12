package com.inventory.model;

import lombok.Data;
import java.time.Instant;

@Data
public class PdfGenerationStatus {
    private final Instant startTime;
    private final long timeoutMinutes = 5;
    
    public PdfGenerationStatus() {
        this.startTime = Instant.now();
    }
    
    public boolean isStale() {
        return Instant.now().isAfter(startTime.plusSeconds(timeoutMinutes * 60));
    }
} 