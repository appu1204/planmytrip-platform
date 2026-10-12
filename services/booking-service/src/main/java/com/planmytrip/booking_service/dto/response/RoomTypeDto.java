package com.planmytrip.booking_service.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 

public class RoomTypeDto {
    private String roomTypeId;
    private String name;
    private int sleeps;
    private BigDecimal pricePerNight;
    private String currency;
    private boolean available;
    private boolean refundable;
    private String freeCancellationUntil; // ISO date, nullable
}
