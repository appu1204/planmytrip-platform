package com.planmytrip.ai_itinerary_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * This is the structured shape that we force ai-provider to respond in, and it is what
 * gets persisted as JSON in the `plan_data` column (Itinerary entity).
 * Kept as a single object so Phase 1 (generate), Phase 3 (view) and
 * Phase 5 (budget) -> they all can reuse it without duplicating fields.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItineraryPlanData {

    private List<DayPlan> days;
    private BudgetBreakdown budgetBreakdown;
    private List<String> preferencesUsed;
    private String aiSummary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DayPlan {
        private int dayNumber;
        private String title;
        private String date;             // ISO yyyy-MM-dd
        private List<Activity> activities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Activity {
        private String time;             // example -> "10:00 AM"
        private String title;
        private String description;
        private String tag;              // example -> "Kid-friendly", "Scenic drive"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BudgetBreakdown {
        private BigDecimal stays;
        private BigDecimal activities;
        private BigDecimal transport;
        private BigDecimal buffer;
        private BigDecimal total;
        private String currency;
    }
}
