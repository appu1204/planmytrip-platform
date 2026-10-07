package com.planmytrip.trip_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    public static final String CACHE_POPULAR_DESTINATIONS = "popularDestinations";
    public static final String CACHE_WEATHER_ADVISORIES = "weatherAdvisories";
    public static final String CACHE_TRIPS = "trips";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.java()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        // Popular destinations: static read-heavy catalog -> 24 hour TTL
        cacheConfigs.put(CACHE_POPULAR_DESTINATIONS, defaultConfig.entryTtl(Duration.ofHours(24)));
        // Weather advisories: external API response -> 30 minute TTL
        cacheConfigs.put(CACHE_WEATHER_ADVISORIES, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        // User trip details: frequent reads -> 15 minute TTL
        cacheConfigs.put(CACHE_TRIPS, defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
    public org.springframework.cache.CacheManager simpleCacheManager() {
        return new org.springframework.cache.concurrent.ConcurrentMapCacheManager(
                CACHE_POPULAR_DESTINATIONS,
                CACHE_WEATHER_ADVISORIES,
                CACHE_TRIPS
        );
    }
}
