package com.fakru.interview.tracker.repository;

import com.fakru.interview.tracker.annotation.LogExecutionTime;
import com.fakru.interview.tracker.dynamodata.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class UserRepository {

    private static final String TABLE_NAME = "user";
    private static final String PK_COLUMN = "pk";
    private static final String GSI_EMAIL_COLUMN = "email";
    private final DynamoDbClient dynamoDbClient;

    public UserRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @LogExecutionTime
    public void saveUser(String uuid, User user) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put(PK_COLUMN, AttributeValue.builder().s(uuid).build());
            item.put(GSI_EMAIL_COLUMN, AttributeValue.builder().s(user.getEmail()).build());

            item.put("is_email_validated", AttributeValue.builder().bool(user.isEmailValidated()).build());
            item.put("created_at", AttributeValue.builder().n(String.valueOf(user.getCreatedAt().getTime())).build());
            item.put("updated_at", AttributeValue.builder().n(String.valueOf(user.getUpdatedAt().getTime())).build());
            item.put("otp_expiry_time", AttributeValue.builder().n(String.valueOf(user.getOtpExpiryTime().getTime())).build());

            if (StringUtils.isEmpty(user.getName())) {
                item.put("name", AttributeValue.builder().s(user.getName()).build());
            }
            if (StringUtils.isEmpty(user.getPhoneNumber())) {
                item.put("phone_number", AttributeValue.builder().s(String.valueOf(user.getPhoneNumber())).build());
            }
            if (StringUtils.isEmpty(user.getPasswordHash())) {
                item.put("password_hash", AttributeValue.builder().s(user.getPasswordHash()).build());
            }
            if (StringUtils.isEmpty(user.getOtpHash())) {
                item.put("otp_hash", AttributeValue.builder().s(user.getOtpHash()).build());
            }

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(request);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error saving user: " + e.getMessage(), e);
        }
    }

    public Map<String, AttributeValue> getUser(String uuid) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of(PK_COLUMN, AttributeValue.builder().s(uuid).build()))
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);
            if (response.hasItem()) {
                return response.item();
            } else {
                throw new RuntimeException("User not found");
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error retrieving user: " + e.getMessage(), e);
        }
    }

    public Map<String, AttributeValue> findByEmail(String email) {
        Map<String, AttributeValue> eav = Map.of(":email", AttributeValue.builder().s(email).build());

        // TODO: change to GetItemRequest
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName("email-index")
                .keyConditionExpression("email = :email")
                .expressionAttributeValues(eav)
                .build();

        QueryResponse response = dynamoDbClient.query(queryRequest);
        if (!response.items().isEmpty()) {
            return response.items().get(0);
        } else {
            throw new RuntimeException("User with email " + email + " not found");
        }
    }

    public void updateUser(String uuid, Map<String, AttributeValue> updatedFields) {
        StringBuilder updateExpression = new StringBuilder("SET ");

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        List<String> expressions = new ArrayList<>();
        for (Map.Entry<String, AttributeValue> entry : updatedFields.entrySet()) {
            String fieldName = entry.getKey();
            AttributeValue value = entry.getValue();
            expressions.add(fieldName + " = :" + fieldName);
            expressionAttributeValues.put(":" + fieldName, value);
        }
        updateExpression.append(String.join(", ", expressions));

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(PK_COLUMN, AttributeValue.builder().s(uuid).build()))
                .updateExpression(updateExpression.toString())
                .expressionAttributeValues(expressionAttributeValues)
                .build();
        dynamoDbClient.updateItem(request);
    }
}