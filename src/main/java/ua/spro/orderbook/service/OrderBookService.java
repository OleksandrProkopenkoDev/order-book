package ua.spro.orderbook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ua.spro.orderbook.config.WebSocketConfig;
import ua.spro.orderbook.model.OrderBookResponse;

@Service
public class OrderBookService {

  private static final Logger log = LoggerFactory.getLogger(OrderBookService.class);

  public static final String BID = "bid";
  public static final String ASK = "ask";
  public static final int MAX_LIMIT = 1000;
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private OrderBookResponse orderBook;
  private long lastUpdateId;
  private long previousUpdateId = 0;

  // Maps to store bids and asks, using price as the key and quantity as the value
  private final Map<String, String> bidsMap = new HashMap<>();
  private final Map<String, String> asksMap = new HashMap<>();

  private final WebSocketConfig webSocketConfig;

  public OrderBookService(@Lazy WebSocketConfig webSocketConfig) {
    this.webSocketConfig = webSocketConfig;
  }

  public OrderBookResponse getOrderBook(int limit) {
    return limitOrderBook(limit);
  }

  public void initializeOrderBookFromHttp() {
    OrderBookResponse snapshot = getFromBinance();
    if (snapshot != null) {

      orderBook = processAndFill(snapshot);

      log.info("Order book initialized with snapshot: {}", orderBook);
    } else {
      log.error("Failed to retrieve order book snapshot.");
    }
  }

  public void updateOrderBookFromWebSocket(JsonNode root) {
    try {
      long u = root.get("u").asLong(); // Final update ID in event
      long U = root.get("U").asLong(); // First update ID in event
      long pu = root.get("pu").asLong(); // Final update Id in last stream(ie `u` in last stream)

      if (isOutOfDate(u)) {
        log.debug("u({}) < lastUpdateId({})", u, lastUpdateId);
        return;
      }

      if (hasMissingMessages(pu)) {
        log.debug("pu({}) != previousUpdateId({})", pu, previousUpdateId);
        initializeOrderBookFromHttp();
        return;
      }

      if (isFirstProcessedEvent()) {
        if (isNewMessage(U)) {
          log.debug("U({}) <= lastUpdateId({})", U, lastUpdateId);
          processEvent(root);
        }
        return;
      }
      processEvent(root);
    } catch (Exception e) {
      log.error("Failed to update order book", e);
    }
  }

  private boolean isNewMessage(long U) {
    return U <= lastUpdateId;
  }

  private boolean isFirstProcessedEvent() {
    return previousUpdateId == 0;
  }

  private boolean hasMissingMessages(long pu) {
    return previousUpdateId != 0 && pu != previousUpdateId;
  }

  private boolean isOutOfDate(long u) {
    return u < lastUpdateId;
  }

  private OrderBookResponse processAndFill(OrderBookResponse snapshot) {

    lastUpdateId = snapshot.lastUpdateId();
    previousUpdateId = 0;

    String symbol = snapshot.symbol();
    String pair = snapshot.pair();

    List<List<String>> bids = convertJsonNodeToListOfLists(snapshot.bids());
    List<List<String>> asks = convertJsonNodeToListOfLists(snapshot.asks());

    // Initialize the bids and asks maps
    bidsMap.clear();
    asksMap.clear();

    bids.forEach(bid -> bidsMap.put(bid.getFirst(), bid.get(1)));
    asks.forEach(ask -> asksMap.put(ask.getFirst(), ask.get(1)));

    // Convert the map to list of lists
    List<List<String>> bidsList = getBidsListFromMap(MAX_LIMIT);
    List<List<String>> asksList = getAsksListFromMap(MAX_LIMIT);

    JsonNode bidsNode = objectMapper.valueToTree(bidsList);
    JsonNode asksNode = objectMapper.valueToTree(asksList);

    long messageTime;
    long transactionTime = messageTime = System.currentTimeMillis();

    return new OrderBookResponse(
        lastUpdateId, messageTime, transactionTime, symbol, pair, bidsNode, asksNode);
  }

  private List<List<String>> convertJsonNodeToListOfLists(JsonNode jsonNode) {
    return objectMapper.convertValue(jsonNode, new TypeReference<>() {});
  }

