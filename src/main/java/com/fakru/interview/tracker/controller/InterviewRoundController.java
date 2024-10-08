package com.fakru.interview.tracker.controller;

import com.fakru.interview.tracker.annotation.JwtAuthenticate;
import com.fakru.interview.tracker.model.request.AddInterviewRequest;
import com.fakru.interview.tracker.model.response.InterviewResponse;
import com.fakru.interview.tracker.service.InterviewService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import util.JoseJwtUtil;

import java.util.List;
import java.util.Map;

import static com.fakru.interview.tracker.constants.ApiConstants.*;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

@RestController
public class InterviewRoundController {

    private final InterviewService interviewService;

    public InterviewRoundController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @JwtAuthenticate
    @PostMapping("api/v1/jobs/{jobId}/interview")
    public ResponseEntity<Map<String, String>> addInterview(@PathVariable("jobId") String jobId,
                                                            @RequestBody AddInterviewRequest request,
                                                            HttpServletRequest httpRequest,
                                                            HttpServletResponse httpResponse) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        interviewService.addInterview(claimsSet.getSubject(), jobId, request);
        setTokenInResponseHeader(claimsSet, httpResponse);
        return new ResponseEntity<>(Map.of(MESSAGE, "Interview round added successfully"), HttpStatus.OK);
    }

    @JwtAuthenticate
    @GetMapping("api/v1/jobs/{jobId}/interview")
    public ResponseEntity<List<InterviewResponse>> listInterviews(@PathVariable("jobId") String jobId,
                                                                  HttpServletRequest httpRequest,
                                                                  HttpServletResponse httpResponse) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        setTokenInResponseHeader(claimsSet, httpResponse);
        return new ResponseEntity<>(interviewService.listInterviews(jobId), HttpStatus.OK);
    }

    private void setTokenInResponseHeader(JWTClaimsSet claimsSet, HttpServletResponse httpResponse) {
        Object items = claimsSet.getClaim("items");
        if (items instanceof Map) {
            String token = JoseJwtUtil.generateSafeToken(claimsSet.getSubject(), (Map<String, Object>) items);
            httpResponse.setHeader(AUTHORIZATION, BEARER + token);
        }
    }
}
