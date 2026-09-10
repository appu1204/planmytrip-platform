package com.planmytrip.ai_itinerary_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.planmytrip.ai_itinerary_service.entity.Itinerary;
import com.planmytrip.ai_itinerary_service.enums.ItineraryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ItineraryResponse {

    private UUID itineraryId;
    private UUID tripId;
    private Long userId;
    private String destination;
    private Integer durationDays;
    private BigDecimal totalBudget;
    private ItineraryStatus status;
    private Integer version;
    private boolean active;
    private ItineraryPlanData planData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ItineraryResponse fromEntity(Itinerary entity) {
        return ItineraryResponse.builder()
                .itineraryId(entity.getId())
                .tripId(entity.getTripId())
                .userId(entity.getUserId())
                .destination(entity.getDestination())
                .durationDays(entity.getDurationDays())
                .totalBudget(entity.getTotalBudget())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .active(entity.isActive())
                .planData(entity.getPlanData())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

