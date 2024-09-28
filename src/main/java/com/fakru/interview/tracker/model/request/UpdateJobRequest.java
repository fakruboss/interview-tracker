package com.fakru.interview.tracker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateJobRequest {
    private String jobId;
    private String position;
    private String location;
    private JobApplicationStatus status;
    private String source;
    private Long minSalary;
    private Long maxSalary;
    private Long esops;
    private Long rsu;
    private Long joiningBonus;
    private Long signOnBonus;
    private Long relocationBonus;
    private String vestingSchedule;
}
