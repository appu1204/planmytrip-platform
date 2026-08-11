package com.planmytrip.trip_service.entity;

import com.planmytrip.trip_service.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * No FK here on purpose — trip-service has its own database
     * (backlog: "Set up a separate database per service").
     * userId is just a reference to a user managed by user-service.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "trip_name", nullable = false, length = 150)
    private String tripName;

    @Column(name = "destination", nullable = false, length = 150)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false, length = 20)
    private TripType tripType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "adults", nullable = false)
    @Builder.Default
    private Integer adults = 1;

    @Column(name = "children", nullable = false)
    @Builder.Default
    private Integer children = 0;

    @Column(name = "budget", precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TripStatus status = TripStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
