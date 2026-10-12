package com.planmytrip.booking_service.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.planmytrip.booking_service.client.HotelProviderClient;
import com.planmytrip.booking_service.dto.request.HotelBookingRequestDto;
import com.planmytrip.booking_service.dto.response.BookingResponseDto;
import com.planmytrip.booking_service.dto.response.HotelDetailDto;
import com.planmytrip.booking_service.dto.response.HotelSearchPageResponse;
import com.planmytrip.booking_service.dto.response.HotelSearchResultDto;
import com.planmytrip.booking_service.dto.response.ProviderBookingResult;
import com.planmytrip.booking_service.dto.response.RoomTypeDto;
import com.planmytrip.booking_service.entity.Booking;
import com.planmytrip.booking_service.enums.BookingModule;
import com.planmytrip.booking_service.enums.BookingStatus;
import com.planmytrip.booking_service.exception.HotelNotFoundException;
import com.planmytrip.booking_service.exception.RoomUnavailableException;
import com.planmytrip.booking_service.service.BookingService;
import com.planmytrip.booking_service.service.HotelBookingService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class HotelBookingServiceImpl implements HotelBookingService {
    
    private static final AtomicLong REF_SEQ = new AtomicLong(1000000L);

    private final HotelProviderClient hotelProviderClient;
    private final BookingService bookingService;

    @Override 
    public HotelSearchPageResponse search(String destination, LocalDate checkIn, LocalDate checkOut,
                                          int adults, int children,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          int page, int size) {
        
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("checkIn date cannot be in the past");
        }
        if (adults < 1) {
            throw new IllegalArgumentException("adults count must be at least 1");
        }
        if (children < 0) {
            throw new IllegalArgumentException("children count cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page index cannot be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("page size must be at least 1");
        }

        List<HotelSearchResultDto> results = hotelProviderClient.search(destination, checkIn, checkOut, adults, children);
        if (results == null) {
            results = List.of();
        }

        List<HotelSearchResultDto> filtered = results.stream()
                .filter(r -> minPrice == null || (r.getPricePerNight() != null && r.getPricePerNight().compareTo(minPrice) >= 0))
                .filter(r -> maxPrice == null || (r.getPricePerNight() != null && r.getPricePerNight().compareTo(maxPrice) <= 0))
                .collect(Collectors.toList());

        long totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        int fromIndex = Math.min(page * size, (int) totalElements);
        int toIndex = Math.min(fromIndex + size, (int) totalElements);
        List<HotelSearchResultDto> pageContent = filtered.subList(fromIndex, toIndex);

        return HotelSearchPageResponse.builder()
                .content(pageContent)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1 || totalPages == 0)
                .build();
    }

    @Override
    public HotelDetailDto getDetail(String hotelId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("checkIn date cannot be in the past");
        }
        HotelDetailDto detail = hotelProviderClient.getDetail(hotelId, checkIn, checkOut);
        if (detail == null) {
            throw new HotelNotFoundException(hotelId);
        }
        return detail; // unavailable rooms are included as-is, never filtered out (see PDF)
    }

    @Override
    public BookingResponseDto book(HotelBookingRequestDto request) {
        HotelDetailDto detail = getDetail(request.getHotelId(), request.getCheckIn(), request.getCheckOut());

        RoomTypeDto roomType = detail.getRoomTypes().stream()
                .filter(rt -> matchesRoomType(rt.getRoomTypeId(), request.getRoomTypeId()))
                .findFirst()
                .orElseThrow(() -> new RoomUnavailableException(request.getRoomTypeId()));

        if (!roomType.isAvailable()) {
            throw new RoomUnavailableException(request.getRoomTypeId());
        }

        long nights = Math.max(1, ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut()));
        BigDecimal amount = roomType.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        // 1. Create the internal Booking in PENDING first (never CONFIRMED before payment).
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .tripId(request.getTripId())
                .module(BookingModule.HOTEL)
                .status(BookingStatus.PENDING)
                .referenceNo(nextReferenceNo())
                .itemName(detail.getName() + " - " + roomType.getName())
                .startDate(request.getCheckIn())
                .endDate(request.getCheckOut())
                .amount(amount)
                .currency(roomType.getCurrency())
                .build();

        booking = bookingService.createPending(booking);

        // 2. Confirm with the hotel provider (hold the room / create the provider order).
        String effectiveRateKey = (request.getRoomTypeId() != null && request.getRoomTypeId().contains("@"))
                ? request.getRoomTypeId()
                : roomType.getRoomTypeId();

        ProviderBookingResult providerResult = hotelProviderClient.book(
                request.getHotelId(), effectiveRateKey, request.getCheckIn(), request.getCheckOut(),
                request.getAdults(), request.getChildren(), request.getGuestFullName(), request.getGuestEmail());

        // If the client's rateKey failed (e.g. stale), retry with the fresh rateKey from availability
        if (!providerResult.success() && !effectiveRateKey.equals(roomType.getRoomTypeId())) {
            providerResult = hotelProviderClient.book(
                    request.getHotelId(), roomType.getRoomTypeId(), request.getCheckIn(), request.getCheckOut(),
                    request.getAdults(), request.getChildren(), request.getGuestFullName(), request.getGuestEmail());
        }

        if (!providerResult.success()) {
            throw new RoomUnavailableException(request.getRoomTypeId());
        }

        // 3. Charge the traveler and flip PENDING -> CONFIRMED (only BookingService may do this).
        booking = bookingService.confirmWithPayment(booking);

        return BookingResponseDto.fromEntity(booking);
    }

    private boolean matchesRoomType(String availableRoomTypeId, String requestedRoomTypeId) {
        if (availableRoomTypeId == null || requestedRoomTypeId == null) {
            return false;
        }
        if (availableRoomTypeId.equals(requestedRoomTypeId)) {
            return true;
        }
        if (availableRoomTypeId.contains("@") && requestedRoomTypeId.contains("@")) {
            String availPrefix = availableRoomTypeId.substring(0, availableRoomTypeId.indexOf('@'));
            String reqPrefix = requestedRoomTypeId.substring(0, requestedRoomTypeId.indexOf('@'));
            if (availPrefix.equals(reqPrefix)) {
                return true;
            }
        }
        if (availableRoomTypeId.contains(requestedRoomTypeId)) {
            return true;
        }
        return false;
    }

    private String nextReferenceNo() {
        return "PMT-HTL-" + REF_SEQ.incrementAndGet();
    }

}
