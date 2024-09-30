package com.fakru.interview.tracker.service;

import com.fakru.interview.tracker.dynamodata.Job;
import com.fakru.interview.tracker.model.request.CreateJobRequest;
import com.fakru.interview.tracker.model.request.JobApplicationStatus;
import com.fakru.interview.tracker.model.request.UpdateJobRequest;
import com.fakru.interview.tracker.model.response.JobsResponse;
import com.fakru.interview.tracker.repository.JobRepository;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import util.JoseJwtUtil;
import util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;

@Service
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final StorageService storageService;

    @Value("${resume.upload.dir}")
    private String uploadDir;

    @Autowired
    public JobService(JobRepository jobRepository, StorageService storageService) {
        this.jobRepository = jobRepository;
        this.storageService = storageService;
    }

    public String addJob(CreateJobRequest request, JWTClaimsSet claimsSet, MultipartFile resume) {
        uploadResume(resume, claimsSet.getSubject());
        long currentTime = System.currentTimeMillis();
        Job job = Job.builder()
                .userId(claimsSet.getSubject())
                .company(request.getCompany())
                .position(request.getPosition())
                .location(request.getLocation())
                .source(request.getSource())
                .status(request.getStatus())
                .dateApplied(new Timestamp(currentTime))
                .isActive(true)
                .currency(request.getCurrency())
                .minSalary(request.getMinSalary())
                .maxSalary(request.getMaxSalary())
                .esops(request.getEsops())
                .rsu(request.getRsu())
                .joiningBonus(request.getJoiningBonus())
                .signOnBonus(request.getSignOnBonus())
                .relocationBonus(request.getRelocationBonus())
                .vestingSchedule(request.getVestingSchedule())
                .createdAt(new Timestamp(currentTime))
                .updatedAt(new Timestamp(currentTime))
                .build();
        jobRepository.saveJob(UUID.randomUUID().toString(), job);
        return JoseJwtUtil.generateSafeToken(claimsSet.getSubject(), claimsSet.getClaims());

    }

    public void updateJob(UpdateJobRequest request) {
        Map<String, AttributeValue> updatedItem = new HashMap<>();

        addOptionalStringAttribute(updatedItem, "position", request.getPosition());
        addOptionalStringAttribute(updatedItem, "location", request.getLocation());
        addOptionalStringAttribute(updatedItem, "status",
                Optional.ofNullable(request.getStatus()).map(Enum::name).orElse(null));
        addOptionalStringAttribute(updatedItem, "source", request.getSource());
        addOptionalStringAttribute(updatedItem, "vesting_schedule", request.getVestingSchedule());

        addOptionalLongAttribute(updatedItem, "min_salary", request.getMinSalary());
        addOptionalLongAttribute(updatedItem, "max_salary", request.getMaxSalary());
        addOptionalLongAttribute(updatedItem, "esops", request.getEsops());
        addOptionalLongAttribute(updatedItem, "rsu", request.getRsu());
        addOptionalLongAttribute(updatedItem, "joining_bonus", request.getJoiningBonus());
        addOptionalLongAttribute(updatedItem, "sign_on_bonus", request.getSignOnBonus());
        addOptionalLongAttribute(updatedItem, "relocation_bonus", request.getRelocationBonus());

        updatedItem.put("updated_at", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());
        jobRepository.updateJob(request.getJobId(), updatedItem);
    }

    private void addOptionalStringAttribute(Map<String, AttributeValue> updatedItem, String key, String valueOpt) {
        Optional.ofNullable(valueOpt).filter(StringUtils::isNonEmpty)
                .ifPresent(value -> updatedItem.put(key, AttributeValue.builder().s(value).build()));
    }

    private void addOptionalLongAttribute(Map<String, AttributeValue> updatedItem, String key, Long valueOpt) {
        Optional.ofNullable(valueOpt).filter(value -> value != 0)
                .ifPresent(value -> updatedItem.put(key, AttributeValue.builder().n(String.valueOf(value)).build()));
    }

    private void uploadResume(MultipartFile resume, String userId) {
        if (Optional.ofNullable(resume).isPresent() && "application/pdf".equalsIgnoreCase(resume.getContentType())) {
            String fileName = userId + "_" + System.currentTimeMillis() + "_" + resume.getOriginalFilename();
            // storageService.uploadFile(fileName, resume);
            Path filePath = Paths.get(uploadDir + File.separator + fileName);
            try {
                Files.copy(resume.getInputStream(), filePath);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to copy file", e);
            }
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
}
