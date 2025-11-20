package com.example.oms.kafka;

import com.example.oms.domain.Order;
import com.example.oms.domain.OrderStatus;
import com.example.oms.dto.PlaceOrderRequest;
import com.example.oms.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderRepository orderRepository;

    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate,
                         OrderRepository orderRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }

    public Long sendOrder(PlaceOrderRequest req) {
        Order order = new Order();
        order.setSymbol(req.getSymbol());
        order.setSide(req.getSide());
        order.setPrice(req.getPrice());
        order.setQuantity(req.getQuantity());
        order.setRemainingQuantity(req.getQuantity());
        order.setStatus(OrderStatus.NEW);
        order.setCreatedAt(Instant.now());

        order = orderRepository.save(order);

        String payload = """
            {"orderId": %d}
            """.formatted(order.getId());

        kafkaTemplate.send("orders", String.valueOf(order.getId()), payload);
        return order.getId();
    }
}
