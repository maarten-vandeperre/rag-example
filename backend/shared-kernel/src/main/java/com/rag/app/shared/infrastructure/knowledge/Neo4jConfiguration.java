package com.rag.app.shared.infrastructure.knowledge;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class Neo4jConfiguration implements AutoCloseable {
    private final String uri;
    private final String username;
    private final String password;
    private final String database;
    private final Duration maxConnectionLifetime;
    private final int maxConnectionPoolSize;
    private final Duration connectionAcquisitionTimeout;
    private Driver driver;

    public Neo4jConfiguration(String uri,
                              String username,
                              String password,
                              String database,
                              Duration maxConnectionLifetime,
                              int maxConnectionPoolSize,
                              Duration connectionAcquisitionTimeout) {
        this.uri = requireText(uri, "uri");
        this.username = requireText(username, "username");
        this.password = Objects.requireNonNull(password, "password cannot be null");
        this.database = requireText(database, "database");
        this.maxConnectionLifetime = Objects.requireNonNull(maxConnectionLifetime, "maxConnectionLifetime cannot be null");
        this.connectionAcquisitionTimeout = Objects.requireNonNull(connectionAcquisitionTimeout, "connectionAcquisitionTimeout cannot be null");
        if (maxConnectionPoolSize <= 0) {
            throw new IllegalArgumentException("maxConnectionPoolSize must be positive");
        }
        this.maxConnectionPoolSize = maxConnectionPoolSize;
    }

    public static Neo4jConfiguration defaults() {
        return new Neo4jConfiguration(
            "bolt://localhost:7687",
            "neo4j",
            "password",
            "neo4j",
            Duration.ofMinutes(30),
            50,
            Duration.ofMinutes(2)
        );
    }

    public Driver createDriver() {
        if (driver == null) {
            driver = GraphDatabase.driver(
                uri,
                AuthTokens.basic(username, password),
                Config.builder()
                    .withMaxConnectionLifetime(maxConnectionLifetime.toMillis(), TimeUnit.MILLISECONDS)
                    .withMaxConnectionPoolSize(maxConnectionPoolSize)
                    .withConnectionAcquisitionTimeout(connectionAcquisitionTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .build()
            );
        }
        return driver;
    }

    public SessionConfig sessionConfig() {
        return SessionConfig.builder().withDatabase(database).build();
    }

    public String database() {
        return database;
    }

    @Override
    public void close() {
        if (driver != null) {
            driver.close();
            driver = null;
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