  private void processEvent(JsonNode root) {
    try {
      String symbol = root.get("s").asText();
      String pair = root.get("ps").asText();
      long messageTime = root.get("E").asLong();
      long transactionTime = root.get("T").asLong();
      previousUpdateId = root.get("u").asLong();

      JsonNode bids = root.get("b");
      JsonNode asks = root.get("a");

      updateOrderBookLevels(bids, BID);
      updateOrderBookLevels(asks, ASK);

      // Convert the map to list of lists
      List<List<String>> bidsList = getBidsListFromMap(MAX_LIMIT);
      List<List<String>> asksList = getAsksListFromMap(MAX_LIMIT);

      JsonNode bidsNode = objectMapper.valueToTree(bidsList);
      JsonNode asksNode = objectMapper.valueToTree(asksList);

      orderBook =
          new OrderBookResponse(
              previousUpdateId, messageTime, transactionTime, symbol, pair, bidsNode, asksNode);

      log.info("Order book updated: {}", orderBook);
    } catch (Exception e) {
      log.error("Failed to process event", e);
    }
  }

  private OrderBookResponse limitOrderBook(int limit) {
    // Limit the number of records in bids and asks lists
    List<List<String>> limitedBids = getBidsListFromMap(limit);
    List<List<String>> limitedAsks = getAsksListFromMap(limit);

    JsonNode limitedBidsNode = objectMapper.valueToTree(limitedBids);
    JsonNode limitedAsksNode = objectMapper.valueToTree(limitedAsks);

    // Construct a new OrderBookResponse with the limited bids and asks
    return new OrderBookResponse(
        orderBook.lastUpdateId(),
        orderBook.E(),
        orderBook.T(),
        orderBook.symbol(),
        orderBook.pair(),
        limitedBidsNode,
        limitedAsksNode
    );
  }

  // Helper methods to apply the limit
  private List<List<String>> getBidsListFromMap(int limit) {
    return bidsMap.entrySet().stream()
        .sorted((entry1, entry2) -> Double.compare(Double.parseDouble(entry2.getKey()), Double.parseDouble(entry1.getKey())))
        .limit(limit)  // Apply limit here
        .map(entry -> List.of(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private List<List<String>> getAsksListFromMap(int limit) {
    return asksMap.entrySet().stream()
        .sorted(Comparator.comparingDouble(entry -> Double.parseDouble(entry.getKey())))
        .limit(limit)  // Apply limit here
        .map(entry -> List.of(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private void updateOrderBookLevels(JsonNode levels, String type) {
    levels.forEach(
        level -> {
          String price = level.get(0).asText();
          String quantity = level.get(1).asText();

          if (quantity.equals("0")) {
            removeOrderBookLevel(price, type);
          } else {
            updateOrderBookLevel(price, quantity, type);
          }
        });
  }

  private void removeOrderBookLevel(String price, String type) {
    if (type.equals(BID)) {
      bidsMap.remove(price);
    } else {
      asksMap.remove(price);
    }
    log.debug("Removed {} level at price: {}", type, price);
  }

  private void updateOrderBookLevel(String price, String quantity, String type) {
    if (type.equals(BID)) {
      bidsMap.put(price, quantity);
    } else {
      asksMap.put(price, quantity);
    }
    log.debug("Updated {} level at price: {}, quantity: {}", type, price, quantity);
  }

  private OrderBookResponse getFromBinance() {
    ResponseEntity<OrderBookResponse> response = null;
    try {
      response =
          restTemplate.exchange(
              webSocketConfig.getBinanceHttpUrl(),
              HttpMethod.GET,
              HttpEntity.EMPTY,
              new ParameterizedTypeReference<>() {});
    } catch (RestClientException e) {
      // Log the error message before shutting down
      log.error(
          "Error occurred while fetching data from Binance: {} | Error: {}",
          webSocketConfig.getBinanceHttpUrl(),
          e.getMessage());
      // Shut down the application
      System.exit(1);
    }
    log.info("Fetched OrderBook from : {}", webSocketConfig.getBinanceHttpUrl());
    return response.getBody();
  }
}
