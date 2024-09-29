package com.fakru.interview.tracker.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddInterviewRequest {
    private short roundCount;
    private String roundName;
    private String description;
    private Timestamp interviewDateTime;
}
