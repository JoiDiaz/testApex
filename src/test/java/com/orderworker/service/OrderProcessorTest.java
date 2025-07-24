package com.orderworker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import com.orderworker.ApiClient;
import com.orderworker.model.Customer;
import com.orderworker.model.EnrichedOrder;
import com.orderworker.model.OrderMessage;
import com.orderworker.model.Product;
import com.orderworker.repository.OrderRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OrderProcessorTest {

    @Mock private ApiClient apiClient;
    @Mock private OrderRepository orderRepository;
    @Mock private LockService lockService;
    @Mock private ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock private ReactiveValueOperations<String, String> valueOps;

    @InjectMocks private OrderProcessor orderProcessor;

    private final String orderId = "orden1";
    private final String customerId = "cliente1";
    private final List<String> productIds = List.of("prod1", "prod2");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shouldProcessOrderSuccessfully() {
       OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setCustomerId(customerId);
        message.setProductIds(productIds);
        when(lockService.acquireLock("lock:order:" + orderId)).thenReturn(Mono.just(true));
        when(valueOps.get("retry:order:" + orderId)).thenReturn(Mono.just("0"));

        Customer customer = new Customer(customerId, "John Doe", true);

        
        List<Product> products = List.of(
        	    new Product("prod1", "Product 1", "Desc 1", 10.0),
        	    new Product("prod2", "Product 2", "Desc 2", 20.0)
        	);

        when(apiClient.getCustomer(customerId)).thenReturn(Mono.just(customer));
        when(apiClient.getProductDetails(productIds)).thenReturn(Mono.just(products));
        when(orderRepository.save(any(EnrichedOrder.class))).thenReturn(Mono.just(new EnrichedOrder()));

        when(lockService.releaseLock("lock:order:" + orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderProcessor.processOrder(message))
            .verifyComplete();

        verify(orderRepository).save(any(EnrichedOrder.class));
    }

    @Test
    void shouldSkipIfLockNotAcquired() {
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setCustomerId(customerId);
        message.setProductIds(productIds);
        when(lockService.acquireLock("lock:order:" + orderId)).thenReturn(Mono.just(false));

        StepVerifier.create(orderProcessor.processOrder(message))
            .verifyComplete();

        verifyNoInteractions(apiClient);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldSkipIfMaxRetriesReached() {
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setCustomerId(customerId);
        message.setProductIds(productIds);
        when(lockService.acquireLock("lock:order:" + orderId)).thenReturn(Mono.just(true));
        when(valueOps.get("retry:order:" + orderId)).thenReturn(Mono.just("5")); // MAX_ATTEMPTS = 5
        when(lockService.releaseLock("lock:order:" + orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderProcessor.processOrder(message))
            .verifyComplete();

        verifyNoInteractions(apiClient);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldRetryIfApiFails() {
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setCustomerId(customerId);
        message.setProductIds(productIds);

        when(lockService.acquireLock("lock:order:" + orderId)).thenReturn(Mono.just(true));
        when(lockService.releaseLock("lock:order:" + orderId)).thenReturn(Mono.empty());

        when(valueOps.get("retry:order:" + orderId)).thenReturn(Mono.just("1"));

        Customer customer = new Customer(customerId, "John Doe", true);
        when(apiClient.getCustomer(customerId)).thenReturn(Mono.just(customer));

    }
}
