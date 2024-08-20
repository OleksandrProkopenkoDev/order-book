package ua.spro.orderbook.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import ua.spro.orderbook.model.OrderBook;

@Service
public class OrderBookService {

  private static final String ORDER_BOOK_URL = "https://dapi.binance.com/dapi/v1/depth?symbol=BTCUSD_PERP&limit=1000";
  private final OrderBook orderBook = new OrderBook();
  private final RestTemplate restTemplate = new RestTemplate();

  public OrderBook getOrderBook() {
    return orderBook;
  }

  // Initialize the order book with a snapshot from the API
  public void initializeOrderBook() {
    orderBook.clear();
    Map<String, Object> snapshot = restTemplate.getForObject(ORDER_BOOK_URL, Map.class);

    // Process bids
    List<List<String>> bids = (List<List<String>>) snapshot.get("bids");
    for (List<String> bid : bids) {
      double price = Double.parseDouble(bid.get(0));
      double quantity = Double.parseDouble(bid.get(1));
      orderBook.updateOrder(true, price, quantity);
    }

    // Process asks
    List<List<String>> asks = (List<List<String>>) snapshot.get("asks");
    for (List<String> ask : asks) {
      double price = Double.parseDouble(ask.get(0));
      double quantity = Double.parseDouble(ask.get(1));
      orderBook.updateOrder(false, price, quantity);
    }
  }

  // Update the order book with a WebSocket message
  public void updateOrderBookFromWebSocket(String message) {
    // Parse the WebSocket message (assumed to be JSON)
    // Example: {"b": [["price1", "quantity1"], ["price2", "quantity2"]], "a": [["price1", "quantity1"], ...]}
    Map<String, Object> updates = parseWebSocketMessage(message);

    // Process bids (b)
    List<List<String>> bids = (List<List<String>>) updates.get("b");
    for (List<String> bid : bids) {
      double price = Double.parseDouble(bid.get(0));
      double quantity = Double.parseDouble(bid.get(1));
      orderBook.updateOrder(true, price, quantity);
    }

    // Process asks (a)
    List<List<String>> asks = (List<List<String>>) updates.get("a");
    for (List<String> ask : asks) {
      double price = Double.parseDouble(ask.get(0));
      double quantity = Double.parseDouble(ask.get(1));
      orderBook.updateOrder(false, price, quantity);
    }
  }

  private Map<String, Object> parseWebSocketMessage(String message) {
    // You can use Jackson ObjectMapper or other JSON parsers to parse the message
    // For simplicity, returning an empty map here
    return Map.of(); // Replace with actual JSON parsing logic
  }
}

