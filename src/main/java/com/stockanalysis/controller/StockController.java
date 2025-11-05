package com.stockanalysis.controller;

import com.stockanalysis.model.StockData;
import com.stockanalysis.service.StockService;
import com.stockanalysis.service.NSEClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private NSEClientService nseClientService;

    @GetMapping("/{symbol}")
    public ResponseEntity<StockData> getStockData(@PathVariable String symbol) {
        StockData stockData = stockService.getStockData(symbol);
        return ResponseEntity.ok(stockData);
    }

    @GetMapping("/nse/{symbol}")
    public ResponseEntity<Map<String, Object>> getNSEStockData(@PathVariable String symbol) {
        try {
            Map<String, Object> nseData = nseClientService.getStockQuote(symbol);
            return ResponseEntity.ok(nseData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/nse/all")
    public ResponseEntity<List<Map<String, Object>>> getAllNSEStocks() {
        try {
            List<Map<String, Object>> allStocks = nseClientService.getAllStockDetails();
            return ResponseEntity.ok(allStocks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{symbol}/technical")
    public ResponseEntity<Map<String, Object>> getTechnicalIndicators(@PathVariable String symbol) {
        StockData stockData = stockService.getLatestStockData(symbol);
        if (stockData == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> technicalData = stockService.getTechnicalIndicators(stockData);
        return ResponseEntity.ok(technicalData);
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<StockData>> getStockHistory(
            @PathVariable String symbol,
            @RequestParam(required = false) LocalDateTime startDate) {
        if (startDate != null) {
            return ResponseEntity.ok(stockService.getHistoricalData(symbol, startDate));
        }
        return ResponseEntity.ok(stockService.getStockHistory(symbol));
    }

    @GetMapping("/tracked")
    public ResponseEntity<List<String>> getTrackedSymbols() {
        return ResponseEntity.ok(stockService.getAllTrackedSymbols());
    }
}