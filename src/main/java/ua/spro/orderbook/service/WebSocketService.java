package ua.spro.orderbook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Service
public class WebSocketService {

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
    webSocketClient.execute(webSocketHandler, BINANCE_WS_URL);
  }
}
