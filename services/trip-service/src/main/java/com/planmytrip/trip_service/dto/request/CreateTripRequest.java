package com.planmytrip.trip_service.dto.request;

import com.planmytrip.trip_service.enums.TripType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CreateTripRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "tripName is required")
    @Size(max = 150, message = "tripName must be at most 150 characters")
    private String tripName;

    @NotBlank(message = "destination is required")
    @Size(max = 150, message = "destination must be at most 150 characters")
    private String destination;

    @NotNull(message = "tripType is required")
    private TripType tripType;

    @NotNull(message = "startDate is required")
    @FutureOrPresent(message = "startDate cannot be in the past")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    @NotNull(message = "adults is required")
    @Min(value = 1, message = "adults must be at least 1")
    private Integer adults;

    @NotNull(message = "children is required")
    @Min(value = 0, message = "children cannot be negative")
    private Integer children;

    @DecimalMin(value = "0.0", inclusive = true, message = "budget cannot be negative")
    private BigDecimal budget;

    @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    private String currency;

}
