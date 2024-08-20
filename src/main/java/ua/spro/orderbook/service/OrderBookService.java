package ua.spro.orderbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ua.spro.orderbook.model.OrderBook;

@Slf4j
@Service
public class OrderBookService {

  private static final String ORDER_BOOK_URL =
      "https://dapi.binance.com/dapi/v1/depth?symbol=BTCUSD_PERP&limit=1000";
  private final OrderBook orderBook = new OrderBook();
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public OrderBook getOrderBook() {
    return orderBook;
  }

  // Initialize the order book with a snapshot from the API
  public void initializeOrderBook() {
    orderBook.clear();
    Map<String, Object> snapshot = restTemplate.getForObject(ORDER_BOOK_URL, Map.class);

    if (snapshot != null) {
      // Process bids
      List<List<String>> bids = (List<List<String>>) snapshot.get("bids");
      processBids(bids);

      // Process asks
      List<List<String>> asks = (List<List<String>>) snapshot.get("asks");
      processAsks(asks);
      log.info("Order book snapshot: {}", orderBook);
    } else {
      log.error("Failed to retrieve order book snapshot.");
    }
  }

  // Update the order book with a WebSocket message
  public void updateOrderBookFromWebSocket(String message) {
    // Parse the WebSocket message (assumed to be JSON)
    Map<String, Object> updates = parseWebSocketMessage(message);

    // Process bids (b)
    List<List<String>> bids = (List<List<String>>) updates.get("b");
    processBids(bids);

    // Process asks (a)
    List<List<String>> asks = (List<List<String>>) updates.get("a");
    processAsks(asks);
    log.info("Updated order book from socket: {}", orderBook);
  }

  private void processAsks(List<List<String>> asks) {
    if (asks != null) {
      for (List<String> ask : asks) {
        double price = Double.parseDouble(ask.get(0));
        double quantity = Double.parseDouble(ask.get(1));
        orderBook.updateOrder(false, price, quantity);
      }
    }
  }

  private void processBids(List<List<String>> bids) {
    if (bids != null) {
      for (List<String> bid : bids) {
        double price = Double.parseDouble(bid.get(0));
        double quantity = Double.parseDouble(bid.get(1));
        orderBook.updateOrder(true, price, quantity);
      }
    }
  }

  private Map<String, Object> parseWebSocketMessage(String message) {
    try {
      return objectMapper.readValue(message, Map.class);
    } catch (IOException e) {
      log.error("Failed to parse WebSocket message: {}", message, e);
      return Map.of(); // Return an empty map in case of parsing failure
    }
  }
}
