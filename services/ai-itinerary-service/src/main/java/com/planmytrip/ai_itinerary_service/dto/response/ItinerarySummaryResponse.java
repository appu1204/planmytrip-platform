package com.planmytrip.ai_itinerary_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.planmytrip.ai_itinerary_service.entity.Itinerary;
import com.planmytrip.ai_itinerary_service.enums.ItineraryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 

public class ItinerarySummaryResponse {

    private UUID itineraryId;
    private Integer version;
    private ItineraryStatus status;
    private boolean active;
    private String aiSummary;
    private LocalDateTime createdAt;

    public static ItinerarySummaryResponse fromEntity(Itinerary entity) {
        return ItinerarySummaryResponse.builder()
                .itineraryId(entity.getId())
                .version(entity.getVersion())
                .status(entity.getStatus())
                .active(entity.isActive())
                .aiSummary(entity.getPlanData() != null ? entity.getPlanData().getAiSummary() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

}
