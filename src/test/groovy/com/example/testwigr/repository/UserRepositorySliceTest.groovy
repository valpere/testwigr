package com.example.testwigr.repository

import com.example.testwigr.model.User
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@DataMongoTest
@ActiveProfiles("test")
class UserRepositorySliceTest extends Specification {
    
    @Autowired
    UserRepository userRepository
    
    def cleanup() {
        userRepository.deleteAll()
    }
    
    def "should save and find user by username"() {
        given: "a user to save"
        def user = TestDataFactory.createUser(null, "testuser")
        
        when: "the user is saved"
        userRepository.save(user)
        
        and: "we try to find the user by username"
        def found = userRepository.findByUsername("testuser")
        
        then: "the user should be found"
        found.isPresent()
        found.get().username == "testuser"
        found.get().email == "testuser@example.com"
    }
}
