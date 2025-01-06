package com.inventory.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

@Component
public class CustomDateDeserializer extends JsonDeserializer<OffsetDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateStr = p.getText();
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateStr, formatter);
            return localDateTime.atZone(IST).toOffsetDateTime();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing date: " + dateStr + ". Expected format: dd-MM-yyyy HH:mm:ss", e);
        }
    }
} 