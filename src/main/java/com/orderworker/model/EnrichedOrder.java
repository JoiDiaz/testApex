package com.orderworker.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class EnrichedOrder {
    @Id
    private String id;
    private String orderId;
    private String customerId;
    private String customerName;
    private List<Product> products;
}
