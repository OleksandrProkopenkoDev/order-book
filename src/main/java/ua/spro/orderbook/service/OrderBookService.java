package ua.spro.orderbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ua.spro.orderbook.model.OrderBookResponse;

@Service
public class OrderBookService {

  private static final Logger log = LoggerFactory.getLogger(OrderBookService.class);

  private static final String ORDER_BOOK_URL =
      "https://dapi.binance.com/dapi/v1/depth?symbol=BTCUSD_PERP&limit=1000";
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private OrderBookResponse latestOrderBook;

  public OrderBookResponse getLatestOrderBook() {
    return latestOrderBook;
  }

  // Initialize the order book with a snapshot from the API
  public void initializeOrderBook() {
    Map<String, Object> snapshot = restTemplate.getForObject(ORDER_BOOK_URL, Map.class);

    if (snapshot != null) {
      long lastUpdateId = ((Number) snapshot.get("lastUpdateId")).longValue();
      String symbol = "BTCUSD_PERP"; // Extract symbol from the URL or response if available
      String pair = "BTCUSD"; // Same for pair

      // Process bids and asks
      List<List<String>> bids = (List<List<String>>) snapshot.get("bids");
      List<List<String>> asks = (List<List<String>>) snapshot.get("asks");

      // Convert bids and asks to JSON nodes or appropriate format
      JsonNode bidsNode = objectMapper.valueToTree(bids);
      JsonNode asksNode = objectMapper.valueToTree(asks);

      long messageTime = System.currentTimeMillis(); // Placeholder for message and transaction time
      long transactionTime = messageTime;

      // Create the OrderBookResponse
      OrderBookResponse response =
          new OrderBookResponse(
              lastUpdateId,  messageTime, transactionTime, symbol, pair, bidsNode, asksNode);

      latestOrderBook = response; // Store the snapshot as the latest order book

      log.info("Order book initialized with snapshot: {}", response);
    } else {
      log.error("Failed to retrieve order book snapshot.");
    }
  }

  // Update the order book with a WebSocket message
  public void updateOrderBookFromWebSocket(JsonNode root) {

    try {
      String symbol = root.get("s").asText();
      String pair = root.get("ps").asText();
      long messageTime = root.get("E").asLong();
      long transactionTime = root.get("T").asLong();

      JsonNode bids = root.get("b");
      JsonNode asks = root.get("a");

      // Create the output structure
      OrderBookResponse response =
          new OrderBookResponse(
              root.get("u").asLong(), // use the `u` field as `lastUpdateId`
              messageTime,
              transactionTime,
              symbol,
              pair,
              // Convert bids and asks
              bids,
              asks);
      latestOrderBook = response; // Store the latest order book
      // Log or send the response as required
      log.info("Order book update: {}", response);
    } catch (Exception e) {
      log.error("Failed to parse and update order book", e);
    }
  }
}
