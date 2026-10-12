package com.planmytrip.booking_service.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 

public class HotelDetailDto {

    private String hotelId;
    private String name;
    private String location;
    private double ratingScore;
    private int reviewCount;
    private List<String> amenities;
    private List<RoomTypeDto> roomTypes;
    private String cancellationPolicy;
}
