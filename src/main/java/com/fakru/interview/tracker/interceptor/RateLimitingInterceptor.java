package com.fakru.interview.tracker.interceptor;

import com.nimbusds.jwt.JWTClaimsSet;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.fakru.interview.tracker.constants.ApiConstants.JWT_CLAIMS;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String apiPath = request.getRequestURI();

        // Retrieve the userId from request attributes (set by JwtAuthenticationInterceptor)
        String userId = ((JWTClaimsSet) request.getAttribute(JWT_CLAIMS)).getSubject();

        // Handle unauthenticated requests
        if (userId == null) {
            // Apply a shared rate limit for unauthenticated requests (e.g., IP-based or shared bucket)
            String unauthenticatedApiKey = "unauthenticated:" + apiPath;
            Bucket unauthenticatedBucket = userBuckets.computeIfAbsent(unauthenticatedApiKey, key -> createUnauthenticatedBucket());

            if (!unauthenticatedBucket.tryConsume(1)) {
                response.setStatus(HttpStatus.SC_TOO_MANY_REQUESTS);
                response.getWriter().write("Rate limit exceeded for unauthenticated users. Try again later.");
                return false;
            }
            return true;
        }

        // Handle authenticated requests (using userId)
        String userApiKey = userId + ":" + apiPath;
        Bucket userBucket = userBuckets.computeIfAbsent(userApiKey, key -> createNewBucket());

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
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    // Method to create a new bucket for authenticated users
    private Bucket createNewBucket() {
        // Create a limit of 10 requests per 5 minutes for authenticated users
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(5)));
        return Bucket.builder().addLimit(limit).build();
    }

}
