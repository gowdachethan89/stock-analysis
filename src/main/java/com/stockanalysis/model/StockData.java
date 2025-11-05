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
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal high;
    private BigDecimal low;
    private Long volume;
    private LocalDateTime timestamp;
    private BigDecimal ma20;
    private BigDecimal ma50;
    private BigDecimal rsi;
    private String companyName;
    private String sector;
    private String industry;
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