package com.fakru.interview.tracker.repository;

import com.fakru.interview.tracker.dynamodata.Job;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JobRepository {

    private static final String TABLE_NAME = "job";
    private static final String PK_COLUMN = "pk";
    private static final String DATA_COLUMN = "data";
    private static final String GSI_USER_ID_COLUMN = "user_id";
    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;

    public JobRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.objectMapper = new ObjectMapper();
    }

    public void saveJob(String uuid, Job job) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put(PK_COLUMN, AttributeValue.builder().s(uuid).build());
            item.put(GSI_USER_ID_COLUMN, AttributeValue.builder().s(job.getUserId()).build());

            item.put("company", AttributeValue.builder().s(job.getCompany()).build());
            item.put("position", AttributeValue.builder().s(job.getPosition()).build());
            item.put("location", AttributeValue.builder().s(job.getLocation()).build());
            item.put("source", AttributeValue.builder().s(job.getSource()).build());
            item.put("status", AttributeValue.builder().s(job.getStatus().name()).build());
            item.put("date_applied", AttributeValue.builder().n(String.valueOf(job.getDateApplied().getTime())).build());
            item.put("is_active", AttributeValue.builder().bool(job.isActive()).build());
            item.put("currency", AttributeValue.builder().s(job.getCurrency()).build());
            item.put("min_salary", AttributeValue.builder().n(String.valueOf(job.getMinSalary())).build());
            item.put("max_salary", AttributeValue.builder().n(String.valueOf(job.getMaxSalary())).build());
            item.put("esops", AttributeValue.builder().n(String.valueOf(job.getEsops())).build());
            item.put("rsu", AttributeValue.builder().n(String.valueOf(job.getRsu())).build());
            item.put("joining_bonus", AttributeValue.builder().n(String.valueOf(job.getJoiningBonus())).build());
            item.put("sign_on_bonus", AttributeValue.builder().n(String.valueOf(job.getSignOnBonus())).build());
            item.put("relocation_bonus", AttributeValue.builder().n(String.valueOf(job.getRelocationBonus())).build());
            item.put("vesting_schedule", AttributeValue.builder().s(job.getVestingSchedule()).build());
            item.put("created_at", AttributeValue.builder().n(String.valueOf(job.getCreatedAt().getTime())).build());
            item.put("updated_at", AttributeValue.builder().n(String.valueOf(job.getUpdatedAt().getTime())).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build();
            dynamoDbClient.putItem(request);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Map<String, AttributeValue>> findByUserId(String userId) {
        Map<String, AttributeValue> eav = Map.of(":user_id", AttributeValue.builder().s(userId).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName("user_id-index")
                .keyConditionExpression("user_id = :user_id")
                .expressionAttributeValues(eav)
                .build();

        QueryResponse response = dynamoDbClient.query(queryRequest);
        if (!response.items().isEmpty()) {
            return response.items();
        } else {
            throw new RuntimeException("User with user id " + userId + " not found");
        }
    }

    public void updateJob(String uuid, Map<String, AttributeValue> updatedFields) {
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

    public void deleteJob(String jobId, String userId) {
        try {
            DeleteItemRequest request = DeleteItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of(PK_COLUMN, AttributeValue.builder().s(jobId).build()))
                    .conditionExpression("#userId = :userId")
                    .expressionAttributeNames(Map.of("#userId", GSI_USER_ID_COLUMN))
                    .expressionAttributeValues(Map.of(
                            ":userId", AttributeValue.builder().s(userId).build()
                    ))
                    .build();
            dynamoDbClient.deleteItem(request);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
        }
    }

    public void appendInterviewRoundPutItem(String uuid, String newRoundJson) {
        try {
            // First, get the current item
            GetItemRequest getItemRequest = GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of(PK_COLUMN, AttributeValue.builder().s(uuid).build()))
                    .build();

            Map<String, AttributeValue> item = dynamoDbClient.getItem(getItemRequest).item();

            if (item == null) {
                throw new RuntimeException("Job not found");
            }

            // Parse and update the JSON data
            String existingDataJson = item.get(DATA_COLUMN).s();
            JsonNode existingDataNode = objectMapper.readTree(existingDataJson);

            if (!existingDataNode.has("rounds") || !existingDataNode.get("rounds").isArray()) {
                ((ObjectNode) existingDataNode).putArray("rounds");
            }

            ArrayNode roundsArray = (ArrayNode) existingDataNode.get("rounds");
            roundsArray.add(objectMapper.readTree(newRoundJson));

            String updatedDataJson = objectMapper.writeValueAsString(existingDataNode);

            // Prepare the new item with all existing attributes
            Map<String, AttributeValue> newItem = new HashMap<>(item);
            newItem.put(DATA_COLUMN, AttributeValue.builder().s(updatedDataJson).build());

            // Use PutItemRequest to replace the entire item
            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(newItem)
                    .conditionExpression("attribute_exists(#pk)")
                    .expressionAttributeNames(Map.of("#pk", PK_COLUMN))
                    .build();

            dynamoDbClient.putItem(putRequest);
        } catch (Exception e) {
            throw new RuntimeException("Error appending interview round: " + e.getMessage(), e);
        }
    }
}