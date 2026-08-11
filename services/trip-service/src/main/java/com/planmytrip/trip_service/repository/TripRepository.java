package com.planmytrip.trip_service.repository;

import com.planmytrip.trip_service.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    // Phase 3 will add: List<Trip> findByUserId(Long userId);
}
