package ua.spro.orderbook.service;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Service
public class WebSocketService {

  private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);
  private final WebSocketHandlerImpl webSocketHandler;
  private final WebSocketClient webSocketClient;

  private static final String BINANCE_WS_URL =
      "wss://dstream.binance.com/ws/btcusd_perp@depth@500ms";

  @Autowired
  public WebSocketService(WebSocketHandlerImpl webSocketHandler) {
    this.webSocketHandler = webSocketHandler;
    this.webSocketClient = new StandardWebSocketClient();
    connect();
  }

  public void connect() {
    CompletableFuture<WebSocketSession> sessionFuture =
        webSocketClient.execute(webSocketHandler, BINANCE_WS_URL);

    sessionFuture.whenComplete(
        (session, throwable) -> {
          if (throwable != null) {
            log.error("Failed to connect to WebSocket", throwable);
            connect();
          } else {
            log.info("WebSocket connection established to Binance.");
          }
        });
  }
}
