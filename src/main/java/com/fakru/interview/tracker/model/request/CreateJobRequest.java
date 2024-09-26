package com.fakru.interview.tracker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobRequest {
    private String company;
    private String position;
    private String location;
    private Timestamp dateApplied;
    private JobApplicationStatus status;
    private String source;
    private long maxSalary;
}
