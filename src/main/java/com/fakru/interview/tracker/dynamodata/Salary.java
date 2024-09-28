package com.fakru.interview.tracker.dynamodata;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Salary {
    private String currency;
    private long minSalary;
    private long maxSalary;
    private long esops;
    private long rsu;
    private long joiningBonus;
    private long signOnBonus;
    private long relocationBonus;
    private String vestingSchedule;
}