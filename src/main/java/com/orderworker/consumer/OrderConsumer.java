package com.orderworker.consumer;

import com.orderworker.model.OrderMessage;
import com.orderworker.service.OrderProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderProcessor orderProcessor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-topic", groupId = "order-group")
    public void listen(String messageJson) {
        try {
            OrderMessage message = objectMapper.readValue(messageJson, OrderMessage.class);
            orderProcessor.processOrder(message).subscribe();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
