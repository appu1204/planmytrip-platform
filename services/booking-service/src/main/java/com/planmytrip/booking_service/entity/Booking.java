package com.planmytrip.booking_service.entity;

import com.planmytrip.booking_service.enums.BookingModule;
import com.planmytrip.booking_service.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String userId;

    private String tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    /**
     * Human-facing reference number,
     * e.g. PMT-HTL-8823041
     */
    @Column(nullable = false, unique = true)
    private String referenceNo;

    /**
     * Display name of the booked item,
     * e.g. "Kumarakom Lake Resort"
     */
    private String itemName;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    /**
     * Module-specific payload:
     * room type, guests, special requests, etc.
     */
    @Lob
    private String detailJson;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    private Instant cancelledAt;

    private BigDecimal refundAmount;
}