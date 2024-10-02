package com.fakru.interview.tracker.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class CacheAdminController {
    private final CacheManager cacheManager;

    @GetMapping("/cache/stats")
    public Map<String, Object> getCacheStats() {
        Cache cache = cacheManager.getCache("tokenVersions");
        if (cache instanceof CaffeineCache caffeineCache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

            CacheStats stats = nativeCache.stats();

            return Map.of(
                    "hitCount", stats.hitCount(),
                    "missCount", stats.missCount(),
                    "hitRate", stats.hitRate(),
                    "evictionCount", stats.evictionCount()
            );
        }
        return Map.of("error", "Cache stats not available");
    }
}
