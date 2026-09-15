package com.planmytrip.ai_itinerary_service.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Safely parses ISO date strings (yyyy-MM-dd).
 * Returns null if string is empty or blank, preventing deserialization errors when
 * the frontend sends empty strings ("") for unselected date fields.
 */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
