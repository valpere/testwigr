package com.example.testwigr.repository

import com.example.testwigr.config.MongoIntegrationSpec
import com.example.testwigr.model.User
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired

class UserRepositoryIntegrationSpec extends MongoIntegrationSpec {
    
    @Autowired
    UserRepository userRepository
    
    def cleanup() {
        userRepository.deleteAll()
    }
    
    def "should save and find users by username"() {
        given:
        def user = TestDataFactory.createUser(null, "uniqueusername")
        
        when:
        userRepository.save(user)
        def foundUser = userRepository.findByUsername("uniqueusername")
        
        then:
        foundUser.isPresent()
        foundUser.get().username == "uniqueusername"
        foundUser.get().email == "uniqueusername@example.com"
    }
    
    def "should check if username exists"() {
        given:
        def user = TestDataFactory.createUser(null, "existinguser")
        userRepository.save(user)
        
        when:
        def exists = userRepository.existsByUsername("existinguser")
        def doesNotExist = userRepository.existsByUsername("nonexistentuser")
        
        then:
        exists
        !doesNotExist
    }
    
    def "should check if email exists"() {
        given:
        def user = TestDataFactory.createUser(null, "emailuser")
        userRepository.save(user)
        
        when:
        def exists = userRepository.existsByEmail("emailuser@example.com")
        def doesNotExist = userRepository.existsByEmail("nonexistent@example.com")
        
        then:
        exists
        !doesNotExist
    }
}
