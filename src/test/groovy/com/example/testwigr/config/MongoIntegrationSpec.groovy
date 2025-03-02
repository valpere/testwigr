package com.example.testwigr.config

import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@DataMongoTest
@ActiveProfiles('test')
@ContextConfiguration(initializers = MongoTestInitializer.class)
abstract class MongoIntegrationSpec extends Specification {

// Base class for MongoDB integration tests
}

class MongoTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
            'spring.data.mongodb.uri=mongodb://localhost:27018/testdb',
            'spring.data.mongodb.database=testdb'
        ).applyTo(context)
    }
}
