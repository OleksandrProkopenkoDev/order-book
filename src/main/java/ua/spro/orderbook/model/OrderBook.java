package ua.spro.orderbook.model;

import java.util.NavigableMap;
import java.util.TreeMap;

public class OrderBook {
  // TreeMap will automatically sort the orders by price
  private final NavigableMap<Double, Double> bids; // Price -> Quantity
  private final NavigableMap<Double, Double> asks; // Price -> Quantity

  public OrderBook() {
    // For bids, higher prices should come first, so reverse order
    this.bids = new TreeMap<>((a, b) -> Double.compare(b, a));
    // For asks, lower prices should come first, so natural order
    this.asks = new TreeMap<>();
  }

  public NavigableMap<Double, Double> getBids() {
    return bids;
  }

  public NavigableMap<Double, Double> getAsks() {
    return asks;
  }

  // Add or update an order in the order book
  public void updateOrder(boolean isBid, double price, double quantity) {
    NavigableMap<Double, Double> orders = isBid ? bids : asks;
    if (quantity == 0) {
      // Remove the order if the quantity is 0
      orders.remove(price);
    } else {
      // Add or update the order
      orders.put(price, quantity);
    }
  }

  // Clear the order book (e.g., when initializing)
  public void clear() {
    bids.clear();
    asks.clear();
  }

  @Override
  public String toString() {
    return "OrderBook{" +
        "asks=" + asks +
        ", bids=" + bids +
        '}';
  }
}

