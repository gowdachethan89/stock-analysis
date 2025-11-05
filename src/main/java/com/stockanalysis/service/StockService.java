package com.stockanalysis.service;

import com.stockanalysis.model.StockData;
import com.stockanalysis.repository.StockDataRepository;
import yahoofinance.YahooFinance;
import yahoofinance.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Objects;

@Service
public class StockService {

    @Autowired
    private StockDataRepository stockDataRepository;

    public StockData getStockData(String symbol) {
        try {
            Stock stock = YahooFinance.get(symbol);
            StockData stockData = new StockData();

            stockData.setSymbol(symbol);
            stockData.setCurrentPrice(stock.getQuote().getPrice());
            stockData.setChange(stock.getQuote().getChange());
            stockData.setChangePercent(stock.getQuote().getChangeInPercent());
            stockData.setDayHigh(stock.getQuote().getDayHigh());
            stockData.setDayLow(stock.getQuote().getDayLow());
            stockData.setVolume(stock.getQuote().getVolume());
            stockData.setOpen(stock.getQuote().getOpen());
            stockData.setPreviousClose(stock.getQuote().getPreviousClose());
            stockData.setMarketCap(stock.getStats().getMarketCap());
            stockData.setLastUpdateTime(LocalDateTime.now());

            // Also set the high and low values
            stockData.setHigh(stock.getQuote().getDayHigh());
            stockData.setLow(stock.getQuote().getDayLow());

            // Calculate technical indicators
            List<StockData> historicalData = getStockHistory(symbol);
            calculateTechnicalIndicators(stockData, historicalData);

            // Save to database
            stockDataRepository.save(stockData);

            return stockData;
        } catch (IOException e) {
            throw new RuntimeException("Error fetching stock data for " + symbol, e);
        }
    }

    private void calculateTechnicalIndicators(StockData currentData, List<StockData> historicalData) {
        if (historicalData != null && !historicalData.isEmpty()) {
            // Calculate existing indicators
            calculateMovingAverages(currentData, historicalData);
            calculateRSI(currentData, historicalData);
            calculateMACD(currentData, historicalData);

            // Calculate new indicators
            calculateBollingerBands(currentData, historicalData);
            calculateStochasticOscillator(currentData, historicalData);
            calculateATR(currentData, historicalData);
            calculateMFI(currentData, historicalData);
        }
    }

    private void calculateMovingAverages(StockData currentData, List<StockData> historicalData) {
        // Existing MA calculations remain the same
        if (historicalData.size() >= 20) {
            BigDecimal ma20 = historicalData.stream()
                    .limit(20)
                    .map(StockData::getCurrentPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP);
            currentData.setMa20(ma20);
        }

        if (historicalData.size() >= 50) {
            BigDecimal ma50 = historicalData.stream()
                    .limit(50)
                    .map(StockData::getCurrentPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(50), 2, RoundingMode.HALF_UP);
            currentData.setMa50(ma50);
        }
    }

    private void calculateBollingerBands(StockData currentData, List<StockData> historicalData) {
        if (historicalData.size() >= 20) {
            // Calculate 20-day SMA
            BigDecimal sma = historicalData.stream()
                    .limit(20)
                    .map(StockData::getCurrentPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP);

            // Calculate Standard Deviation
            double sumSquaredDiff = historicalData.stream()
                    .limit(20)
                    .mapToDouble(data ->
                            Math.pow(data.getCurrentPrice().subtract(sma).doubleValue(), 2))
                    .sum();
            double standardDeviation = Math.sqrt(sumSquaredDiff / 20);

            // Calculate Bollinger Bands
            currentData.setBollingerMiddle(sma);
            currentData.setBollingerUpper(sma.add(BigDecimal.valueOf(standardDeviation * 2)));
            currentData.setBollingerLower(sma.subtract(BigDecimal.valueOf(standardDeviation * 2)));
        }
    }

