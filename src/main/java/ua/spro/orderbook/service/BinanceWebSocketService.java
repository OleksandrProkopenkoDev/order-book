package ua.spro.orderbook.service;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Service
public class BinanceWebSocketService extends TextWebSocketHandler {

  private final OrderBookService orderBookService;
  private final WebSocketClient webSocketClient;

  private static final String BINANCE_WS_URL =
      "wss://dstream.binance.com/ws/btcusd_perp@depth@500ms";

  @Autowired
  public BinanceWebSocketService(OrderBookService orderBookService) {
    this.orderBookService = orderBookService;
    this.webSocketClient = new StandardWebSocketClient();
    connect();
  }

  private void connect() {
    try {
      webSocketClient.doHandshake(this, BINANCE_WS_URL).get();
      log.info("WebSocket connection established to Binance.");
    } catch (Exception e) {
      log.error("Failed to connect to WebSocket", e);
    }
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
    log.info("Connected to Binance WebSocket.");
    // Initialize the order book once the connection is established
    orderBookService.initializeOrderBook();

    // Send a subscription message to the WebSocket server
    String subscribeMessage = """
    {
      "method": "SUBSCRIBE",
      "params": ["btcusd_perp@depth@500ms"],
      "id": 1
    }
    """;
    try {
      session.sendMessage(new TextMessage(subscribeMessage));
      log.info("Subscription message sent: {}", subscribeMessage);
    } catch (IOException e) {
      log.error("Failed to send subscription message", e);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    log.warn("WebSocket connection closed. Attempting to reconnect...");
    connect(); // Reconnect logic
  }
}
