package com.stockanalysis.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_data")
@Getter
@Setter
public class StockData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String companyName;
    private String industry;

    @Column(name = "current_price")
    private BigDecimal currentPrice;
    private BigDecimal open;

    @Column(name = "day_high")
    private BigDecimal dayHigh;

    @Column(name = "day_low")
    private BigDecimal dayLow;

    @Column(name = "previous_close")
    private BigDecimal previousClose;

    @Column(name = "chg") // 'change' is a reserved keyword in MySQL
    private BigDecimal change;

    @Column(name = "change_percent")
    private BigDecimal changePercent;

    private Long volume;
    private BigDecimal vwap;

    @Column(name = "week_high")
    private BigDecimal weekHigh;

    @Column(name = "week_low")
    private BigDecimal weekLow;

    @Column(name = "upper_circuit")
    private BigDecimal upperCircuit;

    @Column(name = "lower_circuit")
    private BigDecimal lowerCircuit;

    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;

    // Technical indicators
    private BigDecimal ma20;
    private BigDecimal ma50;
    private BigDecimal rsi;
    private BigDecimal marketCap;

    @Column(name = "macd")
    private BigDecimal macd;

    @Column(name = "signal_line")
    private BigDecimal signalLine;

    @Column(name = "macd_histogram")
    private BigDecimal macdHistogram;

    // Bollinger Bands
    private BigDecimal bollingerUpper;
    private BigDecimal bollingerMiddle;
    private BigDecimal bollingerLower;

    // Stochastic Oscillator
    private BigDecimal stochasticK;
    private BigDecimal stochasticD;

    // Average True Range
    private BigDecimal atr;

    // Money Flow Index
    private BigDecimal mfi;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}