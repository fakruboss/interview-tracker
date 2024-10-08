package com.fakru.interview.tracker.repository;

import com.fakru.interview.tracker.annotation.LogExecutionTime;
import com.fakru.interview.tracker.dynamodata.Interview;
import com.fakru.interview.tracker.dynamodata.Job;
import com.fakru.interview.tracker.exception.DatabaseOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class JobRepository {

    private static final String TABLE_NAME = "job";
    private static final String PK_COLUMN = "pk";
    private static final String SK_COLUMN = "sk";
    private static final String GSI_USER_ID_COLUMN = "user_id";
    private final DynamoDbClient dynamoDbClient;

    public JobRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void saveJob(String uuid, Job job) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put(PK_COLUMN, AttributeValue.builder().s(uuid).build());
            item.put(SK_COLUMN, AttributeValue.builder().n(String.valueOf(0)).build());
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
            throw new DatabaseOperationException(e.getMessage());
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

    @LogExecutionTime
    public void deleteJob(String jobId, String userId) {
        try {
            // Step 1: Query all items with the given jobId (partition key)
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression(PK_COLUMN + " = :jobId")
                    .filterExpression(GSI_USER_ID_COLUMN + " = :userId")
                    .expressionAttributeValues(Map.of(
                            ":jobId", AttributeValue.builder().s(jobId).build(),
                            ":userId", AttributeValue.builder().s(userId).build()
                    ))
                    .build();

            // Execute the query and get all matching items
            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

            List<Map<String, AttributeValue>> items = queryResponse.items();

            // Step 2: Iterate over each item and delete it
            for (Map<String, AttributeValue> item : items) {
                // Extract the sort key (sk) for the item
                String skValue = item.get(SK_COLUMN).n();

                // Delete each item based on jobId (pk) and sk (sort key)
                DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .key(Map.of(
                                PK_COLUMN, AttributeValue.builder().s(jobId).build(),
                                SK_COLUMN, AttributeValue.builder().n(skValue).build()
                        ))
                        .build();

                dynamoDbClient.deleteItem(deleteRequest);
            }
        } catch (DynamoDbException e) {
            throw new DatabaseOperationException("Error deleting job: " + e.getMessage(), e);
        }
    }

    public void saveInterview(String userId, String jobId, short roundCount, Interview interview) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put(PK_COLUMN, AttributeValue.builder().s(jobId).build());
            item.put(SK_COLUMN, AttributeValue.builder().n(String.valueOf(roundCount)).build());
            item.put(GSI_USER_ID_COLUMN, AttributeValue.builder().s(userId).build());

            item.put("round_name", AttributeValue.builder().s(interview.getRoundName()).build());
            item.put("description", AttributeValue.builder().s(interview.getDescription()).build());
            item.put("interview_date_time", AttributeValue.builder().n(String.valueOf(interview.getInterviewDateTime().getTime())).build());

            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName("job")
                    .item(item)
                    .conditionExpression("attribute_not_exists(#pk) AND attribute_not_exists(#sk)")
                    .expressionAttributeNames(Map.of("#pk", PK_COLUMN, "#sk", SK_COLUMN))
                    .build();

            dynamoDbClient.putItem(putRequest);
            log.info("Interview round saved successfully.");
        } catch (ConditionalCheckFailedException e) {
            log.error("An interview round with this job ID and round count already exists.");
        } catch (DynamoDbException e) {
            throw new DatabaseOperationException("Error while saving interview for user id: " + userId + ", job id: " + jobId, e);
        }
    }

    public QueryResponse listInterviews(String jobId) {
        try {
            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression(PK_COLUMN + " = :jobId")
                    .expressionAttributeValues(Map.of(
                            ":jobId", AttributeValue.builder().s(jobId).build()))
                    .build();

            return dynamoDbClient.query(request);
        } catch (DynamoDbException e) {
            throw new DatabaseOperationException("Error retrieving jobs for the job id: " + jobId + " " + e.getMessage(), e);
        }
    }
}
