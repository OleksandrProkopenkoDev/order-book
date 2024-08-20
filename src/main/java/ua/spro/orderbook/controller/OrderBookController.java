package ua.spro.orderbook.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.spro.orderbook.model.OrderBook;
import ua.spro.orderbook.service.OrderBookService;

@RestController
@RequestMapping("/api/orderbook")
public class OrderBookController {

  private final OrderBookService orderBookService;

  @Autowired
  public OrderBookController(OrderBookService orderBookService) {
    this.orderBookService = orderBookService;
  }

  @GetMapping
  public ResponseEntity<Map<String, List<List<String>>>> getOrderBook() {
    OrderBook orderBook = orderBookService.getOrderBook();

    // Convert bids and asks to the required format
    List<List<String>> bids =
        orderBook.getBids().entrySet().stream()
            .map(entry -> List.of(entry.getKey().toString(), entry.getValue().toString()))
            .toList();

    List<List<String>> asks =
        orderBook.getAsks().entrySet().stream()
            .map(entry -> List.of(entry.getKey().toString(), entry.getValue().toString()))
            .toList();

    // Construct the response similar to Binance API format
    Map<String, List<List<String>>> response =
        Map.of(
            "bids", bids,
            "asks", asks);

    return ResponseEntity.ok(response);
  }
}
