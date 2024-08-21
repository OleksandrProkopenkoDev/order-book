package ua.spro.orderbook.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ua.spro.orderbook.service.WebSocketHandlerImpl;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Value("${binance.ws.url}")
  private String binanceWsUrl;

  @Value("${binance.http.url}")
  private String binanceHttpUrl;


  private final WebSocketHandlerImpl webSocketHandler;

  @Autowired
  public WebSocketConfig(WebSocketHandlerImpl webSocketHandler) {
    this.webSocketHandler = webSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(webSocketHandler, "/binance");
  }

  public String getBinanceWsUrl() {
    return binanceWsUrl;
  }

  public String getBinanceHttpUrl() {
    return binanceHttpUrl;
  }
}
