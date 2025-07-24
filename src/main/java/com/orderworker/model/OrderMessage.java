package com.orderworker.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class OrderMessage {
    private String orderId;
    private String customerId;

    @JsonProperty("products")
    private List<String> productIds;
}