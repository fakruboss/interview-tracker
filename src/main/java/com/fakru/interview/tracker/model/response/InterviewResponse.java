package com.fakru.interview.tracker.model.response;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class InterviewResponse {
    private String roundNumber;
    private String roundName;
    private String description;
    private Timestamp interviewDateTime;
}
