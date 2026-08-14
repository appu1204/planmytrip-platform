package com.planmytrip.trip_service.dto.response;

import com.planmytrip.trip_service.enums.TripStatus;
import com.planmytrip.trip_service.enums.TripType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripResponse {

    private UUID id;
    private UUID userId;
    private String tripName;
    private String destination;
    private TripType tripType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer adults;
    private Integer children;
    private BigDecimal budget;
    private String currency;
    private TripStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
