package ua.spro.orderbook.model;

import com.fasterxml.jackson.databind.JsonNode;

public record OrderBookResponse(
    long lastUpdateId,long E, long T, String symbol, String pair, JsonNode bids, JsonNode asks) {}
