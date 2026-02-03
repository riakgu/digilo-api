package com.riakgu.digilo.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CATEGORIES_CACHE = "categories";
    public static final String CATEGORY_BY_SLUG_CACHE = "categoryBySlug";
    public static final String PRODUCTS_CACHE = "products";
    public static final String PRODUCT_BY_SLUG_CACHE = "productBySlug";
    public static final String DASHBOARD_STATS_CACHE = "dashboardStats";
    public static final String DASHBOARD_TOP_USERS_CACHE = "dashboardTopUsers";
    public static final String DASHBOARD_TOP_PRODUCTS_CACHE = "dashboardTopProducts";
    public static final String DASHBOARD_RECENT_ORDERS_CACHE = "dashboardRecentOrders";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Categories - 5 minutes TTL
        cacheConfigurations.put(CATEGORIES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CATEGORY_BY_SLUG_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Products - 2 minutes TTL (more dynamic)
        cacheConfigurations.put(PRODUCTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put(PRODUCT_BY_SLUG_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(2)));
        
        // Dashboard stats - 1 minute TTL
        cacheConfigurations.put(DASHBOARD_STATS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put(DASHBOARD_TOP_USERS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(DASHBOARD_TOP_PRODUCTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(DASHBOARD_RECENT_ORDERS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
