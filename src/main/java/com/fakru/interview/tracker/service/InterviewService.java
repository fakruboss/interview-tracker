package com.fakru.interview.tracker.service;

import com.fakru.interview.tracker.dynamodata.Interview;
import com.fakru.interview.tracker.model.request.AddInterviewRequest;
import com.fakru.interview.tracker.model.response.InterviewResponse;
import com.fakru.interview.tracker.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.sql.Timestamp;
import java.util.List;

@Service
public class InterviewService {

    private final JobRepository jobRepository;

    @Autowired
    public InterviewService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void addInterview(String userId, String jobId, AddInterviewRequest request) {
        Interview interview = Interview.builder()
                .roundName(request.getRoundName())
                .description(request.getDescription())
                .interviewDateTime(request.getInterviewDateTime())
                .build();
        jobRepository.saveInterview(userId, jobId, request.getRoundCount(), interview);
    }

    public List<InterviewResponse> listInterviews(String jobId) {
        QueryResponse queryResponse = jobRepository.listInterviews(jobId);
        return queryResponse.items().stream().skip(1) // Ignore the first item since it's job data
                .map(item -> InterviewResponse.builder()
                        .roundNumber(item.get("sk").n()) // sk is stored as a number
                        .roundName(item.get("round_name").s()) // round_name as a string
                        .description(item.get("description").s()) // description as a string
                        .interviewDateTime(new Timestamp(Long.parseLong(item.get("interview_date_time").n()))) // Parse timestamp
                        .build())
                .toList();
    }
}
