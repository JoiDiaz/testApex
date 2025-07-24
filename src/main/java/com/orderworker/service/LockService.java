package com.orderworker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LockService {

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> acquireLock(String key) {
        return redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(30));
    }

    public Mono<Boolean> releaseLock(String key) {
        return redisTemplate.delete(key).thenReturn(true);
    }
}
