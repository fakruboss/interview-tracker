package com.fakru.interview.tracker.controller;

import com.fakru.interview.tracker.annotation.JwtAuthenticate;
import com.fakru.interview.tracker.model.request.CreateJobRequest;
import com.fakru.interview.tracker.model.request.UpdateJobRequest;
import com.fakru.interview.tracker.model.response.JobsResponse;
import com.fakru.interview.tracker.service.JobService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import util.JoseJwtUtil;

import java.util.List;
import java.util.Map;

import static com.fakru.interview.tracker.constants.ApiConstants.JWT_CLAIMS;
import static com.fakru.interview.tracker.constants.ApiConstants.MESSAGE;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

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
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        String token = jobService.addJob(request, claimsSet, resume);
        httpResponse.setHeader(AUTHORIZATION, token);
        return new ResponseEntity<>(Map.of(MESSAGE, "New Job added successfully"), HttpStatus.OK);
    }

    @JwtAuthenticate
    @GetMapping("api/v1/jobs")
    public ResponseEntity<List<JobsResponse>> listAllJobs(HttpServletRequest httpRequest,
                                                          HttpServletResponse httpResponse) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        List<JobsResponse> jobsResponses = jobService.listJobsByUserId(claimsSet.getSubject());
        setTokenInResponseHeader(claimsSet, httpResponse);
        return new ResponseEntity<>(jobsResponses, HttpStatus.OK);
    }

    @JwtAuthenticate
    @PutMapping("api/v1/jobs")
    public ResponseEntity<Map<String, String>> updateJob(@RequestBody UpdateJobRequest request,
                                                         HttpServletRequest httpRequest,
                                                         HttpServletResponse httpResponse) {
        jobService.updateJob(request);
        setTokenInResponseHeader((JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS), httpResponse);
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

    private void setTokenInResponseHeader(JWTClaimsSet claimsSet, HttpServletResponse httpResponse) {
        String token = JoseJwtUtil.generateSafeToken(claimsSet.getSubject(), claimsSet.getClaims());
        httpResponse.setHeader(AUTHORIZATION, token);
    }
}