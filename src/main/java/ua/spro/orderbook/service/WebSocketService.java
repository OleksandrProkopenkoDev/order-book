package ua.spro.orderbook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import ua.spro.orderbook.config.WebSocketConfig;

@Service
public class WebSocketService {

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
    webSocketClient.execute(webSocketHandler, webSocketConfig.getBinanceWsUrl());
  }
}
