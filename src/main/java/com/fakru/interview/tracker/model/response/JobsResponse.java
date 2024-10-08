package com.fakru.interview.tracker.model.response;

import com.fakru.interview.tracker.model.request.JobApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@Builder
public class JobsResponse {
    private String jobId;
    private String company;
    private String position;
    private String location;
    private String source;
    private JobApplicationStatus status;
    private Timestamp dateApplied;
}
