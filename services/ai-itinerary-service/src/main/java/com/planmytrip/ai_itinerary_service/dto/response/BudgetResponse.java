package com.planmytrip.ai_itinerary_service.dto.response;

import java.util.UUID;

import com.planmytrip.ai_itinerary_service.entity.Itinerary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 

public class BudgetResponse {
    private UUID itineraryId;
    private UUID tripId;
    private ItineraryPlanData.BudgetBreakdown budgetBreakdown;

    public static BudgetResponse fromEntity(Itinerary entity) {
        return BudgetResponse.builder()
                .itineraryId(entity.getId())
                .tripId(entity.getTripId())
                .budgetBreakdown(entity.getPlanData().getBudgetBreakdown())
                .build();
    }

}
