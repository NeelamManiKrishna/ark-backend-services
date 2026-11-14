package com.app.ark_backend_services;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Base class for integration tests. One shared throwaway MongoDB container per JVM,
 * wired into Spring via @ServiceConnection (Docker must be running).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractMongoIntegrationTest {

    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    static {
        MONGO.start();
    }
}
