package com.planmytrip.trip_service.service;

import org.springframework.stereotype.Service;

import com.planmytrip.trip_service.dto.response.DestinationResponse;
import com.planmytrip.trip_service.enums.TripType;
import java.util.List;

@Service
public interface DestinationService {

    List<DestinationResponse> getPopularDestinations(TripType persona, Integer limit);

}
