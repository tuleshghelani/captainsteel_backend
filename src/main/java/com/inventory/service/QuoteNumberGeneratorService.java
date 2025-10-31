package com.inventory.service;

import com.inventory.entity.Client;
import com.inventory.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.Month;

@Service
@RequiredArgsConstructor
public class QuoteNumberGeneratorService {
    private final ClientRepository clientRepository;
    
    @Transactional
    public synchronized String generateQuoteNumber(Client client) {
        // Using pessimistic lock to ensure thread safety
        Client lockedClient = clientRepository.findByIdWithPessimisticLock(client.getId())
            .orElseThrow(() -> new RuntimeException("Client not found"));
            
        Long nextNumber = (lockedClient.getLastQuoteNumber() == null || 
                          lockedClient.getLastQuoteNumber() < 1) ? 
                          1L : lockedClient.getLastQuoteNumber() + 1;
                          
        lockedClient.setLastQuoteNumber(nextNumber);
        clientRepository.save(lockedClient);
        
        // Generate financial year format: QT-2025-26-1
        String financialYear = getFinancialYear();
        return String.format("QT-%s-%d", financialYear, nextNumber);
    }
    
    /**
     * Get current financial year in format YYYY-YY
     * Financial year runs from April 1 to March 31
     * @return Financial year string (e.g., "2025-26" or "2026-27")
     */
    private String getFinancialYear() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        
        // If month is April or later, financial year is current-next
        // If month is January to March, financial year is previous-current
        if (today.getMonth().compareTo(Month.APRIL) >= 0) {
            // April to December: FY is current year to next year
            int nextYear = currentYear + 1;
            return String.format("%d-%02d", currentYear, nextYear % 100);
        } else {
            // January to March: FY is previous year to current year
            int previousYear = currentYear - 1;
            return String.format("%d-%02d", previousYear, currentYear % 100);
        }
    }
} 