    private void calculateStochasticOscillator(StockData currentData, List<StockData> historicalData) {
        if (historicalData.size() >= 14) {
            List<StockData> period = historicalData.subList(0, 14);

            BigDecimal lowestLow = period.stream()
                    .map(StockData::getLow)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            BigDecimal highestHigh = period.stream()
                    .map(StockData::getHigh)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            // Calculate %K
            BigDecimal currentPrice = currentData.getCurrentPrice();
            BigDecimal range = highestHigh.subtract(lowestLow);
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal k = currentPrice.subtract(lowestLow)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(range, 2, RoundingMode.HALF_UP);
                currentData.setStochasticK(k);

                // Calculate %D (3-day SMA of %K)
                if (historicalData.size() >= 3) {
                    BigDecimal d = historicalData.stream()
                            .limit(3)
                            .map(StockData::getStochasticK)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
                    currentData.setStochasticD(d);
                }
            }
        }
    }

    private void calculateATR(StockData currentData, List<StockData> historicalData) {
        if (historicalData.size() >= 14) {
            List<BigDecimal> trueRanges = new ArrayList<>();

            for (int i = 0; i < 14; i++) {
                StockData current = historicalData.get(i);
                StockData previous = (i < historicalData.size() - 1) ? historicalData.get(i + 1) : null;

                if (previous != null) {
                    BigDecimal tr1 = current.getHigh().subtract(current.getLow());
                    BigDecimal tr2 = current.getHigh().subtract(previous.getCurrentPrice()).abs();
                    BigDecimal tr3 = current.getLow().subtract(previous.getCurrentPrice()).abs();

                    BigDecimal trueRange = tr1.max(tr2).max(tr3);
                    trueRanges.add(trueRange);
                }
            }

            // Calculate ATR as average of true ranges
            if (!trueRanges.isEmpty()) {
                BigDecimal atr = trueRanges.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(trueRanges.size()), 2, RoundingMode.HALF_UP);
                currentData.setAtr(atr);
            }
        }
    }

    private void calculateMFI(StockData currentData, List<StockData> historicalData) {
        if (historicalData.size() >= 14) {
            List<BigDecimal> positiveFlow = new ArrayList<>();
            List<BigDecimal> negativeFlow = new ArrayList<>();

            for (int i = 0; i < 13; i++) {
                StockData current = historicalData.get(i);
                StockData previous = historicalData.get(i + 1);

                // Calculate typical price
                BigDecimal currentTP = current.getHigh()
                        .add(current.getLow())
                        .add(current.getCurrentPrice())
                        .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

                BigDecimal previousTP = previous.getHigh()
                        .add(previous.getLow())
                        .add(previous.getCurrentPrice())
                        .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

                // Calculate money flow
                BigDecimal rawMoneyFlow = currentTP.multiply(BigDecimal.valueOf(current.getVolume()));

                if (currentTP.compareTo(previousTP) > 0) {
                    positiveFlow.add(rawMoneyFlow);
                } else {
                    negativeFlow.add(rawMoneyFlow);
                }
            }

            // Calculate MFI
            BigDecimal positiveFlowSum = positiveFlow.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal negativeFlowSum = negativeFlow.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (negativeFlowSum.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal moneyRatio = positiveFlowSum.divide(negativeFlowSum, 2, RoundingMode.HALF_UP);
                BigDecimal mfi = BigDecimal.valueOf(100)
                        .subtract(BigDecimal.valueOf(100)
                                .divide(BigDecimal.ONE.add(moneyRatio), 2, RoundingMode.HALF_UP));
                currentData.setMfi(mfi);
            }
        }
    }

    // Existing methods remain the same
    private void calculateRSI(StockData currentData, List<StockData> historicalData) {
        if (historicalData.size() >= 14) {
            List<StockData> rsiData = historicalData.subList(0, 14);
            double gains = 0;
            double losses = 0;

            for (int i = 0; i < rsiData.size() - 1; i++) {
                BigDecimal change = rsiData.get(i).getCurrentPrice().subtract(rsiData.get(i + 1).getCurrentPrice());
                if (change.compareTo(BigDecimal.ZERO) > 0) {
                    gains += change.doubleValue();
                } else {
                    losses += change.abs().doubleValue();
                }
            }

            double avgGain = gains / 14;
            double avgLoss = losses / 14;
            double rs = avgGain / avgLoss;
            double rsi = 100 - (100 / (1 + rs));

            currentData.setRsi(BigDecimal.valueOf(rsi));
        }
    }

    private BigDecimal calculateEMA(List<StockData> data, int period) {
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal initialSMA = data.stream()
                .limit(period)
                .map(StockData::getCurrentPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), 2, RoundingMode.HALF_UP);

        return data.stream()
                .skip(period -1)
                .limit(1)
                .map(StockData::getCurrentPrice)
                .map(price -> price.multiply(multiplier)
                        .add(initialSMA.multiply(BigDecimal.ONE.subtract(multiplier))))
                .findFirst()
                .orElse(initialSMA);
    }

    private BigDecimal calculateSignalLine(List<StockData> data) {
        if (data.size() >= 9) {
            return data.stream()
                    .limit(9)
                    .map(StockData::getMacd)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(9), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private void calculateMACD(StockData currentData, List<StockData> historicalData) {
        if (historicalData.size() >= 26) {
            // Calculate 12-day EMA
            BigDecimal ema12 = calculateEMA(historicalData, 12);
            // Calculate 26-day EMA
            BigDecimal ema26 = calculateEMA(historicalData, 26);

            // MACD Line = 12-day EMA - 26-day EMA
            BigDecimal macd = ema12.subtract(ema26);
            currentData.setMacd(macd);

            // Calculate Signal Line (9-day EMA of MACD)
            BigDecimal signalLine = calculateSignalLine(historicalData);
            if (signalLine != null) {
                currentData.setSignalLine(signalLine);
                // MACD Histogram = MACD Line - Signal Line
                currentData.setMacdHistogram(macd.subtract(signalLine));
            }
        }
    }

    public Map<String, Object> getTechnicalIndicators(StockData stockData) {
        Map<String, Object> technicalData = new HashMap<>();
        technicalData.put("symbol", stockData.getSymbol());
        technicalData.put("price", stockData.getCurrentPrice());

        // Moving Averages
        technicalData.put("ma20", stockData.getMa20());
        technicalData.put("ma50", stockData.getMa50());

        // RSI
        technicalData.put("rsi", stockData.getRsi());

        // MACD
        technicalData.put("macd", stockData.getMacd());
        technicalData.put("signalLine", stockData.getSignalLine());
        technicalData.put("macdHistogram", stockData.getMacdHistogram());

        // Bollinger Bands
        technicalData.put("bollingerUpper", stockData.getBollingerUpper());
        technicalData.put("bollingerMiddle", stockData.getBollingerMiddle());
        technicalData.put("bollingerLower", stockData.getBollingerLower());

        // Stochastic Oscillator
        technicalData.put("stochasticK", stockData.getStochasticK());
        technicalData.put("stochasticD", stockData.getStochasticD());

        // ATR
        technicalData.put("atr", stockData.getAtr());

        // MFI
        technicalData.put("mfi", stockData.getMfi());

        return technicalData;
    }

    public StockData getLatestStockData(String symbol) {
        return stockDataRepository.findFirstBySymbolOrderByLastUpdateTimeDesc(symbol);
    }

    public List<StockData> getStockHistory(String symbol) {
        return stockDataRepository.findBySymbolOrderByLastUpdateTimeDesc(symbol);
    }

    public List<StockData> getHistoricalData(String symbol, LocalDateTime startDate) {
        return stockDataRepository.findHistoricalData(symbol, startDate);
    }

    public List<StockData> getRecentStockData(LocalDateTime since) {
        return stockDataRepository.findAllRecentData(since);
    }

    public List<StockData> getLastNRecords(String symbol, int n) {
        return stockDataRepository.findLastNRecords(symbol, n);
    }

    public List<StockData> getStocksAbovePrice(BigDecimal price) {
        return stockDataRepository.findStocksAbovePrice(price);
    }

    public List<StockData> getTopGainers(BigDecimal minChangePercent) {
        return stockDataRepository.findTopGainers(minChangePercent);
    }

    public List<StockData> getTopLosers(BigDecimal maxChangePercent) {
        return stockDataRepository.findTopLosers(maxChangePercent);
    }

    public List<StockData> getHighVolumeStocks(Long minVolume) {
        return stockDataRepository.findHighVolumeStocks(minVolume);
    }

    public List<StockData> getOversoldStocks(BigDecimal rsiThreshold) {
        return stockDataRepository.findOversoldStocks(rsiThreshold);
    }

    public List<StockData> getOverboughtStocks(BigDecimal rsiThreshold) {
        return stockDataRepository.findOverboughtStocks(rsiThreshold);
    }

    public List<StockData> getStocksWithBullishMACD() {
        return stockDataRepository.findStocksWithBullishMACD();
    }

    public List<StockData> getStocksWithBearishMACD() {
        return stockDataRepository.findStocksWithBearishMACD();
    }

    public List<StockData> getStocksBelowBollingerLower() {
        return stockDataRepository.findStocksBelowBollingerLower();
    }

    public List<StockData> getStocksAboveBollingerUpper() {
        return stockDataRepository.findStocksAboveBollingerUpper();
    }

    public List<StockData> getHistoricalDataBetweenDates(String symbol, LocalDateTime startDate, LocalDateTime endDate) {
        return stockDataRepository.findHistoricalDataBetweenDates(symbol, startDate, endDate);
    }

    public List<String> getAllTrackedSymbols() {
        return stockDataRepository.findAllTrackedSymbols();
    }
}
