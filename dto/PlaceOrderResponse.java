package com.example.oms.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderResponse {
    private Long orderId;
    private String status;
    private List<Long> tradeIds;
}
