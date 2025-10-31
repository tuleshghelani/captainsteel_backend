package com.inventory.service;

import com.inventory.entity.Client;
import com.inventory.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteNumberResetService {
    private final ClientRepository clientRepository;
    
    /**
     * Reset all clients' last_quote_number to 0
     * Called by scheduled task or manually via API
     */
    @Transactional
    public int resetQuoteNumbers() {
        log.info("Starting quote number reset at {}", LocalDateTime.now());
        
        try {
            List<Client> allClients = clientRepository.findAll();
            
            int resetCount = 0;
            for (Client client : allClients) {
                Long currentQuoteNumber = client.getLastQuoteNumber();
                
                // Only reset if the number is not already 0
                if (currentQuoteNumber != null && currentQuoteNumber != 0) {
                    client.setLastQuoteNumber(0L);
                    clientRepository.save(client);
                    resetCount++;
                    log.debug("Reset quote number for client: {} (was: {})", client.getId(), currentQuoteNumber);
                }
            }
            
            log.info("Successfully reset quote numbers for {} clients", resetCount);
            return resetCount;
        } catch (Exception e) {
            log.error("Error occurred while resetting quote numbers: {}", e.getMessage(), e);
            throw e; // Re-throw to ensure transaction rollback
        }
    }
}
