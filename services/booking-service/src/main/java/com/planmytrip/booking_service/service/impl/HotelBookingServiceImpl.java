package com.planmytrip.booking_service.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.planmytrip.booking_service.client.HotelProviderClient;
import com.planmytrip.booking_service.dto.response.HotelDetailDto;
import com.planmytrip.booking_service.dto.response.HotelSearchPageResponse;
import com.planmytrip.booking_service.dto.response.HotelSearchResultDto;
import com.planmytrip.booking_service.exception.HotelNotFoundException;
import com.planmytrip.booking_service.service.HotelBookingService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class HotelBookingServiceImpl implements HotelBookingService {
    
    private final HotelProviderClient hotelProviderClient;

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


}
