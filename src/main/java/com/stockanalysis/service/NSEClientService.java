package com.stockanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalysis.model.StockData;
import com.stockanalysis.repository.StockDataRepository;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class NSEClientService {
    private static final Logger logger = LoggerFactory.getLogger(NSEClientService.class);
    private static final String NSE_BASE_URL = "https://www.nseindia.com/api/";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final StockDataRepository stockDataRepository;

    public NSEClientService(StockDataRepository stockDataRepository) {
        this.stockDataRepository = stockDataRepository;
    }

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

            // Populate the map with stock data
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

            // Store the data in the database
            saveStockData(stockData);

            return stockData;
        } catch (Exception e) {
            throw new IOException("Error fetching NSE data: " + e.getMessage(), e);
        }
    }

    private void saveStockData(Map<String, Object> stockData) {
        StockData entity = new StockData();
        entity.setSymbol((String) stockData.get("symbol"));
        entity.setCompanyName((String) stockData.get("companyName"));
        entity.setIndustry((String) stockData.get("industry"));
        entity.setCurrentPrice((BigDecimal) stockData.get("currentPrice"));
        entity.setOpen((BigDecimal) stockData.get("open"));
        entity.setDayHigh((BigDecimal) stockData.get("dayHigh"));
        entity.setDayLow((BigDecimal) stockData.get("dayLow"));
        entity.setPreviousClose((BigDecimal) stockData.get("previousClose"));
        entity.setChange((BigDecimal) stockData.get("change"));
        entity.setChangePercent((BigDecimal) stockData.get("changePercent"));
        entity.setVolume((Long) stockData.get("volume"));
        entity.setVwap((BigDecimal) stockData.get("vwap"));
        entity.setWeekHigh((BigDecimal) stockData.get("weekHigh"));
        entity.setWeekLow((BigDecimal) stockData.get("weekLow"));
        entity.setUpperCircuit((BigDecimal) stockData.get("upperCircuit"));
        entity.setLowerCircuit((BigDecimal) stockData.get("lowerCircuit"));

        String lastUpdateTimeStr = (String) stockData.get("lastUpdateTime");
        if (lastUpdateTimeStr != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
            entity.setLastUpdateTime(LocalDateTime.parse(lastUpdateTimeStr, formatter));
        }

        stockDataRepository.save(entity);
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

                // Store each stock's data
                try {
                    getStockQuote(stock.path("symbol").asText());
                } catch (Exception e) {
                    logger.error("Failed to fetch quote for symbol: {}", stock.path("symbol").asText(), e);
                }

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
