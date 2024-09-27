package com.fakru.interview.tracker.controller;

import com.fakru.interview.tracker.annotation.JwtAuthenticate;
import com.fakru.interview.tracker.model.request.CreateJobRequest;
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
    public ResponseEntity<Map<String, String>> addNewJob(@RequestParam(value = "resume", required = false) MultipartFile resume,
                                                         @RequestPart("createJobRequest") CreateJobRequest request,
                                                         HttpServletRequest httpRequest)
            throws IOException {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute("jwtClaims");
        jobService.addJob(request, claimsSet, resume);
        return new ResponseEntity<>(Map.of(MESSAGE, "New Job added successfully"), HttpStatus.OK);
    }

    @JwtAuthenticate
    @GetMapping("api/v1/jobs")
    public ResponseEntity<List<JobsResponse>> listAllJobs(HttpServletRequest httpRequest) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute("jwtClaims");
        return new ResponseEntity<>(jobService.listJobsByUserId(claimsSet.getSubject()), HttpStatus.OK);
    }

    // TODO: handle update job
    @PutMapping("api/v1/jobs")
    public ResponseEntity<String> updateJob(@RequestHeader("Authorization") String bearerToken,
                                            @RequestBody CreateJobRequest createJobRequest) {
        return new ResponseEntity<>("Job updated successfully", HttpStatus.OK);
    }

    @JwtAuthenticate
    @DeleteMapping("api/v1/jobs/{jobId}")
    public ResponseEntity<String> deleteJob(@PathVariable String jobId,
                                            HttpServletRequest httpRequest) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute("jwtClaims");
        jobService.deleteJob(claimsSet, jobId);
        return new ResponseEntity<>("Job deleted successfully", HttpStatus.OK);
    }
}