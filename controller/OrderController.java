package com.example.oms.controller;

import com.example.oms.dto.PlaceOrderRequest;
import com.example.oms.dto.PlaceOrderResponse;
import com.example.oms.kafka.OrderProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderProducer orderProducer;

    public OrderController(OrderProducer orderProducer) {
        this.orderProducer = orderProducer;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        Long orderId = orderProducer.sendOrder(request);

        PlaceOrderResponse resp = new PlaceOrderResponse();
        resp.setOrderId(orderId);
        resp.setStatus("ACCEPTED");
        resp.setTradeIds(null); // trades happen async

        return ResponseEntity.ok(resp);
    }
}
