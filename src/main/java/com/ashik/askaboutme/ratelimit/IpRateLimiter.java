package com.ashik.askaboutme.ratelimit;

import com.ashik.askaboutme.configdto.AskRateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class IpRateLimiter {

    private final AskRateLimitProperties properties;
    private final Map<String, Window> windowsByIp = new ConcurrentHashMap<>();

    public boolean tryConsume(String clientIp) {
        long now = System.currentTimeMillis();
        long windowMillis = properties.window().toMillis();
        AtomicInteger countInWindow = new AtomicInteger();

        windowsByIp.compute(clientIp, (ip, existing) -> {
            Window window = (existing == null || now - existing.startMillis() >= windowMillis)
                    ? new Window(now)
                    : existing;
            countInWindow.set(window.count().incrementAndGet());
            return window;
        });

        return countInWindow.get() <= properties.maxRequests();
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    void evictExpiredWindows() {
        long now = System.currentTimeMillis();
        long windowMillis = properties.window().toMillis();
        windowsByIp.entrySet().removeIf(entry -> now - entry.getValue().startMillis() >= windowMillis);
    }

    private record Window(long startMillis, AtomicInteger count) {
        private Window(long startMillis) {
            this(startMillis, new AtomicInteger(0));
        }
    }
}
