package com.example.oms.engine;

import com.example.oms.domain.*;
import com.example.oms.repository.OrderRepository;
import com.example.oms.repository.TradeRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class MatchingEngine {

    private final StringRedisTemplate redisTemplate;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;

    public MatchingEngine(StringRedisTemplate redisTemplate,
                          OrderRepository orderRepository,
                          TradeRepository tradeRepository) {
        this.redisTemplate = redisTemplate;
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
    }

    /**
     * Match an incoming order against the order book.
     * The order is assumed to already be saved and have a non-null ID.
     */
    public List<Trade> match(Order incoming) {
        List<Trade> trades = new ArrayList<>();

        if (incoming.getSide() == OrderSide.BUY) {
            matchBuy(incoming, trades);
        } else {
            matchSell(incoming, trades);
        }

        orderRepository.save(incoming);
        trades.forEach(tradeRepository::save);
        return trades;
    }

    private void matchBuy(Order buy, List<Trade> trades) {
        String sellBookKey = "orderbook:" + buy.getSymbol() + ":SELL";

        while (buy.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
            Set<String> candidates = redisTemplate.opsForZSet()
                    .range(sellBookKey, 0, 0); // best (lowest price) sell

            if (candidates == null || candidates.isEmpty()) break;

            String bestSellIdStr = candidates.iterator().next();
            Long bestSellId = Long.valueOf(bestSellIdStr);

            Order sell = orderRepository.findById(bestSellId).orElse(null);
            if (sell == null || sell.getStatus() == OrderStatus.FILLED) {
                redisTemplate.opsForZSet().remove(sellBookKey, bestSellIdStr);
                continue;
            }

            // Price check: buy.price >= sell.price
            if (buy.getPrice().compareTo(sell.getPrice()) < 0) break;

            BigDecimal tradedQty = buy.getRemainingQuantity().min(sell.getRemainingQuantity());
            BigDecimal tradePrice = sell.getPrice();

            buy.setRemainingQuantity(buy.getRemainingQuantity().subtract(tradedQty));
            sell.setRemainingQuantity(sell.getRemainingQuantity().subtract(tradedQty));

            if (sell.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                sell.setStatus(OrderStatus.FILLED);
                redisTemplate.opsForZSet().remove(sellBookKey, bestSellIdStr);
            } else {
                sell.setStatus(OrderStatus.PARTIALLY_FILLED);
            }

            if (buy.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                buy.setStatus(buy.getStatus() == OrderStatus.NEW
                        ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED);
            } else {
                buy.setStatus(OrderStatus.PARTIALLY_FILLED);
            }

            orderRepository.save(sell);

            Trade trade = new Trade();
            trade.setBuyOrderId(buy.getId());
            trade.setSellOrderId(sell.getId());
            trade.setSymbol(buy.getSymbol());
            trade.setPrice(tradePrice);
            trade.setQuantity(tradedQty);
            trade.setTradedAt(Instant.now());
            trades.add(trade);

            if (buy.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) break;
        }

        // If still open, put on order book
        if (buy.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0 &&
            (buy.getStatus() == null || buy.getStatus() == OrderStatus.NEW)) {

            buy.setStatus(OrderStatus.NEW);
            String buyBookKey = "orderbook:" + buy.getSymbol() + ":BUY";
            // We want highest price first → use negative price as score
            double score = buy.getPrice().negate().doubleValue();
            redisTemplate.opsForZSet().add(buyBookKey, String.valueOf(buy.getId()), score);
        }
    }

    private void matchSell(Order sell, List<Trade> trades) {
        String buyBookKey = "orderbook:" + sell.getSymbol() + ":BUY";

        while (sell.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
            Set<String> candidates = redisTemplate.opsForZSet()
                    .range(buyBookKey, 0, 0); // best (highest price) buy via negative score

            if (candidates == null || candidates.isEmpty()) break;

            String bestBuyIdStr = candidates.iterator().next();
            Long bestBuyId = Long.valueOf(bestBuyIdStr);

            Order buy = orderRepository.findById(bestBuyId).orElse(null);
            if (buy == null || buy.getStatus() == OrderStatus.FILLED) {
                redisTemplate.opsForZSet().remove(buyBookKey, bestBuyIdStr);
                continue;
            }

            // Price check: buy.price >= sell.price
            if (buy.getPrice().compareTo(sell.getPrice()) < 0) break;

            BigDecimal tradedQty = sell.getRemainingQuantity().min(buy.getRemainingQuantity());
            BigDecimal tradePrice = buy.getPrice();

            sell.setRemainingQuantity(sell.getRemainingQuantity().subtract(tradedQty));
            buy.setRemainingQuantity(buy.getRemainingQuantity().subtract(tradedQty));

            if (buy.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                buy.setStatus(OrderStatus.FILLED);
                redisTemplate.opsForZSet().remove(buyBookKey, bestBuyIdStr);
            } else {
                buy.setStatus(OrderStatus.PARTIALLY_FILLED);
            }

            if (sell.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                sell.setStatus(sell.getStatus() == OrderStatus.NEW
                        ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED);
            } else {
                sell.setStatus(OrderStatus.PARTIALLY_FILLED);
            }

            orderRepository.save(buy);

            Trade trade = new Trade();
            trade.setBuyOrderId(buy.getId());
            trade.setSellOrderId(sell.getId());
            trade.setSymbol(sell.getSymbol());
            trade.setPrice(tradePrice);
            trade.setQuantity(tradedQty);
            trade.setTradedAt(Instant.now());
            trades.add(trade);

            if (sell.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) break;
        }

        if (sell.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0 &&
            (sell.getStatus() == null || sell.getStatus() == OrderStatus.NEW)) {

            sell.setStatus(OrderStatus.NEW);
            String sellBookKey = "orderbook:" + sell.getSymbol() + ":SELL";
            double score = sell.getPrice().doubleValue(); // lower first
            redisTemplate.opsForZSet().add(sellBookKey, String.valueOf(sell.getId()), score);
        }
    }
}

