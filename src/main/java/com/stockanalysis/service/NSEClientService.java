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

            // Extract data from the correct paths in the JSON structure
            JsonNode info = root.path("info");
            JsonNode priceInfo = root.path("priceInfo");
            JsonNode metadata = root.path("metadata");

            stockData.put("symbol", info.path("symbol").asText());
            stockData.put("companyName", info.path("companyName").asText());
            stockData.put("industry", info.path("industry").asText());
            stockData.put("currentPrice", parseBigDecimal(priceInfo.path("lastPrice").asText()));
            stockData.put("open", parseBigDecimal(priceInfo.path("open").asText()));
            stockData.put("dayHigh", parseBigDecimal(priceInfo.path("intraDayHighLow").path("max").asText()));
            stockData.put("dayLow", parseBigDecimal(priceInfo.path("intraDayHighLow").path("min").asText()));
            stockData.put("previousClose", parseBigDecimal(priceInfo.path("previousClose").asText()));
            stockData.put("change", parseBigDecimal(priceInfo.path("change").asText()));
            stockData.put("changePercent", parseBigDecimal(priceInfo.path("pChange").asText()));
            stockData.put("volume", root.path("preOpenMarket").path("totalTradedVolume").asLong());
            stockData.put("vwap", parseBigDecimal(priceInfo.path("vwap").asText()));
            stockData.put("weekHigh", parseBigDecimal(priceInfo.path("weekHighLow").path("max").asText()));
            stockData.put("weekLow", parseBigDecimal(priceInfo.path("weekHighLow").path("min").asText()));
            stockData.put("upperCircuit", parseBigDecimal(priceInfo.path("upperCP").asText()));
            stockData.put("lowerCircuit", parseBigDecimal(priceInfo.path("lowerCP").asText()));
            stockData.put("lastUpdateTime", metadata.path("lastUpdateTime").asText());

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
