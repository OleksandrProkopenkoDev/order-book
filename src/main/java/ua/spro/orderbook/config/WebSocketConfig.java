package ua.spro.orderbook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ua.spro.orderbook.service.WebSocketService;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final WebSocketService webSocketService;

  // Constructor injection for WebSocketService
  public WebSocketConfig(WebSocketService webSocketService) {
    this.webSocketService = webSocketService;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(webSocketService, "/binance").setAllowedOrigins("*");
  }
}

