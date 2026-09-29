package com.planmytrip.booking_service.dto.response;

import com.planmytrip.booking_service.entity.Booking;
import com.planmytrip.booking_service.enums.BookingModule;
import com.planmytrip.booking_service.enums.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The ONE response shape returned after create/get, identical across
 * Hotel, Flight, Transport and Activity (— "Booking confirmation").
 * GET /api/bookings/{bookingId}
 */
@Getter 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {

    private String bookingId;
    private BookingModule module;
    private BookingStatus status;
    private String referenceNo;
    private String itemName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amount;
    private String currency;

    public static BookingResponseDto fromEntity(Booking booking) {
        return BookingResponseDto.builder()
                .bookingId(booking.getId())
                .module(booking.getModule())
                .status(booking.getStatus())
                .referenceNo(booking.getReferenceNo())
                .itemName(booking.getItemName())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .amount(booking.getAmount())
                .currency(booking.getCurrency())
                .build();
    }
}

