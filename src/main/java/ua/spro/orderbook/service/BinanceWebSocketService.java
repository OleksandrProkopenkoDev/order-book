package ua.spro.orderbook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Service
public class BinanceWebSocketService extends TextWebSocketHandler {

  private final OrderBookService orderBookService;

  @Autowired
  public BinanceWebSocketService(OrderBookService orderBookService) {
    this.orderBookService = orderBookService;
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) {
    String payload = message.getPayload();
    log.info("Received text message: {}", payload);
    // Pass the WebSocket message to the OrderBookService for processing
    orderBookService.updateOrderBookFromWebSocket(payload);
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    System.out.println("Connected to Binance WebSocket.");
    // Initialize the order book once the connection is established
    orderBookService.initializeOrderBook();
  }
}
