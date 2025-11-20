package com.example.oms.dto;

import com.example.oms.domain.OrderSide;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaceOrderRequest {
    private String symbol;
    private OrderSide side;
    private BigDecimal price;
    private BigDecimal quantity;
}
