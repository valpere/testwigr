package com.example.testwigr.controller

import io.swagger.v3.oas.annotations.Hidden
import org.springdoc.core.properties.SpringDocConfigProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

/**
 * Controller for downloading the OpenAPI specification.
 * 
 * This controller provides endpoints for downloading the OpenAPI specification
 * in JSON or YAML format. This is useful for developers who want to generate
 * client code or documentation from the specification, or who want to use the
 * specification with tools other than Swagger UI.
 * 
 * The controller is hidden from the Swagger UI to avoid cluttering the API
 * documentation with endpoints that are related to the documentation itself.
 */
@RestController
@RequestMapping("/api/docs")
@Hidden
@Profile("!test") // Exclude from test profile
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
class OpenApiController {

    // Use simple String property instead of @Value with template
    private String apiDocsPath = "/v3/api-docs"
    
    private final RestTemplate restTemplate = new RestTemplate()
    
    /**
     * Downloads the OpenAPI specification in JSON format.
     * This endpoint fetches the specification from the springdoc endpoint and returns
     * it with appropriate headers for downloading as a file.
     * 
     * @return ResponseEntity containing the OpenAPI JSON specification
     */
    @GetMapping("/openapi.json")
    ResponseEntity<String> downloadOpenApiJson() {
        // Get the OpenAPI JSON from the springdoc endpoint
        String apiDocsUrl = "http://localhost:8080" + apiDocsPath
        String openApiJson = restTemplate.getForObject(apiDocsUrl, String.class)
        
        // Set headers for file download
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.set("Content-Disposition", "attachment; filename=testwigr-openapi.json")
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(openApiJson)
    }
    
    /**
     * Downloads the OpenAPI specification in YAML format.
     * This endpoint fetches the specification from the springdoc endpoint in YAML format
     * and returns it with appropriate headers for downloading as a file.
     * 
     * @return ResponseEntity containing the OpenAPI YAML specification
     */
    @GetMapping("/openapi.yaml")
    ResponseEntity<String> downloadOpenApiYaml() {
        // Get the OpenAPI YAML from the springdoc endpoint
        String apiDocsUrl = "http://localhost:8080" + apiDocsPath + ".yaml"
        String openApiYaml = restTemplate.getForObject(apiDocsUrl, String.class)
        
        // Set headers for file download
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(new MediaType("application", "yaml"))
        headers.set("Content-Disposition", "attachment; filename=testwigr-openapi.yaml")
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(openApiYaml)
    }
}
