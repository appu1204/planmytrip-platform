package com.planmytrip.trip_service.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.planmytrip.trip_service.enums.TripPreferenceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trip_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "preference"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TripPreference {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tripId", nullable = false)
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference", nullable = false, length = 30)
    private TripPreferenceType preference;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

}
