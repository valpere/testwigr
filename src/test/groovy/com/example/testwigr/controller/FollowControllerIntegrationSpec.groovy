package com.example.testwigr.controller

import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FollowControllerIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    def setup() {
        userRepository.deleteAll()
        
        // Create test users
        def follower = TestDataFactory.createUser(null, "follower")
        def following = TestDataFactory.createUser(null, "following")
        
        userRepository.save(follower)
        userRepository.save(following)
    }

    @WithMockUser(username = "follower")
    def "should follow and unfollow a user"() {
        given:
        def following = userRepository.findByUsername("following").get()

        when: "User follows another user"
        def followResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/follow/${following.id}")
        )

        then: "Follow operation succeeds"
        followResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
            .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(true))
        
        when: "Get follow status"
        def statusResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/follow/${following.id}/status")
        )
        
        then: "User is shown as following"
        statusResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(true))
        
        when: "User unfollows the other user"
        def unfollowResult = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/follow/${following.id}")
        )
        
        then: "Unfollow operation succeeds"
        unfollowResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
            .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(false))
        
        when: "Get follow status after unfollowing"
        def finalStatusResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/follow/${following.id}/status")
        )
        
        then: "User is shown as not following"
        finalStatusResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(false))
    }

    @WithMockUser(username = "follower")
    def "should get followers and following lists"() {
        given:
        def follower = userRepository.findByUsername("follower").get()
        def following = userRepository.findByUsername("following").get()
        
        // Set up follow relationship
        follower.following.add(following.id)
        following.followers.add(follower.id)
        userRepository.save(follower)
        userRepository.save(following)

        when: "Get list of users that follower is following"
        def followingResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/follow/following")
        )

        then: "List contains the followed user"
        followingResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$[0].id').value(following.id))
            .andExpect(MockMvcResultMatchers.jsonPath('$[0].username').value("following"))
        
        when: "Get followers of 'following' user"
        // Need to authenticate as 'following' to see followers
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout"))
        
        and: "Switch to 'following' user"
        def followersResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/users/following")
                .header("Authorization", "Bearer " + getAuthToken("following", "password123"))
        )
        
        then: "Correct followers are returned"
        followersResult.andExpect(MockMvcResultMatchers.status().isOk())
    }
    
    private String getAuthToken(String username, String password) {
        def loginRequest = [username: username, password: password]
        
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn()
        
        def response = objectMapper.readValue(result.response.contentAsString, Map)
        return response.token
    }
}
