package com.fakru.interview.tracker.service;

import com.fakru.interview.tracker.dynamodata.Interview;
import com.fakru.interview.tracker.dynamodata.Job;
import com.fakru.interview.tracker.dynamodata.Salary;
import com.fakru.interview.tracker.model.request.AddInterviewRequest;
import com.fakru.interview.tracker.model.request.CreateJobRequest;
import com.fakru.interview.tracker.model.request.JobApplicationStatus;
import com.fakru.interview.tracker.model.response.JobsResponse;
import com.fakru.interview.tracker.repository.JobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import util.JoseJwtUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;

import static util.JoseJwtUtil.CLAIMS;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final StorageService storageService;

    private final ObjectMapper objectMapper;

    @Value("${resume.upload.dir}")
    private String uploadDir;

    @Autowired
    public JobService(JobRepository jobRepository, StorageService storageService, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    public void addJob(CreateJobRequest createJobRequest, JWTClaimsSet claimsSet, MultipartFile resume)
            throws IOException {
        UUID userId = UUID.fromString(claimsSet.getSubject());
        uploadResume(resume, userId);
        long currentTime = System.currentTimeMillis();
        Job job = Job.builder()
                .userId(userId.toString())
                .company(createJobRequest.getCompany())
                .position(createJobRequest.getPosition())
                .location(createJobRequest.getLocation())
                .source(createJobRequest.getSource())
                .status(createJobRequest.getStatus())
                .dateApplied(new Timestamp(currentTime))
                .isActive(true)
                .salary(Salary.builder().maxSalary(createJobRequest.getMaxSalary()).build())
                .createdAt(new Timestamp(currentTime))
                .updatedAt(new Timestamp(currentTime))
                .build();
        jobRepository.saveJob(UUID.randomUUID().toString(), job);
    }

    private void uploadResume(MultipartFile resume, UUID subject) throws IOException {
        if (Optional.ofNullable(resume).isPresent() && "application/pdf".equalsIgnoreCase(resume.getContentType())) {
            String fileName = subject + "_" + System.currentTimeMillis() + "_" + resume.getOriginalFilename();
            // storageService.uploadFile(fileName, resume);
            Path filePath = Paths.get(uploadDir + File.separator + fileName);
            Files.copy(resume.getInputStream(), filePath);
        }
    }

    public List<JobsResponse> listJobsByUserId(String userId) {
        List<Map<String, AttributeValue>> byUserId = jobRepository.findByUserId(userId);
        List<JobsResponse> jobsResponses = new ArrayList<>();
        for (Map<String, AttributeValue> e : byUserId) {
            jobsResponses.add(JobsResponse.builder()
                    .company(e.get("company").s())
                    .position(e.get("position").s())
                    .location(e.get("location").s())
                    .source(e.get("source").s())
                    .status(JobApplicationStatus.valueOf(e.get("status").s()))
                    .dateApplied(new Timestamp(Long.parseLong(e.get("date_applied").n())))
                    .build()
            );
        }
        return jobsResponses;
    }

    public void deleteJob(JWTClaimsSet claimsSet, String jobId) {
        UUID userId = UUID.fromString(claimsSet.getSubject());
        jobRepository.deleteJob(jobId, userId.toString());
    }

    public String addInterview(AddInterviewRequest request, String jobId, JWTClaimsSet claimsSet)
            throws JOSEException, JsonProcessingException {
        Interview.InterviewBuilder interviewBuilder = Interview.builder()
                .roundName(request.getRoundName())
                .description(request.getDescription());
        if (request.getInterviewDateTime() != null)
            interviewBuilder.interviewDateTime(request.getInterviewDateTime());
        String interviewDataJson = objectMapper.writeValueAsString(interviewBuilder.build());
        jobRepository.appendInterviewRoundPutItem(jobId, interviewDataJson);
        return JoseJwtUtil.generateToken(claimsSet.getSubject(), (Map<String, Object>) claimsSet.getClaim(CLAIMS));
    }
}
