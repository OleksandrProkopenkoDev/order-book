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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ua.spro.orderbook.model.OrderBookResponse;

@Service
public class OrderBookService {

  private static final Logger log = LoggerFactory.getLogger(OrderBookService.class);

  private static final String ORDER_BOOK_URL =
      "https://dapi.binance.com/dapi/v1/depth?symbol=BTCUSD_PERP&limit=1000";
  public static final String BID = "bid";
  public static final String ASK = "ask";
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private OrderBookResponse latestOrderBook;
  private long lastUpdateId;
  private long previousUpdateId = 0;

  // Maps to store bids and asks, using price as the key and quantity as the value
  private final Map<String, String> bidsMap = new HashMap<>();
  private final Map<String, String> asksMap = new HashMap<>();

  public OrderBookResponse getLatestOrderBook() {
    return latestOrderBook;
  }
  
  public void initializeOrderBook() {
    OrderBookResponse snapshot = getFromBinance();
    if (snapshot != null) {
      lastUpdateId = snapshot.lastUpdateId();
      previousUpdateId = 0;

      String symbol = snapshot.symbol();
      String pair = snapshot.pair();

      // Convert bids and asks JsonNode to List<List<String>>
      List<List<String>> bids = convertJsonNodeToListOfLists(snapshot.bids());
      List<List<String>> asks = convertJsonNodeToListOfLists(snapshot.asks());

      // Initialize the bids and asks maps
      bidsMap.clear();
      asksMap.clear();

      bids.forEach(bid -> bidsMap.put(bid.getFirst(), bid.get(1)));
      asks.forEach(ask -> asksMap.put(ask.getFirst(), ask.get(1)));

      // Convert the map to list of lists using streams
      List<List<String>> bidsList = getBidsListFromMap();
      List<List<String>> asksList = getAsksListFromMap();

      JsonNode bidsNode = objectMapper.valueToTree(bidsList);
      JsonNode asksNode = objectMapper.valueToTree(asksList);

      long messageTime;
      long transactionTime = messageTime = System.currentTimeMillis();

      latestOrderBook =
          new OrderBookResponse(
              lastUpdateId, messageTime, transactionTime, symbol, pair, bidsNode, asksNode);

      log.info("Order book initialized with snapshot: {}", latestOrderBook);
    } else {
      log.error("Failed to retrieve order book snapshot.");
    }
  }


  public void updateOrderBookFromWebSocket(JsonNode root) {
    try {
      long u = root.get("u").asLong();
      long U = root.get("U").asLong();
      long pu = root.get("pu").asLong();

      if (u < lastUpdateId) {
        log.debug("u({}) < lastUpdateId({})", u, lastUpdateId);
        return;
      }

      if (previousUpdateId != 0 && pu != previousUpdateId) {
        log.debug("pu({}) != previousUpdateId({})", pu, previousUpdateId);
        initializeOrderBook();
        return;
      }

      if (previousUpdateId == 0) {
        if (U <= lastUpdateId) {
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
      List<List<String>> bidsList = getBidsListFromMap();
      List<List<String>> asksList = getAsksListFromMap();

      JsonNode bidsNode = objectMapper.valueToTree(bidsList);
      JsonNode asksNode = objectMapper.valueToTree(asksList);

      latestOrderBook =
          new OrderBookResponse(
              previousUpdateId, messageTime, transactionTime, symbol, pair, bidsNode, asksNode);

      log.info("Order book updated: {}", latestOrderBook);
    } catch (Exception e) {
      log.error("Failed to process event", e);
    }
  }

  private List<List<String>> getBidsListFromMap() {
    return bidsMap.entrySet().stream()
        .sorted((entry1, entry2) -> Double.compare(Double.parseDouble(entry2.getKey()), Double.parseDouble(entry1.getKey())))
        .map(entry -> List.of(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private List<List<String>> getAsksListFromMap() {
    return asksMap.entrySet().stream()
        .sorted(Comparator.comparingDouble(entry -> Double.parseDouble(entry.getKey())))
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
    ResponseEntity<OrderBookResponse> response =
        restTemplate.exchange(
            ORDER_BOOK_URL,
            HttpMethod.GET,
            HttpEntity.EMPTY,
            new ParameterizedTypeReference<>() {});
    return response.getBody();
  }
}
