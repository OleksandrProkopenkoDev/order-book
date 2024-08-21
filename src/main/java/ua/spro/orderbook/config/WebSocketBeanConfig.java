package ua.spro.orderbook.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import ua.spro.orderbook.service.OrderBookService;
import ua.spro.orderbook.service.WebSocketHandlerImpl;
import ua.spro.orderbook.service.WebSocketService;

@Configuration
public class WebSocketBeanConfig {

  @Bean
  public WebSocketHandlerImpl webSocketHandlerImpl(
      OrderBookService orderBookService,
      ObjectMapper objectMapper,
      @Lazy WebSocketService webSocketService) {
    return new WebSocketHandlerImpl(orderBookService, objectMapper, webSocketService::connect);
  }
}
