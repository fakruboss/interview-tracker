package com.fakru.interview.tracker.service;

import com.fakru.interview.tracker.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TokenVersionService {
    private static final String CACHE_NAME = "tokenVersions";

    private final CacheManager cacheManager;
    private final UserRepository userRepository;

    @Autowired
    public TokenVersionService(CacheManager cacheManager, UserRepository userRepository) {
        this.cacheManager = cacheManager;
        this.userRepository = userRepository;
    }

    public void cacheTokenVersion(String userId, Long tokenVersion) {
        getCache().put(userId, tokenVersion);
        log.debug("Cached token version {} for user {}", tokenVersion, userId);
    }

    public Long getTokenVersion(String userId) {
        Cache cache = getCache();
        Long cachedVersion = cache.get(userId, Long.class);

        if (cachedVersion != null) {
            return cachedVersion;
        }

        // Cache miss - fetch from DynamoDB and update cache
        return fetchAndCacheTokenVersion(userId);
    }

    @Async
    public CompletableFuture<Long> getTokenVersionAsync(String userId) {
        return CompletableFuture.supplyAsync(() -> getTokenVersion(userId));
    }

    private Long fetchAndCacheTokenVersion(String userId) {
        long tokenVersion = Long.parseLong(userRepository.getUser(userId).get("token_version").n());
        cacheTokenVersion(userId, tokenVersion);
        return tokenVersion;
    }

    private Cache getCache() {
        return cacheManager.getCache(CACHE_NAME);
    }
}
