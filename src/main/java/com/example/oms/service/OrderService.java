package com.example.oms.service;

import com.example.oms.model.Order;
import com.example.oms.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {

    private static final String ORDERS_TOPIC = "orders";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderService(OrderRepository orderRepository,
                        KafkaTemplate<String, String> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Order createOrder(String symbol, String side, BigDecimal price, Long quantity) {
        Order.Side orderSide = Order.Side.valueOf(side.toUpperCase());
        Order order = new Order(symbol, orderSide, price, quantity);
        Order saved = orderRepository.save(order);

        // send to Kafka (simple JSON)
        try {
            String payload = objectMapper.writeValueAsString(saved);
            kafkaTemplate.send(ORDERS_TOPIC, saved.getId().toString(), payload);
        } catch (JsonProcessingException e) {
            // in a real system you'd log this properly
            e.printStackTrace();
        }

        return saved;
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id).orElse(null);
    }
}
