package com.planmytrip.trip_service.config;

import com.planmytrip.trip_service.dto.response.DestinationResponse;
import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.dto.response.WeatherAdvisoryResponse;
import com.planmytrip.trip_service.enums.TripStatus;
import com.planmytrip.trip_service.enums.TripType;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RedisCacheSerializationTest {

    @Test
    void testTripResponseSerialization() {
        RedisSerializer<Object> serializer = RedisSerializer.java();

        TripResponse trip = TripResponse.builder()
                .id(UUID.randomUUID())
                .userId(1L)
                .tripName("Goa Beach Trip")
                .destination("Goa")
                .tripType(TripType.FRIENDS)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 10))
                .adults(2)
                .children(0)
                .budget(BigDecimal.valueOf(50000))
                .currency("INR")
                .status(TripStatus.DRAFT)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        byte[] serialized = serializer.serialize(trip);
        assertNotNull(serialized);

        Object deserialized = serializer.deserialize(serialized);
        assertNotNull(deserialized);
        assertInstanceOf(TripResponse.class, deserialized);
        TripResponse result = (TripResponse) deserialized;
        assertEquals(trip.getId(), result.getId());
        assertEquals("Goa Beach Trip", result.getTripName());
        assertEquals(trip.getStartDate(), result.getStartDate());
    }

    @Test
    void testDestinationResponseSerialization() {
        RedisSerializer<Object> serializer = RedisSerializer.java();

        List<DestinationResponse> destinations = List.of(
                DestinationResponse.builder()
                        .id(UUID.randomUUID())
                        .name("Goa")
                        .country("India")
                        .imageUrl("https://example.com/goa.jpg")
                        .persona(TripType.FRIENDS)
                        .popularityRank(1)
                        .build()
        );

        byte[] serialized = serializer.serialize(destinations);
        assertNotNull(serialized);

        Object deserialized = serializer.deserialize(serialized);
        assertNotNull(deserialized);
    }

    @Test
    void testWeatherAdvisoryResponseSerialization() {
        RedisSerializer<Object> serializer = RedisSerializer.java();

        WeatherAdvisoryResponse advisory = WeatherAdvisoryResponse.builder()
                .destination("Goa")
                .status("SAFE")
                .title("Safe to travel")
                .professionalVerdict("All good")
                .recommendation("Travel freely")
                .safetyScore(90)
                .temperature(28.0)
                .windSpeedKmh(10.0)
                .precipitationMm(0.0)
                .isHillyTerrain(false)
                .hazardReasons(List.of())
                .build();

        byte[] serialized = serializer.serialize(advisory);
        assertNotNull(serialized);

        Object deserialized = serializer.deserialize(serialized);
        assertNotNull(deserialized);
        assertInstanceOf(WeatherAdvisoryResponse.class, deserialized);
    }
}
