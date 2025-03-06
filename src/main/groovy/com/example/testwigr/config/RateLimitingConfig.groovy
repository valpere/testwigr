package com.example.testwigr.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.Refill
import java.time.Duration

/**
 * Configuration for API rate limiting.
 * 
 * This class configures rate limiting for the Testwigr API to prevent abuse and ensure
 * fair usage of resources. It uses the token bucket algorithm via the Bucket4j library
 * to implement rate limiting with different limits for authenticated and unauthenticated
 * requests.
 * 
 * Rate limits are enforced by an interceptor that checks each request against the
 * appropriate bucket based on the client's authentication status.
 */
@Configuration
class RateLimitingConfig implements WebMvcConfigurer {

    /**
     * Creates a rate limit bucket for authenticated users.
     * Authenticated users receive a higher rate limit since they are identified users.
     * 
     * @return A token bucket configured for authenticated user limits
     */
    @Bean
    Bucket authenticatedBucket() {
        // Allow 100 requests per minute for authenticated users
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)))
        return Bucket4j.builder()
                .addLimit(limit)
                .build()
    }
    
    /**
     * Creates a rate limit bucket for unauthenticated users.
     * Unauthenticated users receive a lower rate limit to prevent abuse.
     * 
     * @return A token bucket configured for unauthenticated user limits
     */
    @Bean
    Bucket unauthenticatedBucket() {
        // Allow 20 requests per minute for unauthenticated users
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)))
        return Bucket4j.builder()
                .addLimit(limit)
                .build()
    }
    
    /**
     * Adds the rate limiting interceptor to the interceptor registry.
     * This ensures all requests are checked against rate limits.
     * 
     * @param registry The interceptor registry to add to
     */
    @Override
    void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/api/**") // Apply to all API endpoints
                .excludePathPatterns("/api/health/**") // Exclude health checks
    }
    
    /**
     * Creates the rate limiting interceptor bean.
     * This interceptor checks each request against the appropriate rate limit bucket.
     * 
     * @return The configured rate limit interceptor
     */
    @Bean
    RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor(
                authenticatedBucket: authenticatedBucket(),
                unauthenticatedBucket: unauthenticatedBucket()
        )
    }
}
