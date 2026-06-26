package com.saas.MedStorage_api.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final long windowMillis;

    private final ConcurrentHashMap<String, AttemptWindow> windows = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${security.login.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${security.login.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowSeconds * 1000L;
    }

    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();
        AttemptWindow window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startMs > windowMillis) {
                return new AttemptWindow(now);
            }
            existing.increment();
            return existing;
        });
        return window.count() <= maxAttempts;
    }

    @Scheduled(fixedDelay = 300_000)
    void evictExpired() {
        long now = System.currentTimeMillis();
        int before = windows.size();
        windows.entrySet().removeIf(e -> now - e.getValue().startMs > windowMillis * 2);
        int removed = before - windows.size();
        if (removed > 0) {
            log.debug("Rate limiter evicted {} expired entries", removed);
        }
    }

    static final class AttemptWindow {
        final long startMs;
        private final AtomicInteger attempts;

        AttemptWindow(long startMs) {
            this.startMs = startMs;
            this.attempts = new AtomicInteger(1);
        }

        void increment() {
            attempts.incrementAndGet();
        }

        int count() {
            return attempts.get();
        }
    }
}
