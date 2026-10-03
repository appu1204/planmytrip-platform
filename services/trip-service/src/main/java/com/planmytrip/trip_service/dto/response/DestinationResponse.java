package com.planmytrip.trip_service.dto.response;

import java.util.UUID;

import com.planmytrip.trip_service.enums.TripType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String name;
    private String country;
    private String imageUrl;
    private TripType persona;
    private Integer popularityRank;

}
