package com.planmytrip.trip_service.entity;

import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.planmytrip.trip_service.enums.TripType;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A curated destination card shown on the home page
 * ("Popular Destinations based on persona"). Content is admin/seed-managed,
 * not user-generated — there is no create/update API for this yet.
 */

@Entity
@Table (name = "Destinations")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Destination {

    @Id
    @GeneratedValue
    @Column (name = "user_id", updatable = false, nullable = false)
    private UUID id;

    @Column (name = "name", nullable = false, length = 100)
    private String name;

    @Column (name = "country", nullable = false, length = 100)
    private String country;

    /**
     * Plain URL string on purpose — Storage Can be Cloudinary or s3 based on the need.
     */
    
    @Column (name = "image_url", nullable = false, length = 100)
    private String imageUrl;


    /**
     * used persona based preferences for better user experince (Solo/Couple/Friends/Family).
     */

    @Enumerated(EnumType.STRING)
    @Column (name = "persona", nullable = false, length = 20)
    private TripType persona;

    @Column(name = "popularity_rank", nullable = false)
    @Builder.Default
    private Integer popularityRank = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column (name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column (name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
