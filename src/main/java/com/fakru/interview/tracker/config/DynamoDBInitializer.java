package com.fakru.interview.tracker.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Component
public class DynamoDBInitializer {

    private final DynamoDbClient dynamoDbClient;

    public DynamoDBInitializer(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @PostConstruct
    public void createTables() {
        createUserTable();
        createJobTable();
    }

    private void createUserTable() {
        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName("user")
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("pk")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("pk")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("email")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName("email-index")
                            .keySchema(KeySchemaElement.builder()
                                    .attributeName("email")
                                    .keyType(KeyType.HASH)
                                    .build())
                            .projection(Projection.builder()
                                    .projectionType(ProjectionType.ALL)
                                    .build())
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            System.out.println("User table created successfully.");
        } catch (ResourceInUseException e) {
            System.out.println("User table already exists.");
        } catch (DynamoDbException e) {
            System.err.println("Error creating User table: " + e.getMessage());
        }
    }

    private void createJobTable() {
        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName("job")
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("pk")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("pk")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("sk")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("user_id")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                    .indexName("user_id-index")
                                    .keySchema(KeySchemaElement.builder()
                                            .attributeName("user_id")
                                            .keyType(KeyType.HASH)
                                            .build())
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL)
                                            .build())
                                    .build(),
                            GlobalSecondaryIndex.builder()
                                    .indexName("pk-sk-index")
                                    .keySchema(
                                            KeySchemaElement.builder()
                                                    .attributeName("pk")
                                                    .keyType(KeyType.HASH)
                                                    .build(),
                                            KeySchemaElement.builder()
                                                    .attributeName("sk")
                                                    .keyType(KeyType.RANGE)
                                                    .build())
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL)
                                            .build())
                                    .build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            System.out.println("Job table created successfully.");
        } catch (ResourceInUseException e) {
            System.out.println("Job table already exists.");
        } catch (DynamoDbException e) {
            System.err.println("Error creating Job table: " + e.getMessage());
        }
    }
}