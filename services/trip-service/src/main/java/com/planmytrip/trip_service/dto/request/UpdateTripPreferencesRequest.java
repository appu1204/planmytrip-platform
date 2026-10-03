package com.planmytrip.trip_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.planmytrip.trip_service.enums.TripPreferenceType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UpdateTripPreferencesRequest {

    @NotEmpty(message = "preferences must contain at least one item")
    @Size(max = 9, message = "preferences cannot exceed 9 items")
    private List<TripPreferenceType> preferences;
}
