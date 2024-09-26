package com.fakru.interview.tracker.dynamodata;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Salary {
    private long minSalary;
    private long maxSalary;
}