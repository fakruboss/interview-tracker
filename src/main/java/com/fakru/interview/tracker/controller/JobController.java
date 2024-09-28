package com.fakru.interview.tracker.controller;

import com.fakru.interview.tracker.annotation.JwtAuthenticate;
import com.fakru.interview.tracker.model.request.CreateJobRequest;
import com.fakru.interview.tracker.model.request.UpdateJobRequest;
import com.fakru.interview.tracker.model.response.JobsResponse;
import com.fakru.interview.tracker.service.JobService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.fakru.interview.tracker.constants.ApiConstants.JWT_CLAIMS;
import static com.fakru.interview.tracker.constants.ApiConstants.MESSAGE;

@RestController
public class JobController {

    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @JwtAuthenticate
    @PostMapping("api/v1/jobs")
    public ResponseEntity<Map<String, String>> addJob(@RequestParam(value = "resume", required = false) MultipartFile resume,
                                                      @RequestPart("createJobRequest") CreateJobRequest request,
                                                      HttpServletRequest httpRequest)
            throws IOException {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        String userId = claimsSet.getSubject();
        jobService.addJob(request, userId, resume);
        return new ResponseEntity<>(Map.of(MESSAGE, "New Job added successfully"), HttpStatus.OK);
    }

    @JwtAuthenticate
    @GetMapping("api/v1/jobs")
    public ResponseEntity<List<JobsResponse>> listAllJobs(HttpServletRequest httpRequest) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        return new ResponseEntity<>(jobService.listJobsByUserId(claimsSet.getSubject()), HttpStatus.OK);
    }

    @JwtAuthenticate
    @PutMapping("api/v1/jobs")
    public ResponseEntity<Map<String, String>> updateJob(@RequestBody UpdateJobRequest request) {
        jobService.updateJob(request);
        return new ResponseEntity<>(Map.of(MESSAGE, "Job updated successfully"), HttpStatus.OK);
    }

    @JwtAuthenticate
    @DeleteMapping("api/v1/jobs/{jobId}")
    public ResponseEntity<String> deleteJob(@PathVariable String jobId,
                                            HttpServletRequest httpRequest) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        jobService.deleteJob(claimsSet, jobId);
        return new ResponseEntity<>("Job deleted successfully", HttpStatus.OK);
    }
}