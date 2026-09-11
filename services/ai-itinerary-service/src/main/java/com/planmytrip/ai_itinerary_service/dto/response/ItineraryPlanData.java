package com.planmytrip.ai_itinerary_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
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
public class ItineraryPlanData implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<DayPlan> days;
    private BudgetBreakdown budgetBreakdown;
    private List<String> preferencesUsed;
    private String aiSummary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DayPlan implements Serializable {
        private static final long serialVersionUID = 1L;

        private int dayNumber;
        private String title;
        private String date;             // ISO yyyy-MM-dd
        private List<Activity> activities;

        public String getId() {
            return "day-" + (dayNumber > 0 ? dayNumber : 1);
        }

        public void setId(String id) {}

        public int getIndex() {
            return dayNumber > 0 ? dayNumber : 1;
        }

        public void setIndex(int index) {
            this.dayNumber = index;
        }

        public String getLabel() {
            return (title != null && !title.isBlank()) ? title : "Day " + getIndex();
        }

        public void setLabel(String label) {
            this.title = label;
        }

        public String getDateLabel() {
            if (date != null && !date.isBlank()) {
                try {
                    java.time.LocalDate ld = java.time.LocalDate.parse(date);
                    return ld.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMM"));
                } catch (Exception ignored) {}
                return date;
            }
            return "Day " + getIndex();
        }

        public void setDateLabel(String dateLabel) {}
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Activity implements Serializable {
        private static final long serialVersionUID = 1L;

        private String time;             // example -> "10:00 AM"
        private String title;
        private String description;
        private String tag;              // example -> "Kid-friendly", "Scenic drive"

        public String getId() {
            String sanitizedTime = time != null ? time.replaceAll("\\s+", "") : "time";
            int hash = title != null ? Math.abs(title.hashCode()) : 0;
            return "act-" + sanitizedTime + "-" + hash;
        }

        public void setId(String id) {}

        public String getNote() {
            return description != null ? description : "";
        }

        public void setNote(String note) {
            if (this.description == null || this.description.isBlank()) {
                this.description = note;
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BudgetBreakdown implements Serializable {
        private static final long serialVersionUID = 1L;

        private BigDecimal stays;
        private BigDecimal activities;
        private BigDecimal transport;
        private BigDecimal buffer;
        private BigDecimal total;
        private String currency;
    }
}
