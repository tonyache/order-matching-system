package com.example.oms.controller;

import com.example.oms.model.Order;
import com.example.oms.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> body) {
        String symbol = (String) body.get("symbol");
        String side   = (String) body.get("side");
        Number priceN = (Number) body.get("price");
        Number qtyN   = (Number) body.get("quantity");

        if (symbol == null || side == null || priceN == null || qtyN == null) {
            return ResponseEntity.badRequest().build();
        }

        BigDecimal price = new BigDecimal(priceN.toString());
        Long quantity = qtyN.longValue();

        Order order = orderService.createOrder(symbol, side, price, quantity);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrder(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }
}
