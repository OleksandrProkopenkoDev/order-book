package ua.spro.orderbook.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.spro.orderbook.model.OrderBookResponse;
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
  public OrderBookResponse getOrderBook() {
    return orderBookService.getOrderBook();
  }
}
