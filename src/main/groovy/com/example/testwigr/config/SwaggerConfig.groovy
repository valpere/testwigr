package com.example.testwigr.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    
    @Bean
    static OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Twitter-Like API")
                .description("API for a Twitter-like social media application")
                .version("1.0.0"))
            .addSecurityItem(new SecurityRequirement().addList("JWT"))
            .components(new Components()
                .addSecuritySchemes("JWT", new SecurityScheme()
                    .name("JWT")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
    }
}
