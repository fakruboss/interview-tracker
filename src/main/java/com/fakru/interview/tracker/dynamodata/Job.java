package com.fakru.interview.tracker.dynamodata;

import com.fakru.interview.tracker.model.request.JobApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Builder
@Data
public class Job {
    private String userId;
    private String company;
    private String position;
    private String location;
    private String source;
    private JobApplicationStatus status;
    private Timestamp dateApplied;
    private boolean isActive;
    private Salary salary;
    private List<Interview> rounds;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
