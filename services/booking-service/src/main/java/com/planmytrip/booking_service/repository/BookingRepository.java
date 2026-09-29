package com.planmytrip.booking_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.planmytrip.booking_service.entity.Booking;
import com.planmytrip.booking_service.enums.BookingModule;

/**
 * The only repository needed — every module (Hotel/Flight/Transport/Activity)
 * shares the one `bookings` table, discriminated by `module`.
 */
public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    List<Booking> findByUserIdAndModule(String userId, BookingModule module);

    List<Booking> findByUserIdAndTripId(String userId, String tripId);

    List<Booking> findByUserIdAndModuleAndTripId(String userId, BookingModule module, String tripId);

    Optional<Booking> findByReferenceNo(String referenceNo);
}
