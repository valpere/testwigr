package com.example.testwigr.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                'userProfile',
                'userById',
                'postCount',
                'personalFeed',
                'userFeed',
                'discoveryFeed'
        )
    }
}
