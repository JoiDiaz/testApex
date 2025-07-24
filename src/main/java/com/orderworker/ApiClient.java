package com.orderworker;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import reactor.core.publisher.Mono;
import io.github.resilience4j.reactor.retry.RetryOperator;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.orderworker.model.Customer;
import com.orderworker.model.Product;

@Component
public class ApiClient {

    private final WebClient webClient;
    private final Retry retry;
    
    @Value("${api.customer-url}")
    private String customerUrl;

    @Value("${api.products-url}")
    private String productsUrl;

    public ApiClient(WebClient.Builder builder) {
        this.webClient = builder.build();
        this.retry = Retry.of("api-retry", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .build());
    }

    public Mono<Customer> getCustomer(String customerId) {
        return webClient.get()
                .uri(customerUrl + "/" + customerId)
                .retrieve()
                .bodyToMono(Customer.class)
                .transformDeferred(RetryOperator.of(retry))
                .onErrorResume(throwable -> fallbackCustomer(customerId, throwable));
    }

    public Mono<List<Product>> getProductDetails(List<String> productIds) {
        return webClient.post()
                .uri(productsUrl)
                .bodyValue(productIds)
                .retrieve()
                .bodyToFlux(Product.class)
                .transformDeferred(RetryOperator.of(retry))
                .collectList()
                .onErrorResume(throwable -> fallbackProducts(productIds, throwable));
    }


    public Mono<Customer> fallbackCustomer(String customerId, Throwable t) {
        System.err.println("Fallback for customerId=" + customerId + ", cause: " + t.getMessage());
        return Mono.empty(); 
    }

    public Mono<List<Product>> fallbackProducts(List<String> productIds, Throwable t) {
        System.err.println("Fallback for productIds=" + productIds + ", cause: " + t.getMessage());
        return Mono.just(List.of()); 
    }
}
