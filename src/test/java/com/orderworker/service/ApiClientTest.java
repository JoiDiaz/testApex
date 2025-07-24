package com.orderworker.service;

import com.orderworker.ApiClient;
import com.orderworker.model.Customer;
import com.orderworker.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApiClientTest {

    private WebClient mockWebClient;
    private ApiClient apiClient;

    @BeforeEach
    void setup() {
        mockWebClient = WebClient.builder().baseUrl("http://localhost").build();
       // apiClient = new ApiClient(mockWebClient);
    }

    @Test
    void testGetCustomerFallback() {
        ApiClient fallbackClient = spy(apiClient);
        doReturn(Mono.error(new RuntimeException("error")))
                .when(fallbackClient).getCustomer(anyString());

        Mono<Customer> result = fallbackClient.getCustomer("c1");

        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void testGetProductDetailsFallback() {
        ApiClient fallbackClient = spy(apiClient);
        doReturn(Mono.error(new RuntimeException("error")))
                .when(fallbackClient).getProductDetails(anyList());

        Mono<List<Product>> result = fallbackClient.getProductDetails(Collections.singletonList("p1"));

        StepVerifier.create(result)
                .expectError()
                .verify();
    }
}
