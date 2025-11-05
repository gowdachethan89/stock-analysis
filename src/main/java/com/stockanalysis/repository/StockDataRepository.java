package com.stockanalysis.repository;

import com.stockanalysis.model.StockData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, Long> {
    List<StockData> findBySymbolOrderByTimestampDesc(String symbol);
    StockData findFirstBySymbolOrderByTimestampDesc(String symbol);

    @Query("SELECT s FROM StockData s WHERE s.symbol = ?1 AND s.timestamp >= ?2")
    List<StockData> findHistoricalData(String symbol, LocalDateTime startDate);

    @Query("SELECT s FROM StockData s WHERE s.symbol = ?1 AND s.timestamp >= ?2 AND s.timestamp <= ?3")
    List<StockData> findHistoricalDataBetweenDates(String symbol, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT DISTINCT s.symbol FROM StockData s")
    List<String> findAllTrackedSymbols();
}