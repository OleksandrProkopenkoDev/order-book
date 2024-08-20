package ua.spro.orderbook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ua.spro.orderbook.service.OrderBookService;

@Slf4j
@SpringBootApplication
public class OrderBookApplication {

  private final OrderBookService orderBookService;

  @Autowired
  public OrderBookApplication(OrderBookService orderBookService) {
    this.orderBookService = orderBookService;
  }

  public static void main(String[] args) {
    SpringApplication.run(OrderBookApplication.class, args);
  }

  @Bean
  CommandLineRunner run() {
    return (args) -> {
//      log.info("Initializing Order Book...");
//      orderBookService.initializeOrderBook();
//      log.info("Order Book initialized.");
    };
  }
}
