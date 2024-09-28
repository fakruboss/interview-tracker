package com.fakru.interview.tracker.dynamodata;

import com.fakru.interview.tracker.model.request.JobApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

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
    private String currency;
    private long minSalary;
    private long maxSalary;
    private long esops;
    private long rsu;
    private long joiningBonus;
    private long signOnBonus;
    private long relocationBonus;
    private String vestingSchedule;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
