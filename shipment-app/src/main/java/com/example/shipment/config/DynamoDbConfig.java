package com.example.shipment.config;

import com.example.shipment.target.DynamoDbTargetStore;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Bean
    @DependsOn("dynamoDbLocalServer")
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClient client = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://localhost:" + DynamoDbLocalServer.PORT))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("local", "local")))
            .build();
        createTable(client);
        return client;
    }

    private void createTable(DynamoDbClient client) {
        try {
            client.createTable(b -> b
                .tableName(DynamoDbTargetStore.TABLE)
                .keySchema(
                    k -> k.attributeName("shipmentId").keyType(KeyType.HASH),
                    k -> k.attributeName("orderId").keyType(KeyType.RANGE))
                .attributeDefinitions(
                    AttributeDefinition.builder().attributeName("shipmentId")
                        .attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName("orderId")
                        .attributeType(ScalarAttributeType.S).build())
                .billingMode(BillingMode.PAY_PER_REQUEST));
        } catch (ResourceInUseException alreadyThere) {
            // fine
        }
    }
}
