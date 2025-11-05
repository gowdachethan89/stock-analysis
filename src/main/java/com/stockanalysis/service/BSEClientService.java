package com.stockanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class BSEClientService {
    private static final String BSE_BASE_URL = "https://api.bseindia.com/BseIndiaAPI/api/";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public Map<String, Object> getStockQuote(String scripCode) throws IOException {
        String url = BSE_BASE_URL + "StockReachGraph/w?scripcode=" + scripCode;

        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.addHeader("Accept", "application/json");

        try {
            String response = httpClient.execute(request, response1 -> {
                if (response1.getCode() != 200) {
                    throw new IOException("Failed to fetch BSE data: HTTP error code " + response1.getCode());
                }
                return new String(response1.getEntity().getContent().readAllBytes());
            });

            JsonNode root = objectMapper.readTree(response);
            Map<String, Object> stockData = new HashMap<>();

            stockData.put("scripCode", scripCode);
            stockData.put("currentPrice", parseBigDecimal(root.path("CurrentPrice").asText()));
            stockData.put("dayHigh", parseBigDecimal(root.path("High").asText()));
            stockData.put("dayLow", parseBigDecimal(root.path("Low").asText()));
            stockData.put("previousClose", parseBigDecimal(root.path("PrevClose").asText()));
            stockData.put("change", parseBigDecimal(root.path("Change").asText()));
            stockData.put("changePercent", parseBigDecimal(root.path("PerChange").asText()));
            stockData.put("volume", root.path("Volume").asLong());
            stockData.put("tradedValue", parseBigDecimal(root.path("TurnOver").asText()));

            return stockData;
        } catch (Exception e) {
            throw new IOException("Error fetching BSE data: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getAllStockDetails() throws IOException {
        // BSE provides market data through different APIs
        // This is a sample implementation that would need to be adjusted based on actual BSE API access
        String url = BSE_BASE_URL + "homepage/GetMarketData";

        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.addHeader("Accept", "application/json");

        List<Map<String, Object>> allStocks = new ArrayList<>();

        try {
            String response = httpClient.execute(request, response1 -> {
                if (response1.getCode() != 200) {
                    throw new IOException("Failed to fetch BSE market data: HTTP error code " + response1.getCode());
                }
                return new String(response1.getEntity().getContent().readAllBytes());
            });

            JsonNode root = objectMapper.readTree(response);
            JsonNode stocks = root.path("Table");

            for (JsonNode stock : stocks) {
                Map<String, Object> stockData = new HashMap<>();
                stockData.put("scripCode", stock.path("scripcode").asText());
                stockData.put("symbol", stock.path("scripname").asText());
                stockData.put("currentPrice", parseBigDecimal(stock.path("ltradert").asText()));
                stockData.put("change", parseBigDecimal(stock.path("change").asText()));
                stockData.put("changePercent", parseBigDecimal(stock.path("pchange").asText()));
                allStocks.add(stockData);
            }
        } catch (Exception e) {
            throw new IOException("Error fetching BSE market data: " + e.getMessage(), e);
        }

        return allStocks;
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
