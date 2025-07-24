package com.orderworker.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import com.orderworker.ApiClient;
import com.orderworker.model.Customer;
import com.orderworker.model.EnrichedOrder;
import com.orderworker.model.OrderMessage;
import com.orderworker.model.Product;
import com.orderworker.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProcessor {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(10);

    private final ApiClient apiClient;
    private final OrderRepository orderRepository;
    private final LockService lockService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public Mono<Void> processOrder(OrderMessage message) {
        String lockKey = "lock:order:" + message.getOrderId();
        String retryKey = "retry:order:" + message.getOrderId();

        return lockService.acquireLock(lockKey)
            .flatMap(acquired -> {
                if (!acquired) return Mono.empty();

                return redisTemplate.opsForValue().get(retryKey)
                    .map(s -> {
                        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
                    })
                    .defaultIfEmpty(0)
                    .flatMap(attempts -> {
                        if (attempts >= MAX_ATTEMPTS) {
                            log.warn("Máximos intentos alcanzados para orden {}", message.getOrderId());
                            // Aquí podrías enviar a DLQ o hacer otro manejo
                            return Mono.empty();
                        }

                        return Mono.zip(
                            apiClient.getCustomer(message.getCustomerId()),
                            apiClient.getProductDetails(message.getProductIds())
                        )
                        .flatMap(tuple -> {
                            Customer customer = tuple.getT1();
                            List<Product> products = tuple.getT2();

                            if (customer == null || !customer.isActive())
                                return Mono.error(new RuntimeException("Cliente inactivo o inexistente"));

                            if (products == null || products.size() != message.getProductIds().size())
                                return Mono.error(new RuntimeException("Productos inválidos"));

                            EnrichedOrder order = new EnrichedOrder(
                                null, message.getOrderId(),
                                customer.getCustomerId(), customer.getFullName(), products);

                            return orderRepository.save(order).then();
                        })
                        .onErrorResume(ex -> 
                            redisTemplate.opsForValue()
                                .increment(retryKey)
                                .flatMap(i -> redisTemplate.expire(retryKey, RETRY_DELAY))
                                .then(Mono.error(ex))
                        );
                    })
                    .doFinally(sig -> lockService.releaseLock(lockKey).subscribe());
            });
    }

}
