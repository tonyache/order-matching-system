package com.example.oms.service;

import com.example.oms.domain.Order;
import com.example.oms.domain.OrderSide;
import com.example.oms.domain.OrderStatus;
import com.example.oms.repository.OrderRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReplayService {

    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;

    public ReplayService(OrderRepository orderRepository,
                         StringRedisTemplate redisTemplate) {
        this.orderRepository = orderRepository;
        this.redisTemplate = redisTemplate;
    }

    public void rebuildOrderBook() {
        List<Order> openOrders = orderRepository
                .findByStatusIn(List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED));

        for (Order o : openOrders) {
            String key = "orderbook:" + o.getSymbol() + ":" + o.getSide().name();
            double score;

            if (o.getSide() == OrderSide.BUY) {
                score = o.getPrice().negate().doubleValue();
            } else {
                score = o.getPrice().doubleValue();
            }

            redisTemplate.opsForZSet().add(key, String.valueOf(o.getId()), score);
        }
    }
}
