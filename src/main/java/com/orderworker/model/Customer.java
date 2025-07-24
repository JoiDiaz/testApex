package com.orderworker.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private String customerId;
    private String fullName;
    private boolean active;
}
