package com.planmytrip.ai_itinerary_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.planmytrip.ai_itinerary_service.entity.Itinerary;

public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    Optional<Itinerary> findByIdAndUserId(UUID id, Long userId);

}
