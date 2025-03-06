package com.example.testwigr.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for OpenAPI 3.0 documentation (Swagger).
 * This class provides detailed API documentation for the Testwigr application,
 * including security schemes, server configurations, and API grouping.
 */
@Configuration
class SwaggerConfig {

    @Value('${spring.profiles.active:dev}')
    private String activeProfile

    /**
     * Creates the main OpenAPI specification for the Testwigr API.
     * Provides comprehensive information about the API including
     * version, description, contact info, license, security schemes, and servers.
     *
     * @return A fully configured OpenAPI object
     */
    @Bean
    OpenAPI openAPI() {
        // Create the main OpenAPI object with detailed API information
        return new OpenAPI()
            .info(new Info()
                .title("Testwigr API")
                .description("RESTful API for a Twitter-like social media platform, allowing users to share posts, follow other users, and interact through likes and comments.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Testwigr Team")
                    .email("support@testwigr.example.com")
                    .url("https://testwigr.example.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            // Add security requirement for JWT
            .addSecurityItem(new SecurityRequirement().addList("JWT"))
            // Define security components
            .components(new Components()
                .addSecuritySchemes("JWT", new SecurityScheme()
                    .name("JWT")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter JWT Bearer token in the format: Bearer {token}")))
            // Define servers for different environments
            .servers(getServers())
            // Define tags for organizing endpoints
            .tags(getTags())
    }

    /**
     * Creates an API group for authentication-related endpoints.
     * Groups endpoints related to user registration, login, and logout.
     *
     * @return GroupedOpenApi configuration for auth endpoints
     */
    @Bean
    GroupedOpenApi authenticationApi() {
        return GroupedOpenApi.builder()
                .group("authentication")
                .displayName("Authentication API")
                .pathsToMatch("/api/auth/**")
                .build()
    }

    /**
     * Creates an API group for user management endpoints.
     * Groups endpoints related to user profiles, following, and followers.
     *
     * @return GroupedOpenApi configuration for user endpoints
     */
    @Bean
    GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("users")
                .displayName("User Management API")
                .pathsToMatch("/api/users/**", "/api/follow/**")
                .build()
    }

    /**
     * Creates an API group for content management endpoints.
     * Groups endpoints related to posts, comments, and likes.
     *
     * @return GroupedOpenApi configuration for content endpoints
     */
    @Bean
    GroupedOpenApi contentApi() {
        return GroupedOpenApi.builder()
                .group("content")
                .displayName("Content Management API")
                .pathsToMatch("/api/posts/**", "/api/comments/**", "/api/likes/**")
                .build()
    }

    /**
     * Creates an API group for feed-related endpoints.
     * Groups endpoints related to user feeds and timelines.
     *
     * @return GroupedOpenApi configuration for feed endpoints
     */
    @Bean
    GroupedOpenApi feedApi() {
        return GroupedOpenApi.builder()
                .group("feeds")
                .displayName("Feed API")
                .pathsToMatch("/api/feed/**")
                .build()
    }

    /**
     * Generates a list of server configurations based on the active profile.
     * Provides server URLs for development, test, and production environments.
     *
     * @return A list of server configurations
     */
    private List<Server> getServers() {
        List<Server> servers = new ArrayList<>()
        
        // Development server
        Server devServer = new Server()
        devServer.setUrl("http://localhost:8080")
        devServer.setDescription("Development Server")
        
        // Test server
        Server testServer = new Server()
        testServer.setUrl("http://test.testwigr.example.com")
        testServer.setDescription("Test Server")
        
        // Production server
        Server prodServer = new Server()
        prodServer.setUrl("https://api.testwigr.example.com")
        prodServer.setDescription("Production Server")
        
        // Add servers based on active profile
        if (activeProfile == "prod") {
            servers.add(prodServer)
            servers.add(testServer)
            servers.add(devServer)
        } else if (activeProfile == "test") {
            servers.add(testServer)
            servers.add(devServer)
            servers.add(prodServer)
        } else {
            servers.add(devServer)
            servers.add(testServer)
            servers.add(prodServer)
        }
        
        return servers
    }

    /**
     * Creates a list of tags that are used to categorize API endpoints.
     * Each tag represents a logical group of related endpoints.
     *
     * @return A list of tags for API documentation
     */
    private List<Tag> getTags() {
        return Arrays.asList(
            new Tag().name("Authentication").description("Operations for user registration, login, and logout"),
            new Tag().name("Users").description("Operations for managing user profiles and accounts"),
            new Tag().name("Posts").description("Operations for creating, retrieving, updating, and deleting posts"),
            new Tag().name("Comments").description("Operations for managing comments on posts"),
            new Tag().name("Likes").description("Operations for liking and unliking posts"),
            new Tag().name("Follow").description("Operations for managing follow relationships between users"),
            new Tag().name("Feed").description("Operations for retrieving personalized content feeds")
        )
    }
}
