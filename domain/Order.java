package com.example.oms.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    @Enumerated(EnumType.STRING)
    private OrderSide side;

    private BigDecimal price;

    private BigDecimal quantity;

    @Column(name = "remaining_quantity")
    private BigDecimal remainingQuantity;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;
}
