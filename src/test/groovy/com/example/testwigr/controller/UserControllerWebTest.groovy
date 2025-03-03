package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.service.UserService
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.when

@WebMvcTest(controllers = [UserController.class])
@ActiveProfiles('test')
class UserControllerWebTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @MockBean
    UserService userService

    @MockBean
    AuthenticationManager authenticationManager

    @WithMockUser(username = 'testuser')
    def "should get user by username"() {
        given:
        def username = 'testuser'
        def user = TestDataFactory.createUser('123', username)

        when(userService.getUserByUsername(anyString())).thenReturn(user)

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/users/${username}")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )

        then:
        result.andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath('$.username').value(username))
    }

    @WithMockUser(username = 'testuser')
    def "should get current user"() {
        given:
        def username = 'testuser'
        def user = TestDataFactory.createUser('123', username)

        when(userService.getUserByUsername(anyString())).thenReturn(user)

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get('/api/users/me')
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )

        then:
        result.andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath('$.username').value(username))
    }

}
