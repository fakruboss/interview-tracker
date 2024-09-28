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
import org.springframework.web.servlet.ModelAndView;
import util.JwtAuthenticationHandler;

import java.io.IOException;
import java.util.Map;

import static com.fakru.interview.tracker.constants.ApiConstants.JWT_CLAIMS;

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
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest().getHeader(headerName);
    }

    private Void handleError(Map<String, Object> errorDetails) {
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getResponse();
        try {
            response.sendError((Integer) errorDetails.get("status"), (String) errorDetails.get("message"));
        } catch (IOException e) {
            // Handle exception
        }
        return null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            JwtAuthenticate jwtAuthenticate = handlerMethod.getMethodAnnotation(JwtAuthenticate.class);

            if (jwtAuthenticate != null) {
                JWTClaimsSet claimsSet = jwtHandler.authenticate(Map.of("request", request));
                if (claimsSet != null) {
                    request.setAttribute(JWT_CLAIMS, claimsSet);
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        JWTClaimsSet claimsSet = (JWTClaimsSet) request.getAttribute(JWT_CLAIMS);
        String newToken = jwtHandler.refreshToken(claimsSet);
        if (newToken != null) {
            response.setHeader("Authorization", "Bearer " + newToken);
        }
    }
}
