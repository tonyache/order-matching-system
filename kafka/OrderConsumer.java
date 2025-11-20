package com.example.oms.kafka;

import com.example.oms.domain.Order;
import com.example.oms.domain.Trade;
import com.example.oms.engine.MatchingEngine;
import com.example.oms.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderConsumer {

    private final OrderRepository orderRepository;
    private final MatchingEngine matchingEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderConsumer(OrderRepository orderRepository,
                         MatchingEngine matchingEngine) {
        this.orderRepository = orderRepository;
        this.matchingEngine = matchingEngine;
    }

    @KafkaListener(topics = "orders", groupId = "oms-matching")
    public void handleOrderEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            Long orderId = node.get("orderId").asLong();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            List<Trade> trades = matchingEngine.match(order);

            // In a real system: publish trades to another topic, notify clients, etc.
            if (!trades.isEmpty()) {
                System.out.println("Executed trades: " + trades);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
