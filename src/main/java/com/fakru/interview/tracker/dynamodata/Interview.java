package com.fakru.interview.tracker.dynamodata;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class Interview {
    private String roundName;
    private String description;
    private Timestamp interviewDateTime;
}