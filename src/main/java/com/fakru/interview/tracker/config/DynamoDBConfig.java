package com.fakru.interview.tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
public class DynamoDBConfig {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000"))  // DynamoDB Local URI
                .region(Region.AP_SOUTH_1)  // Any region, as DynamoDB Local does not require a region
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("fakeMyKeyId", "fakeSecretAccessKey")
                ))
                .build();
    }
}