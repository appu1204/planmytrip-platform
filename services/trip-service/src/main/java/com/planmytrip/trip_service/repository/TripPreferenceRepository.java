package com.planmytrip.trip_service.repository;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.planmytrip.trip_service.entity.TripPreference;


@Repository
public interface TripPreferenceRepository extends JpaRepository<TripPreference, UUID> {

    List<TripPreference> findByTripId(UUID tripId);

    @Modifying
    @Query("DELETE FROM TripPreference tp WHERE tp.tripId = :tripId")
    void deleteAllByTripId(UUID tripId);
}

