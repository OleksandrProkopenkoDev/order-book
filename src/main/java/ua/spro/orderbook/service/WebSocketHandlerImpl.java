package ua.spro.orderbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public class WebSocketHandlerImpl implements WebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(WebSocketHandlerImpl.class);
  private final OrderBookService orderBookService;
  private final ObjectMapper objectMapper;
  private final Runnable reconnect;

  public WebSocketHandlerImpl(
      OrderBookService orderBookService, ObjectMapper objectMapper, @Lazy Runnable reconnect) {
    this.orderBookService = orderBookService;
    this.objectMapper = objectMapper;
    this.reconnect = reconnect;
  }

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) {
    log.info("Connected to Binance WebSocket.");
    orderBookService.initializeOrderBook();
  }

  @Override
  public void handleMessage(
      @NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message) {
    try {
      // Parse the incoming message
      JsonNode jsonNode = objectMapper.readTree((String) message.getPayload());

      // Check the event type field
      if (jsonNode.has("e") && "depthUpdate".equals(jsonNode.get("e").asText())) {
        handleDepthUpdate(jsonNode);
      } else if (jsonNode.has("result")) {
        handleSubscriptionConfirmation(jsonNode);
      } else {
        log.warn("Received unexpected message: {}", message);
      }
    } catch (Exception e) {
      log.error("Failed to process message: {}", message, e);
    }
  }

  @Override
  public void handleTransportError(
      @NonNull WebSocketSession session, @NonNull Throwable exception) {}

  @Override
  public void afterConnectionClosed(
      @NonNull WebSocketSession session, @NonNull CloseStatus status) {
    log.warn("WebSocket connection closed. Attempting to reconnect...");
    reconnect.run();
  }

  @Override
  public boolean supportsPartialMessages() {
    return false;
  }

  private void handleDepthUpdate(JsonNode jsonNode) {
    log.info("Received depth update: {}", jsonNode.toString());
    orderBookService.updateOrderBookFromWebSocket(jsonNode);
  }

  private void handleSubscriptionConfirmation(JsonNode jsonNode) {
    log.info("Subscription confirmed: {}", jsonNode.toString());
  }
}
