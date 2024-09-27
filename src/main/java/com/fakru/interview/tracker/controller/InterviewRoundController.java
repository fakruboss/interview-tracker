package com.fakru.interview.tracker.controller;

import com.fakru.interview.tracker.dynamodata.Interview;
import com.fakru.interview.tracker.model.request.AddInterviewRequest;
import com.fakru.interview.tracker.service.JobService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import util.JoseJwtUtil;

import javax.naming.AuthenticationException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static com.fakru.interview.tracker.constants.ApiConstants.MESSAGE;

@RestController
public class InterviewRoundController {

    private final JobService jobService;

    public InterviewRoundController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("api/v1/jobs/{jobId}/interview")
    public ResponseEntity<Map<String, String>> addNewInterview(@RequestHeader("Authorization") String bearerToken,
                                                               @PathVariable("jobId") String jobId,
                                                               @RequestBody AddInterviewRequest request,
                                                               HttpServletResponse response)
            throws ParseException, AuthenticationException, JOSEException, JsonProcessingException {
        JWTClaimsSet claimsSet = JoseJwtUtil.extractClaims(bearerToken);
        String token = jobService.addInterview(request, jobId, claimsSet);
        response.setHeader("Authorization", "Bearer " + token);
        return new ResponseEntity<>(Map.of(MESSAGE, "Interview round added successfully"), HttpStatus.OK);
    }

    @GetMapping("api/v1/jobs/{jobId}/interview")
    public ResponseEntity<List<Interview>> getAllInterviews(@RequestHeader("Authorization") String bearerToken,
                                                            @PathVariable("jobId") long jobId,
                                                            HttpServletResponse response)
            throws ParseException, AuthenticationException, JOSEException {
        JoseJwtUtil.extractClaims(bearerToken);
        response.setHeader("Authorization", "Bearer " + "token");
        return new ResponseEntity<>(null, HttpStatus.OK);
    }
}
