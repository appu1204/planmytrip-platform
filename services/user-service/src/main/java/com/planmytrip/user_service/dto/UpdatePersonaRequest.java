package com.planmytrip.user_service.dto;

import com.planmytrip.user_service.enums.TravelPersona;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePersonaRequest {

    @NotNull(message = "Persona must not be null")
    private TravelPersona persona;
}