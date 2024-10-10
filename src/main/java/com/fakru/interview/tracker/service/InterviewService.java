package com.fakru.interview.tracker.service;

import com.fakru.interview.tracker.annotation.LogExecutionTime;
import com.fakru.interview.tracker.dynamodata.Interview;
import com.fakru.interview.tracker.exception.VerificationException;
import com.fakru.interview.tracker.model.request.AddInterviewRequest;
import com.fakru.interview.tracker.model.response.InterviewResponse;
import com.fakru.interview.tracker.repository.JobRepository;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import util.JoseJwtUtil;

import java.sql.Timestamp;
import java.util.List;

@Service
public class InterviewService {

    private final JobRepository jobRepository;
    private final VerificationService verificationService;

    @Autowired
    public InterviewService(JobRepository jobRepository,
                            VerificationService verificationService) {
        this.jobRepository = jobRepository;
        this.verificationService = verificationService;
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

    @LogExecutionTime
    public String sendPhoneOTP1(JWTClaimsSet claimsSet, String phoneNumber) {
        boolean isSent = verificationService.sendPhoneOTP(phoneNumber);
        if (!isSent) {
            throw new VerificationException("Error while sending OTP. Please try again after sometime");
        }
        return JoseJwtUtil.generateSafeToken(claimsSet.getSubject(), claimsSet.getClaims());
    }
}
