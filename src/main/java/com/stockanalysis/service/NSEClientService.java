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
public class NSEClientService {
    private static final String NSE_BASE_URL = "https://www.nseindia.com/api/";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public Map<String, Object> getStockQuote(String symbol) throws IOException {
        String url = NSE_BASE_URL + "quote-equity?symbol=" + symbol;

        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.addHeader("Accept", "application/json");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        try {
            String response = httpClient.execute(request, response1 -> {
                if (response1.getCode() != 200) {
                    throw new IOException("Failed to fetch NSE data: HTTP error code " + response1.getCode());
                }
                return new String(response1.getEntity().getContent().readAllBytes());
            });

            JsonNode root = objectMapper.readTree(response);
            Map<String, Object> stockData = new HashMap<>();

            JsonNode info = root.path("info");
            stockData.put("symbol", symbol);
            stockData.put("currentPrice", parseBigDecimal(info.path("lastPrice").asText()));
            stockData.put("dayHigh", parseBigDecimal(info.path("dayHigh").asText()));
            stockData.put("dayLow", parseBigDecimal(info.path("dayLow").asText()));
            stockData.put("previousClose", parseBigDecimal(info.path("previousClose").asText()));
            stockData.put("change", parseBigDecimal(info.path("change").asText()));
            stockData.put("changePercent", parseBigDecimal(info.path("pChange").asText()));
            stockData.put("volume", info.path("totalTradedVolume").asLong());
            stockData.put("tradedValue", parseBigDecimal(info.path("totalTradedValue").asText()));

            return stockData;
        } catch (Exception e) {
            throw new IOException("Error fetching NSE data: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getAllStockDetails() throws IOException {
        String url = NSE_BASE_URL + "equity-stockIndices?index=NIFTY%2050";

        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.addHeader("Accept", "application/json");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        List<Map<String, Object>> allStocks = new ArrayList<>();

        try {
            String response = httpClient.execute(request, response1 -> {
                if (response1.getCode() != 200) {
                    throw new IOException("Failed to fetch NSE market data: HTTP error code " + response1.getCode());
                }
                return new String(response1.getEntity().getContent().readAllBytes());
            });

            JsonNode root = objectMapper.readTree(response);
            JsonNode stocks = root.path("data");

            for (JsonNode stock : stocks) {
                Map<String, Object> stockData = new HashMap<>();
                stockData.put("symbol", stock.path("symbol").asText());
                stockData.put("currentPrice", parseBigDecimal(stock.path("lastPrice").asText()));
                stockData.put("change", parseBigDecimal(stock.path("change").asText()));
                stockData.put("changePercent", parseBigDecimal(stock.path("pChange").asText()));
                stockData.put("volume", stock.path("totalTradedVolume").asLong());
                allStocks.add(stockData);
            }
        } catch (Exception e) {
            throw new IOException("Error fetching NSE market data: " + e.getMessage(), e);
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
