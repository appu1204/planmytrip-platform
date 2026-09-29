package com.planmytrip.booking_service.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@Builder
@AllArgsConstructor 
@NoArgsConstructor 

public class HotelSearchResultDto {
    
    private String hotelId;
    private String name;
    private String location;
    private double ratingScore;
    private int reviewCount;
    private BigDecimal pricePerNight;
    private String currency;
    private List<String> amenities;
    private boolean freeCancellation;

}
