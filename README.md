# Stock Analysis Application

## Symbol Usage Guide

### Regular Stock Symbols
- Use standard stock symbols like:
    - AAPL (Apple Inc.)
    - MSFT (Microsoft)
    - GOOGL (Alphabet/Google)
    - AMZN (Amazon)

Example usage in the UI:
```
Enter "AAPL" in the symbol input box to get Apple Inc. stock data
```

### BSE (Bombay Stock Exchange) Symbols
- Use BSE scrip codes for Indian stocks
- Example scrip codes:
    - 500325 (Reliance Industries)
    - 500112 (State Bank of India)
    - 500209 (Infosys)

Example usage for BSE stocks:
```
Use the BSE scrip code "500325" to get Reliance Industries data
```

## API Endpoints

### Regular Stocks
```
GET /api/stocks/{symbol}
Example: /api/stocks/AAPL
```

### BSE Stocks
```
GET /api/stocks/bse/{scripCode}
Example: /api/stocks/bse/500325
```

## API Testing Guide

### Regular Stock Endpoints

1. Get Stock Data
```bash
curl http://localhost:5000/api/stocks/AAPL
```

2. Get Technical Indicators
```bash
curl http://localhost:5000/api/stocks/AAPL/technical
```

3. Get Historical Data
```bash
# Get all historical data
curl http://localhost:5000/api/stocks/AAPL/history

# Get data from a specific date
curl "http://localhost:5000/api/stocks/AAPL/history?startDate=2024-01-01T00:00:00"
```

4. Get List of Tracked Symbols
```bash
curl http://localhost:5000/api/stocks/tracked
```

### BSE Stock Endpoints

1. Get BSE Stock Quote
```bash
curl http://localhost:5000/api/stocks/bse/500325
```

2. Get All BSE Stocks
```bash
curl http://localhost:5000/api/stocks/bse/all
```

## Response Examples

### Regular Stock Data Response
```json
{
  "symbol": "AAPL",
  "price": 173.25,
  "change": 2.35,
  "changePercent": 1.37,
  "high": 174.12,
  "low": 171.45,
  "volume": 52436789,
  "timestamp": "2024-03-14T16:00:00"
}
```

### Technical Indicators Response
```json
{
  "symbol": "AAPL",
  "price": 173.25,
  "ma20": 170.45,
  "ma50": 168.32,
  "rsi": 58.67,
  "macd": 1.23,
  "signalLine": 0.95,
  "macdHistogram": 0.28,
  "bollingerUpper": 175.45,
  "bollingerMiddle": 170.45,
  "bollingerLower": 165.45,
  "stochasticK": 75.34,
  "stochasticD": 68.45,
  "atr": 2.85,
  "mfi": 62.43
}
```

### BSE Stock Data Response
```json
{
  "scripCode": "500325",
  "currentPrice": 2456.75,
  "dayHigh": 2468.90,
  "dayLow": 2445.30,
  "previousClose": 2450.60,
  "change": 6.15,
  "changePercent": 0.25,
  "volume": 1234567,
  "tradedValue": 3012456789.45
}
```

## Technical Indicators
All technical indicators (RSI, MACD, Bollinger Bands, etc.) are calculated for both regular stocks and BSE stocks automatically when you fetch the stock data.