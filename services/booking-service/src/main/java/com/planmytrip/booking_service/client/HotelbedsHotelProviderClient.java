package com.planmytrip.booking_service.client;

import com.planmytrip.booking_service.config.HotelbedsProperties;
import com.planmytrip.booking_service.dto.response.HotelDetailDto;
import com.planmytrip.booking_service.dto.response.HotelSearchResultDto;
import com.planmytrip.booking_service.dto.response.ProviderBookingResult;
import com.planmytrip.booking_service.dto.response.RoomTypeDto;
import com.planmytrip.booking_service.exception.HotelNotFoundException;
import com.planmytrip.booking_service.exception.HotelProviderUnavailableException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

    
/**
 * HotelProviderClient implementation backed by Hotelbeds (HBX Group API Suite)
 * (https://developer.hotelbeds.com).
 *
 * TEST MODE: sign up free at https://developer.hotelbeds.com,
 * API Key + Secret from "My API Keys", put them in hotelbeds.api-key /
 * hotelbeds.secret (application-dev.properties or env vars). Test keys hit
 * api.test.hotelbeds.com — free,real global + India
 * hotel content (1000+ properties in Delhi/Mumbai alone).
 *
 * Auth model: there's no OAuth token step. Every
 * request carries an Api-key header plus a rolling X-Signature header:
 *   X-Signature = SHA256(apiKey + secret + currentUnixTimestampSeconds)
 *
 * Two Hotelbeds APIs are combined here:
 *  - Content API  (contentBaseUrl): static hotel data — name, address,
 *    facilities, images, destination lookups.
 *  - Booking API  (bookingBaseUrl): live availability/rates and the
 *    actual booking confirmation call.
 *
 * Graceful degradation follows the same pattern as the old Duffel client:
 * on a provider failure we fall back to the last successful response for
 * the same search key, and only throw HotelProviderUnavailableException
 * if there is nothing cached either.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class HotelbedsHotelProviderClient implements HotelProviderClient {

    private final RestTemplate restTemplate;
    private final HotelbedsProperties hotelbedsProperties;

    /** destination|checkIn|checkOut|adults|children -> last good results */
    private final Map<String, List<HotelSearchResultDto>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, HotelDetailDto> detailCache = new ConcurrentHashMap<>();

    /** lower-cased city name -> Hotelbeds destination code (dynamically resolved at runtime) */
    private final Map<String, String> destinationCodeCache = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------
    // HotelProviderClient implementation
    // ---------------------------------------------------------------

    @Override
    @Retryable(retryFor = RestClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public List<HotelSearchResultDto> search(String destination, LocalDate checkIn, LocalDate checkOut,
                                              int adults, int children) {
        String cacheKey = cacheKey(destination, checkIn, checkOut, adults, children);
        try {
            String destinationCode = resolveDestinationCode(destination);

            Map<String, Object> body = new HashMap<>();
            body.put("stay", Map.of("checkIn", checkIn.toString(), "checkOut", checkOut.toString()));
            body.put("occupancies", List.of(buildOccupancy(adults, children)));
            body.put("destination", Map.of("code", destinationCode));
            body.put("from", 1);
            body.put("to", 1000);

            Map<?, ?> response = postToHotelbeds(hotelbedsProperties.getBookingBaseUrl() + "/hotels", body);
            List<HotelSearchResultDto> results = parseAvailabilityAsSearchResults(response);

            searchCache.put(cacheKey, results);
            return results;

        } catch (RestClientException ex) {
            log.error("Hotelbeds availability search failed for '{}', falling back to cache. Reason: {}",
                    destination, ex.getMessage(), ex);
            List<HotelSearchResultDto> cached = searchCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            throw new HotelProviderUnavailableException("Hotelbeds availability search unavailable: " + ex.getMessage(), ex);
        }
    }

    @Override
    public HotelDetailDto getDetail(String hotelId, LocalDate checkIn, LocalDate checkOut) {
        try {
            // 1. Static content (name, amenities, rating, description)
            Map<?, ?> contentResponse = getFromHotelbeds(
                    hotelbedsProperties.getContentBaseUrl() + "/hotels/" + hotelId + "/details?language=ENG");

            // 2. Live rooms/rates for this one hotel over the requested dates
            Map<String, Object> availabilityBody = new HashMap<>();
            availabilityBody.put("stay", Map.of("checkIn", checkIn.toString(), "checkOut", checkOut.toString()));
            availabilityBody.put("occupancies", List.of(buildOccupancy(2, 0)));
            availabilityBody.put("hotels", Map.of("hotel", List.of(Integer.parseInt(hotelId))));

            Map<?, ?> availabilityResponse = postToHotelbeds(
                    hotelbedsProperties.getBookingBaseUrl() + "/hotels", availabilityBody);

            HotelDetailDto detail = mergeIntoDetailDto(hotelId, contentResponse, availabilityResponse);
            detailCache.put(hotelId, detail);
            return detail;

        } catch (RestClientException ex) {
            log.warn("Hotelbeds detail lookup failed for hotelId={}, falling back to cache. Reason: {}",
                    hotelId, ex.getMessage());
            HotelDetailDto cached = detailCache.get(hotelId);
            if (cached != null) {
                return cached;
            }
            throw new HotelNotFoundException(hotelId);
        }
    }

    @Override
    public ProviderBookingResult book(String hotelId, String roomTypeId, LocalDate checkIn, LocalDate checkOut,
                                       int adults, int children, String guestFullName, String guestEmail) {
        try {
            // For Hotelbeds, roomTypeId is expected to be the rateKey returned
            // by the availability call (the rate you actually want to book) —
            // same convention the old Duffel client used with rate_id.
            Map<String, Object> holder = new HashMap<>();
            holder.put("name", firstName(guestFullName));
            holder.put("surname", lastName(guestFullName));

            List<Map<String, Object>> paxes = new ArrayList<>();
            int paxId = 1;
            for (int i = 0; i < adults; i++) {
                paxes.add(Map.of("roomId", 1, "type", "AD", "name", firstName(guestFullName),
                        "surname", lastName(guestFullName)));
                paxId++;
            }
            for (int i = 0; i < children; i++) {
                paxes.add(Map.of("roomId", 1, "type", "CH", "age", 8, "name", "Child", "surname", lastName(guestFullName)));
                paxId++;
            }

            Map<String, Object> room = new HashMap<>();
            room.put("rateKey", roomTypeId);
            room.put("paxes", paxes);

            Map<String, Object> body = new HashMap<>();
            body.put("holder", holder);
            body.put("rooms", List.of(room));
            body.put("clientReference", "PlanMyTrip-" + UUID.randomUUID());

            Map<?, ?> response = postToHotelbeds(hotelbedsProperties.getBookingBaseUrl() + "/bookings", body);
            Map<?, ?> booking = (Map<?, ?>) response.get("booking");
            String providerBookingId = booking != null ? String.valueOf(booking.get("reference")) : null;

            if (providerBookingId == null) {
                return new ProviderBookingResult(null, false, "PROVIDER_BOOKING_FAILED");
            }
            return new ProviderBookingResult(providerBookingId, true, null);

        } catch (RestClientException ex) {
            log.error("Hotelbeds booking failed for hotelId={}, rateKey={}: {}",
                    hotelId, roomTypeId, ex.getMessage());
            return new ProviderBookingResult(null, false, "PROVIDER_BOOKING_FAILED");
        }
    }

    // ---------------------------------------------------------------
    // Destination code resolution (city name -> Hotelbeds code)
    // ---------------------------------------------------------------

    /**
     * Hotelbeds destination codes are NOT always IATA airport codes
     * (they happen to match for DEL/BOM/BLR/JAI but not universally —
     * e.g. "GOI" collides with a Brazilian city in their system).
     * This resolves a free-text city name via the Locations API and
     * caches the result so we only hit the network once per city.
     */
    private String resolveDestinationCode(String destination) {
        String key = destination.trim().toLowerCase(Locale.ROOT);
        return destinationCodeCache.computeIfAbsent(key, k -> {
            String url = UriComponentsBuilder
                    .fromUriString(hotelbedsProperties.getContentBaseUrl() + "/locations/destinations")
                    .queryParam("fields", "all")
                    .queryParam("language", "ENG")
                    .queryParam("countryCodes", "IN")
                    .queryParam("from", 1)
                    .queryParam("to", 1000)
                    .toUriString();

            Map<?, ?> response = getFromHotelbeds(url);
            Object destinationsObj = response.get("destinations");
            if (!(destinationsObj instanceof List)) {
                throw new HotelProviderUnavailableException(
                        "Unexpected Hotelbeds locations response shape while resolving '" + destination + "'", null);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> destinations = (List<Map<String, Object>>) destinationsObj;

            for (Map<String, Object> dest : destinations) {
                String code = String.valueOf(dest.get("code"));
                String name = extractContent(dest.get("name"));
                if (code.equalsIgnoreCase(k) || (name != null && name.toLowerCase(Locale.ROOT).contains(k))) {
                    return code;
                }
            }
            throw new HotelProviderUnavailableException(
                    "No Hotelbeds destination code found for '" + destination + "'", null);
        });
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Object nameField) {
        if (nameField instanceof Map) {
            Object content = ((Map<String, Object>) nameField).get("content");
            return content != null ? String.valueOf(content) : null;
        }
        return nameField != null ? String.valueOf(nameField) : null;
    }

    // ---------------------------------------------------------------
    // Hotelbeds HTTP plumbing (signature auth, no OAuth token)
    // ---------------------------------------------------------------

    private Map<?, ?> postToHotelbeds(String url, Object body) {
        HttpEntity<Object> entity = new HttpEntity<>(body, hotelbedsHeaders());
        return restTemplate.exchange(url, HttpMethod.POST, entity, Map.class).getBody();
    }

    private Map<?, ?> getFromHotelbeds(String url) {
        HttpEntity<Void> entity = new HttpEntity<>(hotelbedsHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, Map.class).getBody();
    }

    private HttpHeaders hotelbedsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-key", hotelbedsProperties.getApiKey());
        headers.set("X-Signature", generateSignature());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    /** X-Signature = SHA256(apiKey + secret + currentUnixTimestampSeconds), hex-encoded. */
    private String generateSignature() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String raw = hotelbedsProperties.getApiKey() + hotelbedsProperties.getSecret() + timestamp;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }

    private Map<String, Object> buildOccupancy(int adults, int children) {
        Map<String, Object> occupancy = new HashMap<>();
        occupancy.put("rooms", 1);
        occupancy.put("adults", adults);
        occupancy.put("children", children);
        if (children > 0) {
            List<Map<String, Object>> paxes = new ArrayList<>();
            for (int i = 0; i < children; i++) {
                paxes.add(Map.of("type", "CH", "age", 8));
            }
            occupancy.put("paxes", paxes);
        }
        return occupancy;
    }

    // ---------------------------------------------------------------
    // Response mapping: Hotelbeds JSON -> PlanMyTrip's own DTOs
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<HotelSearchResultDto> parseAvailabilityAsSearchResults(Map<?, ?> response) {
        Object hotelsWrapperObj = response.get("hotels");
        if (!(hotelsWrapperObj instanceof Map)) return List.of();

        Object hotelsListObj = ((Map<String, Object>) hotelsWrapperObj).get("hotels");
        if (!(hotelsListObj instanceof List)) return List.of();

        List<Map<String, Object>> hotels = (List<Map<String, Object>>) hotelsListObj;
        return hotels.stream().map(this::toSearchResultDto).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private HotelSearchResultDto toSearchResultDto(Map<String, Object> hotel) {
        BigDecimal cheapest = cheapestRateForHotel(hotel);
        boolean anyFreeCancellation = hasFreeCancellation(hotel);

        return HotelSearchResultDto.builder()
                .hotelId(String.valueOf(hotel.get("code")))
                .name(String.valueOf(hotel.getOrDefault("name", "")))
                .location(String.valueOf(hotel.getOrDefault("destinationName", "")))
                .ratingScore(toDouble(hotel.get("categoryCode")))
                .reviewCount(0) // Hotelbeds content API has separate review data; wire up if needed
                .pricePerNight(cheapest)
                .currency("INR")
                .amenities(List.of())
                .freeCancellation(anyFreeCancellation)
                .build();
    }

    @SuppressWarnings("unchecked")
    private BigDecimal cheapestRateForHotel(Map<String, Object> hotel) {
        Object roomsObj = hotel.get("rooms");
        if (!(roomsObj instanceof List)) return BigDecimal.ZERO;

        BigDecimal min = null;
        for (Map<String, Object> room : (List<Map<String, Object>>) roomsObj) {
            Object ratesObj = room.get("rates");
            if (!(ratesObj instanceof List)) continue;
            for (Map<String, Object> rate : (List<Map<String, Object>>) ratesObj) {
                BigDecimal amount = toBigDecimal(rate.get("sellingRate") != null
                        ? rate.get("sellingRate") : rate.get("net"));
                if (min == null || amount.compareTo(min) < 0) {
                    min = amount;
                }
            }
        }
        return min != null ? min : BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    private boolean hasFreeCancellation(Map<String, Object> hotel) {
        Object roomsObj = hotel.get("rooms");
        if (!(roomsObj instanceof List)) return false;
        for (Map<String, Object> room : (List<Map<String, Object>>) roomsObj) {
            Object ratesObj = room.get("rates");
            if (!(ratesObj instanceof List)) continue;
            for (Map<String, Object> rate : (List<Map<String, Object>>) ratesObj) {
                Object policiesObj = rate.get("cancellationPolicies");
                if (policiesObj instanceof List && !((List<?>) policiesObj).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private HotelDetailDto mergeIntoDetailDto(String hotelId, Map<?, ?> contentResponse, Map<?, ?> availabilityResponse) {
        Object hotelObj = contentResponse != null ? contentResponse.get("hotel") : null;
        Map<String, Object> content = (hotelObj instanceof Map) ? (Map<String, Object>) hotelObj : Map.of();

        String name = extractContent(content.get("name"));
        String description = content.get("description") != null
                ? extractContent(((Map<String, Object>) content.get("description")))
                : null;

        List<String> amenities = new ArrayList<>();
        Object facilitiesObj = content.get("facilities");
        if (facilitiesObj instanceof List) {
            for (Map<String, Object> facility : (List<Map<String, Object>>) facilitiesObj) {
                String facilityName = extractContent(facility.get("description"));
                if (facilityName != null) amenities.add(facilityName);
            }
        }

        List<RoomTypeDto> roomTypes = extractRoomTypes(availabilityResponse);

        return HotelDetailDto.builder()
                .hotelId(hotelId)
                .name(name != null ? name : "")
                .location(String.valueOf(content.getOrDefault("destinationName", "")))
                .ratingScore(toDouble(content.get("categoryCode")))
                .reviewCount(0)
                .amenities(amenities)
                .roomTypes(roomTypes)
                .cancellationPolicy(description != null ? description : "See rate details for cancellation terms.")
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<RoomTypeDto> extractRoomTypes(Map<?, ?> availabilityResponse) {
        Object hotelsWrapperObj = availabilityResponse.get("hotels");
        if (!(hotelsWrapperObj instanceof Map)) return List.of();
        Object hotelsListObj = ((Map<String, Object>) hotelsWrapperObj).get("hotels");
        if (!(hotelsListObj instanceof List) || ((List<?>) hotelsListObj).isEmpty()) return List.of();

        Map<String, Object> hotel = (Map<String, Object>) ((List<?>) hotelsListObj).get(0);
        Object roomsObj = hotel.get("rooms");
        if (!(roomsObj instanceof List)) return List.of();

        List<RoomTypeDto> roomTypes = new ArrayList<>();
        for (Map<String, Object> room : (List<Map<String, Object>>) roomsObj) {
            Object ratesObj = room.get("rates");
            if (!(ratesObj instanceof List)) continue;
            for (Map<String, Object> rate : (List<Map<String, Object>>) ratesObj) {
                boolean refundable = !"NRF".equalsIgnoreCase(String.valueOf(rate.get("rateClass")));
                roomTypes.add(RoomTypeDto.builder()
                        .roomTypeId(String.valueOf(rate.get("rateKey"))) // used as roomTypeId for the book() call
                        .name(String.valueOf(room.getOrDefault("name", "Standard room")))
                        .sleeps(toInt(rate.getOrDefault("adults", 2)))
                        .pricePerNight(toBigDecimal(rate.get("sellingRate") != null ? rate.get("sellingRate") : rate.get("net")))
                        .currency("INR")
                        .available(true)
                        .refundable(refundable)
                        .freeCancellationUntil(extractCancellationDeadline(rate))
                        .build());
            }
        }
        return roomTypes;
    }

    @SuppressWarnings("unchecked")
    private String extractCancellationDeadline(Map<String, Object> rate) {
        Object policiesObj = rate.get("cancellationPolicies");
        if (policiesObj instanceof List && !((List<?>) policiesObj).isEmpty()) {
            Map<String, Object> firstPolicy = (Map<String, Object>) ((List<?>) policiesObj).get(0);
            Object from = firstPolicy.get("from");
            return from != null ? String.valueOf(from) : null;
        }
        return null;
    }

    // ---------------------------------------------------------------
    // small parsing helpers
    // ---------------------------------------------------------------

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int toInt(Object o) {
        if (o == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String firstName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts[0];
    }

    private String lastName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    private String cacheKey(String destination, LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        return destination.toLowerCase(Locale.ROOT) + "|" + checkIn + "|" + checkOut + "|" + adults + "|" + children;
    }
}



