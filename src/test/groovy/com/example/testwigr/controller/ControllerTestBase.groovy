package com.example.testwigr.controller

import com.example.testwigr.config.SimpleSecurityTestConfig
import com.example.testwigr.test.TestSecurityUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import spock.lang.Specification

/**
 * Base class for controller integration tests that provides common functionality
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SimpleSecurityTestConfig)
abstract class ControllerTestBase extends Specification {

    @Autowired
    protected MockMvc mockMvc

    @Autowired
    protected ObjectMapper objectMapper

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    protected String jwtSecret

    /**
     * Helper method to add authentication token to a request
     */
    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request, String username = "testuser") {
        String token = TestSecurityUtils.generateTestToken(username, jwtSecret)
        return request.header("Authorization", "Bearer ${token}")
    }

    /**
     * Helper method to convert an object to JSON string
     */
    protected String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj)
    }

    /**
     * Helper method to create a JSON request
     */
    protected MockHttpServletRequestBuilder jsonRequest(MockHttpServletRequestBuilder request, Object body = null) {
        def result = request.contentType(MediaType.APPLICATION_JSON)
        if (body) {
            result = result.content(toJson(body))
        }
        return result
    }

}

