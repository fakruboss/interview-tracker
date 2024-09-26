package com.fakru.interview.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class InterviewTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewTrackerApplication.class, args);
    }

}
