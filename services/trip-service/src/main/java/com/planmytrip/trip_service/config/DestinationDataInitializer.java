package com.planmytrip.trip_service.config;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.planmytrip.trip_service.entity.Destination;
import com.planmytrip.trip_service.enums.TripType;
import com.planmytrip.trip_service.repository.DestinationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Automatically seeds the database with curated world-class destinations
 * when the service starts and the table is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DestinationDataInitializer implements CommandLineRunner {

    private final DestinationRepository destinationRepository;

    @Override
    public void run(String... args) {
        try {
            if (destinationRepository.count() == 0) {
                log.info("Destination table is empty. Seeding premier travel destinations...");
                List<Destination> seeds = List.of(
                    // Family
                    Destination.builder()
                        .name("Kerala")
                        .country("India")
                        .imageUrl("https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.FAMILY)
                        .popularityRank(1)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Singapore")
                        .country("Singapore")
                        .imageUrl("https://images.unsplash.com/photo-1525625293386-3f8f99389edd?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.FAMILY)
                        .popularityRank(2)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Maldives")
                        .country("Maldives")
                        .imageUrl("https://images.unsplash.com/photo-1514282401047-d79a71a590e8?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.FAMILY)
                        .popularityRank(3)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Interlaken")
                        .country("Switzerland")
                        .imageUrl("https://images.unsplash.com/photo-1530122037265-a5f1f91d3b99?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.FAMILY)
                        .popularityRank(4)
                        .active(true)
                        .build(),

                    // Couple
                    Destination.builder()
                        .name("Santorini")
                        .country("Greece")
                        .imageUrl("https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.COUPLE)
                        .popularityRank(1)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Paris")
                        .country("France")
                        .imageUrl("https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.COUPLE)
                        .popularityRank(2)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Amalfi Coast")
                        .country("Italy")
                        .imageUrl("https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.COUPLE)
                        .popularityRank(3)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Udaipur")
                        .country("India")
                        .imageUrl("https://images.unsplash.com/photo-1615836245337-f5b9b2303f10?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.COUPLE)
                        .popularityRank(4)
                        .active(true)
                        .build(),

                    // Solo
                    Destination.builder()
                        .name("Kyoto")
                        .country("Japan")
                        .imageUrl("https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.SOLO)
                        .popularityRank(1)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Bali")
                        .country("Indonesia")
                        .imageUrl("https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.SOLO)
                        .popularityRank(2)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Lisbon")
                        .country("Portugal")
                        .imageUrl("https://images.unsplash.com/photo-1508672019048-805b876b67e2?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.SOLO)
                        .popularityRank(3)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Hanoi & Ha Long")
                        .country("Vietnam")
                        .imageUrl("https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.SOLO)
                        .popularityRank(4)
                        .active(true)
                        .build(),

                    // Adventure
                    Destination.builder()
                        .name("Leh Ladakh")
                        .country("India")
                        .imageUrl("https://images.unsplash.com/photo-1581793745862-99fde7fa73d2?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.ADVENTURE)
                        .popularityRank(1)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Queenstown")
                        .country("New Zealand")
                        .imageUrl("https://images.unsplash.com/photo-1507699622108-4be3abd695ad?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.ADVENTURE)
                        .popularityRank(2)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Annapurna")
                        .country("Nepal")
                        .imageUrl("https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.ADVENTURE)
                        .popularityRank(3)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Rishikesh")
                        .country("India")
                        .imageUrl("https://images.unsplash.com/photo-1596761225579-2479427b3b3e?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.ADVENTURE)
                        .popularityRank(4)
                        .active(true)
                        .build(),

                    // Friends
                    Destination.builder()
                        .name("Goa")
                        .country("India")
                        .imageUrl("https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.FRIENDS)
                        .popularityRank(1)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Barcelona")
                        .country("Spain")
                        .imageUrl("https://images.unsplash.com/photo-1583422409516-2895a77efded?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.FRIENDS)
                        .popularityRank(2)
                        .active(true)
                        .build(),
                    Destination.builder()
                        .name("Dubai")
                        .country("UAE")
                        .imageUrl("https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=1200&q=80")
                        .persona(TripType.FRIENDS)
                        .popularityRank(3)
                        .active(true)
                        .build()
                );
                destinationRepository.saveAll(seeds);
                log.info("Successfully seeded {} curated destinations.", seeds.size());
            }
        } catch (Exception e) {
            log.warn("Auto-seeding destinations was skipped: {}", e.getMessage());
        }
    }
}
