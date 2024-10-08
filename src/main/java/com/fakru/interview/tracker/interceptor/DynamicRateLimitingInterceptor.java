package com.fakru.interview.tracker.interceptor;

import com.fakru.interview.tracker.config.RateLimitConfig;
import com.nimbusds.jwt.JWTClaimsSet;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.fakru.interview.tracker.constants.ApiConstants.JWT_CLAIMS;

@Component
@Slf4j
public class DynamicRateLimitingInterceptor implements HandlerInterceptor {

    // Default rate limit (10 requests per minute)
    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_DURATION = 1;

    private final RateLimitConfig rateLimitConfig;
    private final Map<String, Bucket> rateLimitBuckets = new ConcurrentHashMap<>();

    public DynamicRateLimitingInterceptor(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String apiPath = request.getRequestURI();
        String httpMethod = request.getMethod();
        JWTClaimsSet claimsSet = (JWTClaimsSet) request.getAttribute(JWT_CLAIMS);

        // Handle unauthenticated requests
        if (claimsSet == null) {
            String unauthenticatedApiKey = "unauthenticated:" + apiPath + ":" + httpMethod;
            Bucket unauthenticatedBucket = rateLimitBuckets.computeIfAbsent(unauthenticatedApiKey, keyForBucket -> createUnauthenticatedBucket());

            if (!unauthenticatedBucket.tryConsume(1)) {
                response.setStatus(HttpStatus.SC_TOO_MANY_REQUESTS);
                response.getWriter().write("Rate limit exceeded for unauthenticated users. Try again later.");
                return false;
            }
            return true;
        }

        // Handle authenticated requests (using userId)
        String userId = claimsSet.getSubject();
        String userApiKey = userId + ":" + apiPath + ":" + httpMethod;
        RateLimitConfig.MethodConfig methodConfig = getRateLimit(apiPath, httpMethod);
        Bucket userBucket = rateLimitBuckets.computeIfAbsent(userApiKey, keyForBucket -> createBucket(methodConfig));

        if (!userBucket.tryConsume(1)) {
            response.setStatus(HttpStatus.SC_TOO_MANY_REQUESTS);
            response.getWriter().write("Rate limit exceeded. Try again later.");
            return false;
        }

        return true;
    }

    // Method to create a bucket for unauthenticated requests
    private Bucket createUnauthenticatedBucket() {
        // Create a limit of 5 requests per minute for unauthenticated users
        Bandwidth limit = Bandwidth.simple(DEFAULT_LIMIT, Duration.ofMinutes(DEFAULT_DURATION));
        return Bucket.builder().addLimit(limit).build();
    }

    // Method to create a new bucket for authenticated users
    private Bucket createBucket(RateLimitConfig.MethodConfig methodConfig) {
        Bandwidth limit = Bandwidth.simple(methodConfig.getLimit(), Duration.ofMinutes(methodConfig.getDuration()));
        return Bucket.builder().addLimit(limit).build();
    }

    public RateLimitConfig.MethodConfig getRateLimit(String apiPath, String httpMethod) {
        return Optional.ofNullable(rateLimitConfig.getRateLimits())
                .orElse(Collections.emptyList())
                .stream()
                .filter(pathConfig -> matchesPath(apiPath, pathConfig.getPath()))
                .findFirst()
                .map(pathConfig -> pathConfig.getMethods().get(httpMethod))
                .orElseGet(() -> {
                    log.debug("No rate limit found for path: {} and method: {}. Using default values.",
                            apiPath, httpMethod);
                    return createDefaultMethodConfig();
                });
    }

    private boolean matchesPath(String actualPath, String configPath) {
        String regexPath = configPath.replaceAll("\\{[^/]+}", "[^/]+");
        return actualPath.matches(regexPath.replace("/", "\\/"));
    }

    private RateLimitConfig.MethodConfig createDefaultMethodConfig() {
        RateLimitConfig.MethodConfig defaultConfig = new RateLimitConfig.MethodConfig();
        defaultConfig.setLimit(DEFAULT_LIMIT);
        defaultConfig.setDuration(DEFAULT_DURATION);
        return defaultConfig;
    }
}
