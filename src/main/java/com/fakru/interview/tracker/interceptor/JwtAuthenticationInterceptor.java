package com.fakru.interview.tracker.interceptor;

import com.fakru.interview.tracker.annotation.JwtAuthenticate;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import util.JwtAuthenticationHandler;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtAuthenticationHandler jwtHandler;

    public JwtAuthenticationInterceptor() {
        this.jwtHandler = new JwtAuthenticationHandler(
                this::extractHeader,
                this::handleError
        );
    }

    private String extractHeader(String headerName) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // Handle the case when there are no request attributes
            // You might want to log this or throw a custom exception
            throw new IllegalStateException("No request attributes found");
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(headerName);
    }

    private Void handleError(Map<String, Object> errorDetails) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No request attributes found");
        }

        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            throw new IllegalStateException("No response object found");
        }

        Integer status = (Integer) errorDetails.get("status");
        String message = (String) errorDetails.get("message");

        if (status == null || message == null) {
            throw new IllegalArgumentException("Invalid error details provided");
        }

        try {
            response.sendError(status, message);
        } catch (IOException e) {
            // Log the exception
            // You might want to rethrow or handle this exception based on your requirements
            throw new RuntimeException("Failed to send error response", e);
        }
        return null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            JwtAuthenticate jwtAuthenticate = handlerMethod.getMethodAnnotation(JwtAuthenticate.class);

            if (jwtAuthenticate != null) {
                JWTClaimsSet claimsSet = jwtHandler.authenticate();
                if (claimsSet != null) {
                    request.setAttribute("jwtClaims", claimsSet);
                    return true;
                }
                return false;
            }
        }
        return true;
    }
}
