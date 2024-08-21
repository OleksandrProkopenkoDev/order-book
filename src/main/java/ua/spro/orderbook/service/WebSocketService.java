package ua.spro.orderbook.service;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import ua.spro.orderbook.config.WebSocketConfig;

@Service
public class WebSocketService {

  private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);
  private final WebSocketHandlerImpl webSocketHandler;
  private final WebSocketClient webSocketClient;
  private final WebSocketConfig webSocketConfig;

  @Autowired
  public WebSocketService(WebSocketHandlerImpl webSocketHandler, WebSocketConfig webSocketConfig) {
    this.webSocketHandler = webSocketHandler;
    this.webSocketConfig = webSocketConfig;
    this.webSocketClient = new StandardWebSocketClient();
    connect();
  }

  public void connect() {
    CompletableFuture<WebSocketSession> sessionFuture =
        webSocketClient.execute(webSocketHandler, webSocketConfig.getBinanceWsUrl());

    sessionFuture.whenComplete(
        (session, throwable) -> {
          if (throwable != null) {
            log.error("Failed to connect to WebSocket : {}", webSocketConfig.getBinanceWsUrl());
            // Shut down the application
            System.exit(1);
          }
        });
  }
}
