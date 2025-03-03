package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory

@TestConfiguration
@Profile('test')
class TestMongoConfig {

    @Bean
    static MongoDatabaseFactory mongoDatabaseFactory() {
        return new SimpleMongoClientDatabaseFactory('mongodb://localhost:27018/testdb')
    }

    @Bean
    static MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        return new MongoTemplate(factory)
    }

}
