package com.example.shipment.config;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs DynamoDB Local in-process so the example needs no infrastructure.
 * In production this bean simply would not exist; nothing else in the
 * application knows the difference.
 */
@Component
public class DynamoDbLocalServer {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbLocalServer.class);
    static final int PORT = 8000;

    private DynamoDBProxyServer server;

    @PostConstruct
    public void start() throws Exception {
        server = ServerRunner.createServerFromCommandLineArgs(
            new String[]{"-inMemory", "-port", String.valueOf(PORT)});
        server.start();
        log.info("DynamoDB Local listening on {}", PORT);
    }

    @PreDestroy
    public void stop() throws Exception {
        if (server != null) server.stop();
    }
}
