package com.planmytrip.trip_service.dto.request;

import com.planmytrip.trip_service.enums.TripStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ Builder

public class UpdateTripStatusRequest {
    
    @NotNull(message = "status is required")
    private TripStatus status;
}
