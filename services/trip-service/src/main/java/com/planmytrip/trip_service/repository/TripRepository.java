package com.planmytrip.trip_service.repository;

import com.planmytrip.trip_service.entity.Trip;
import com.planmytrip.trip_service.enums.TripStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    // Phase 3 will add: List<Trip> findByUserId(Long userId); -> but earlier added

    Page<Trip> findByUserId(UUID userId, Pageable pageable);
    Page<Trip> findByUserIdAndStatus(UUID userId, TripStatus status, Pageable pageable);

    
}
