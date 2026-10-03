package com.planmytrip.trip_service.mapper;

import com.planmytrip.trip_service.dto.request.CreateTripRequest;
import com.planmytrip.trip_service.dto.request.UpdateTripRequest;
import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.entity.Trip;
import com.planmytrip.trip_service.enums.TripStatus;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {

    public Trip toEntity(CreateTripRequest request) {
        return Trip.builder()
                .userId(request.getUserId())
                .tripName(request.getTripName())
                .destination(request.getDestination())
                .tripType(request.getTripType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .adults(request.getAdults())
                .children(request.getChildren())
                .budget(request.getBudget())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .status(TripStatus.DRAFT)
                .build();
    }

    /**
     * Applies only the non-null fields from the request onto the existing
     * trip. Anything omitted from the request is left exactly as it was.
     */
    public void applyUpdate(Trip trip, UpdateTripRequest request) {
        if (request.getTripName() != null) {
            trip.setTripName(request.getTripName());
        }
        if (request.getDestination() != null) {
            trip.setDestination(request.getDestination());
        }
        if (request.getTripType() != null) {
            trip.setTripType(request.getTripType());
        }
        if (request.getStartDate() != null) {
            trip.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            trip.setEndDate(request.getEndDate());
        }
        if (request.getAdults() != null) {
            trip.setAdults(request.getAdults());
        }
        if (request.getChildren() != null) {
            trip.setChildren(request.getChildren());
        }
        if (request.getBudget() != null) {
            trip.setBudget(request.getBudget());
        }
        if (request.getCurrency() != null) {
            trip.setCurrency(request.getCurrency());
        }
    }

    public TripResponse toResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .userId(trip.getUserId())
                .tripName(trip.getTripName())
                .destination(trip.getDestination())
                .tripType(trip.getTripType())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .adults(trip.getAdults())
                .children(trip.getChildren())
                .budget(trip.getBudget())
                .currency(trip.getCurrency())
                .status(trip.getStatus())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }
}
