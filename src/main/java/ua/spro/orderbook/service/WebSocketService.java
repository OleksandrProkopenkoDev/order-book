package ua.spro.orderbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Service
public class WebSocketService extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);

  private final OrderBookService orderBookService;
  private final WebSocketClient webSocketClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String BINANCE_WS_URL =
      "wss://dstream.binance.com/ws/btcusd_perp@depth@500ms";

  @Autowired
  public WebSocketService(OrderBookService orderBookService) {
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
    try {
      // Parse the incoming message
      JsonNode jsonNode = objectMapper.readTree(message.getPayload());

      // Check the event type field
      if (jsonNode.has("e") && "depthUpdate".equals(jsonNode.get("e").asText())) {
        // Process depth update message
        handleDepthUpdate(jsonNode);
      } else if (jsonNode.has("result")) {
        // Handle subscription confirmation message
        handleSubscriptionConfirmation(jsonNode);
      } else {
        // Log unexpected message types
        log.warn("Received unexpected message: {}", message);
      }
    } catch (Exception e) {
      log.error("Failed to process message: {}", message, e);
    }


  }

  private void handleDepthUpdate(JsonNode jsonNode) {
    log.info("Received text message: {}", jsonNode.toString());
    // Pass the WebSocket message to the OrderBookService for processing
    orderBookService.updateOrderBookFromWebSocket(jsonNode);
  }

  private void handleSubscriptionConfirmation(JsonNode jsonNode) {
    // Handle the subscription confirmation message
    log.info("Subscription confirmed: {}", jsonNode.toString());
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
