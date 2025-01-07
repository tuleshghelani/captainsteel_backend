package com.inventory.service;

import com.inventory.entity.Client;
import com.inventory.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class QuoteNumberGeneratorService {
    private final ClientRepository clientRepository;
    
    @Transactional
    public String generateQuoteNumber(Client client) {
        // Using pessimistic lock to ensure thread safety
        Client lockedClient = clientRepository.findByIdWithPessimisticLock(client.getId())
            .orElseThrow(() -> new RuntimeException("Client not found"));
            
        Long nextNumber = (lockedClient.getLastQuoteNumber() == null || 
                          lockedClient.getLastQuoteNumber() < 1) ? 
                          1L : lockedClient.getLastQuoteNumber() + 1;
                          
        lockedClient.setLastQuoteNumber(nextNumber);
        clientRepository.save(lockedClient);
        
        return String.format("QT-%d-%d", Year.now().getValue(), nextNumber);
    }
} 