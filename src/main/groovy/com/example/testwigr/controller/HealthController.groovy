package com.example.testwigr.controller

import com.example.testwigr.config.ApiVersionInterceptor
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.repository.PostRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Controller for health and status endpoints.
 * 
 * This controller provides endpoints for checking the health and status of the API.
 * These endpoints are useful for monitoring tools, health checks, and providing
 * transparency to API users about the current state of the system.
 * 
 * The health check endpoint performs basic checks on dependencies like the database
 * to ensure the API is fully operational.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "API health and status endpoints")
class HealthController {

    @Autowired(required = false)
    private BuildProperties buildProperties
    
    @Autowired
    private UserRepository userRepository
    
    @Autowired
    private PostRepository postRepository
    
    @Autowired
    private MongoTemplate mongoTemplate
    
    @Value('${spring.profiles.active:dev}')
    private String activeProfile
    
    /**
     * Simple health check endpoint.
     * Returns basic health status without detailed checks.
     * 
     * @return Simple health status response
     */
    @GetMapping("/ping")
    @Operation(
        summary = "Simple health check endpoint",
        description = "Performs a basic health check to confirm the API is running"
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "API is up and running",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = [
            status: "UP",
            timestamp: new Date().toInstant().toString()
        ]
        
        return ResponseEntity.ok(response)
    }
    
    /**
     * Comprehensive health check endpoint.
     * Performs detailed checks on dependencies like the database to ensure the API
     * is fully operational. Returns detailed health information about each component.
     * 
     * @return Detailed health status response
     */
    @GetMapping
    @Operation(
        summary = "Comprehensive health check",
        description = "Performs detailed health checks on all API components and dependencies"
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Health check completed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "One or more components are unhealthy",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Map<String, Object>> healthCheck() {
        boolean isHealthy = true
        Map<String, Object> dbStatus = checkDatabaseHealth()
        
        if (dbStatus.status != "UP") {
            isHealthy = false
        }
        
        Map<String, Object> response = [
            status: isHealthy ? "UP" : "DOWN",
            timestamp: new Date().toInstant().toString(),
            components: [
                database: dbStatus
            ]
        ]
        
        return isHealthy ? ResponseEntity.ok(response) : 
                          ResponseEntity.status(500).body(response)
    }
    
    /**
     * API information endpoint.
     * Provides detailed information about the API, including version, uptime,
     * environment, and resource counts.
     * 
     * @return API information response
     */
    @GetMapping("/info")
    @Operation(
        summary = "API information",
        description = "Provides detailed information about the API, its version, and resource counts"
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Information retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> response = [
            api: [
                name: "Testwigr API",
                version: buildProperties?.version ?: "1.0.0",
                description: "Twitter-like social media API",
                environment: activeProfile,
                buildTime: buildProperties?.time?.toString() ?: "unknown",
                uptime: formatUptime(ManagementFactory.getRuntimeMXBean().uptime)
            ],
            resources: [
                users: userRepository.count(),
                posts: postRepository.count()
            ],
            apiVersioning: [
                current: ApiVersionInterceptor.CURRENT_VERSION,
                supported: ApiVersionInterceptor.SUPPORTED_VERSIONS
            ]
        ]
        
        return ResponseEntity.ok(response)
    }
    
    /**
     * Checks the health of the database connection.
     * 
     * @return Database health status
     */
    private Map<String, Object> checkDatabaseHealth() {
        try {
            // Try to ping the database
            boolean isConnected = mongoTemplate.getDb().runCommand("ping").getBoolean("ok")
            
            if (isConnected) {
                return [
                    status: "UP",
                    details: [
                        databaseName: mongoTemplate.getDb().getName(),
                        userCount: userRepository.count(),
                        postCount: postRepository.count()
                    ]
                ]
            } else {
                return [
                    status: "DOWN",
                    details: "Database ping failed"
                ]
            }
        } catch (Exception e) {
            return [
                status: "DOWN",
                details: "Database connection error: ${e.message}"
            ]
        }
    }
    
    /**
     * Formats uptime in milliseconds to a human-readable string.
     * 
     * @param uptimeMillis Uptime in milliseconds
     * @return Formatted uptime string
     */
    private String formatUptime(long uptimeMillis) {
        long days = ChronoUnit.DAYS.between(
            Instant.now().minusMillis(uptimeMillis),
            Instant.now()
        )
        long hours = ChronoUnit.HOURS.between(
            Instant.now().minusMillis(uptimeMillis),
            Instant.now()
        ) % 24
        long minutes = ChronoUnit.MINUTES.between(
            Instant.now().minusMillis(uptimeMillis),
            Instant.now()
        ) % 60
        
        return "${days}d ${hours}h ${minutes}m"
    }
}
