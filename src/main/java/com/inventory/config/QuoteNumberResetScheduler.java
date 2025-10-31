package com.inventory.config;

import com.inventory.entity.Client;
import com.inventory.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuoteNumberResetScheduler {
    private final ClientRepository clientRepository;
    
    /**
     * Reset all clients' last_quote_number to 0 every year on April 1st at 00:00:00
     * Cron expression: second minute hour day month day-of-week
     * "0 0 0 1 4 *" = At 00:00:00 on April 1st every year
     */
    @Scheduled(cron = "0 0 0 1 4 *")
    @Transactional
    public void resetQuoteNumbers() {
        log.info("Starting annual quote number reset on April 1st at {}", LocalDateTime.now());
        
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
        } catch (Exception e) {
            log.error("Error occurred while resetting quote numbers: {}", e.getMessage(), e);
            throw e; // Re-throw to ensure transaction rollback
        }
    }
}
