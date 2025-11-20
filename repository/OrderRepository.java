package com.example.oms.repository;

import com.example.oms.domain.Order;
import com.example.oms.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusIn(List<OrderStatus> statuses);
}
