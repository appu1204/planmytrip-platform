-- Initialize PlanMyTrip Microservices Databases
CREATE DATABASE IF NOT EXISTS planmytrip_user_db;
CREATE DATABASE IF NOT EXISTS planmytrip_trip_db;
CREATE DATABASE IF NOT EXISTS planmytrip_itinerary_db;

USE planmytrip_trip_db;

CREATE TABLE IF NOT EXISTS destinations (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    persona VARCHAR(20) NOT NULL,
    popularity_rank INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO destinations (id, name, country, image_url, persona, popularity_rank, active) VALUES
('b1010000-0000-0000-0000-000000000001', 'Kerala', 'India', 'https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1200&q=80', 'FAMILY', 1, 1),
('b1010000-0000-0000-0000-000000000002', 'Singapore', 'Singapore', 'https://images.unsplash.com/photo-1525625293386-3f8f99389edd?auto=format&fit=crop&w=1200&q=80', 'FAMILY', 2, 1),
('b1010000-0000-0000-0000-000000000003', 'Maldives', 'Maldives', 'https://images.unsplash.com/photo-1514282401047-d79a71a590e8?auto=format&fit=crop&w=1200&q=80', 'FAMILY', 3, 1),
('b1010000-0000-0000-0000-000000000004', 'Interlaken', 'Switzerland', 'https://images.unsplash.com/photo-1530122037265-a5f1f91d3b99?auto=format&fit=crop&w=1200&q=80', 'FAMILY', 4, 1),
('b1010000-0000-0000-0000-000000000005', 'Santorini', 'Greece', 'https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?auto=format&fit=crop&w=1200&q=80', 'COUPLE', 1, 1),
('b1010000-0000-0000-0000-000000000006', 'Paris', 'France', 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=80', 'COUPLE', 2, 1),
('b1010000-0000-0000-0000-000000000007', 'Amalfi Coast', 'Italy', 'https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1200&q=80', 'COUPLE', 3, 1),
('b1010000-0000-0000-0000-000000000008', 'Udaipur', 'India', 'https://images.unsplash.com/photo-1615836245337-f5b9b2303f10?auto=format&fit=crop&w=1200&q=80', 'COUPLE', 4, 1),
('b1010000-0000-0000-0000-000000000009', 'Kyoto', 'Japan', 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=1200&q=80', 'SOLO', 1, 1),
('b1010000-0000-0000-0000-000000000010', 'Bali', 'Indonesia', 'https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1200&q=80', 'SOLO', 2, 1),
('b1010000-0000-0000-0000-000000000011', 'Lisbon', 'Portugal', 'https://images.unsplash.com/photo-1508672019048-805b876b67e2?auto=format&fit=crop&w=1200&q=80', 'SOLO', 3, 1),
('b1010000-0000-0000-0000-000000000012', 'Leh Ladakh', 'India', 'https://images.unsplash.com/photo-1581793745862-99fde7fa73d2?auto=format&fit=crop&w=1200&q=80', 'ADVENTURE', 1, 1),
('b1010000-0000-0000-0000-000000000013', 'Queenstown', 'New Zealand', 'https://images.unsplash.com/photo-1507699622108-4be3abd695ad?auto=format&fit=crop&w=1200&q=80', 'ADVENTURE', 2, 1),
('b1010000-0000-0000-0000-000000000014', 'Annapurna', 'Nepal', 'https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&w=1200&q=80', 'ADVENTURE', 3, 1),
('b1010000-0000-0000-0000-000000000015', 'Goa', 'India', 'https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1200&q=80', 'FRIENDS', 1, 1),
('b1010000-0000-0000-0000-000000000016', 'Barcelona', 'Spain', 'https://images.unsplash.com/photo-1583422409516-2895a77efded?auto=format&fit=crop&w=1200&q=80', 'FRIENDS', 2, 1);
