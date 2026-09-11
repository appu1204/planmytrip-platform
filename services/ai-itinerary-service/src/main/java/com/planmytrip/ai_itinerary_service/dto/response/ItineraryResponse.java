package com.planmytrip.ai_itinerary_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("id")
    private UUID id;

    private UUID itineraryId;
    private UUID tripId;
    private Long userId;
    private String destination;
    private Integer durationDays;
    private BigDecimal totalBudget;
    private ItineraryStatus status;
    private Integer version;
    private boolean active;
    private List<ItineraryPlanData.DayPlan> days;
    private ItineraryPlanData.BudgetBreakdown budgetBreakdown;
    private ItineraryPlanData planData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return itineraryId != null ? itineraryId : id;
    }

    public static ItineraryResponse fromEntity(Itinerary entity) {
        ItineraryPlanData pd = entity.getPlanData();
        List<ItineraryPlanData.DayPlan> daysList = pd != null ? pd.getDays() : null;
        ItineraryPlanData.BudgetBreakdown bb = pd != null ? pd.getBudgetBreakdown() : null;

        return ItineraryResponse.builder()
                .id(entity.getId())
                .itineraryId(entity.getId())
                .tripId(entity.getTripId())
                .userId(entity.getUserId())
                .destination(entity.getDestination())
                .durationDays(entity.getDurationDays())
                .totalBudget(entity.getTotalBudget())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .active(entity.isActive())
                .days(daysList)
                .budgetBreakdown(bb)
                .planData(pd)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

