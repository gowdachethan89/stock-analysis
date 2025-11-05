package com.stockanalysis.repository;

import com.stockanalysis.model.StockData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, Long> {
    List<StockData> findBySymbolOrderByLastUpdateTimeDesc(String symbol);
    StockData findFirstBySymbolOrderByLastUpdateTimeDesc(String symbol);

    @Query("SELECT s FROM StockData s WHERE s.symbol = ?1 AND s.lastUpdateTime >= ?2")
    List<StockData> findHistoricalData(String symbol, LocalDateTime startDate);

    @Query("SELECT s FROM StockData s WHERE s.symbol = ?1 AND s.lastUpdateTime >= ?2 AND s.lastUpdateTime <= ?3")
    List<StockData> findHistoricalDataBetweenDates(String symbol, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT DISTINCT s.symbol FROM StockData s")
    List<String> findAllTrackedSymbols();

    @Query("SELECT s FROM StockData s WHERE s.lastUpdateTime >= ?1")
    List<StockData> findAllRecentData(LocalDateTime since);

    @Query("SELECT s FROM StockData s WHERE s.symbol = ?1 ORDER BY s.lastUpdateTime DESC LIMIT ?2")
    List<StockData> findLastNRecords(String symbol, int n);

    @Query("SELECT s FROM StockData s WHERE s.currentPrice >= ?1")
    List<StockData> findStocksAbovePrice(BigDecimal price);

    @Query("SELECT s FROM StockData s WHERE s.changePercent >= ?1 ORDER BY s.changePercent DESC")
    List<StockData> findTopGainers(BigDecimal minChangePercent);

    @Query("SELECT s FROM StockData s WHERE s.changePercent <= ?1 ORDER BY s.changePercent ASC")
    List<StockData> findTopLosers(BigDecimal maxChangePercent);

    @Query("SELECT s FROM StockData s WHERE s.volume >= ?1 ORDER BY s.volume DESC")
    List<StockData> findHighVolumeStocks(Long minVolume);

    // Technical Analysis related queries
    @Query("SELECT s FROM StockData s WHERE s.rsi <= ?1")
    List<StockData> findOversoldStocks(BigDecimal rsiThreshold);

    @Query("SELECT s FROM StockData s WHERE s.rsi >= ?1")
    List<StockData> findOverboughtStocks(BigDecimal rsiThreshold);

    @Query("SELECT s FROM StockData s WHERE s.macd > s.signalLine")
    List<StockData> findStocksWithBullishMACD();

    @Query("SELECT s FROM StockData s WHERE s.macd < s.signalLine")
    List<StockData> findStocksWithBearishMACD();

    @Query("SELECT s FROM StockData s WHERE s.currentPrice <= s.bollingerLower")
    List<StockData> findStocksBelowBollingerLower();

    @Query("SELECT s FROM StockData s WHERE s.currentPrice >= s.bollingerUpper")
    List<StockData> findStocksAboveBollingerUpper();
}