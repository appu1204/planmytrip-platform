package com.planmytrip.trip_service.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.planmytrip.trip_service.enums.TripType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UpdateTripRequest {

    @Size(max = 150, message = "tripName must be at most 150 characters")
    private String tripName;

    @Size(max = 150, message = "destination must be at most 150 characters")
    private String destination;

    private TripType tripType;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(value = 1, message = "adults must be at least 1")
    private Integer adults;

    @Min(value = 0, message = "children cannot be negative")
    private Integer children;

    @DecimalMin(value = "0.0", inclusive = true, message = "budget cannot be negative")
    private BigDecimal budget;

    @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    private String currency; 
}
