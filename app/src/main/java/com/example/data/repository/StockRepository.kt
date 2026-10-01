package com.example.data.repository

import com.example.data.local.dao.WatchlistDao
import com.example.data.local.entity.WatchlistEntity
import com.example.data.model.Candle
import com.example.data.model.SmaSignal
import com.example.data.model.Stock
import com.example.data.model.Timeframe
import com.example.data.network.NseLivePriceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class StockRepository(
    private val watchlistDao: WatchlistDao,
    private val scope: CoroutineScope
) {
    private val nseService = NseLivePriceService()

    private val _stocks = MutableStateFlow<List<Stock>>(emptyList())
    val stocks: StateFlow<List<Stock>> = _stocks.asStateFlow()

    private val _nifty50Ltp = MutableStateFlow(22421.95)
    val nifty50Ltp: StateFlow<Double> = _nifty50Ltp.asStateFlow()

    private val _nifty50Change = MutableStateFlow(-294.25)
    val nifty50Change: StateFlow<Double> = _nifty50Change.asStateFlow()

    private val _bankNiftyLtp = MutableStateFlow(48150.80)
    val bankNiftyLtp: StateFlow<Double> = _bankNiftyLtp.asStateFlow()

    private val _bankNiftyChange = MutableStateFlow(-412.30)
    val bankNiftyChange: StateFlow<Double> = _bankNiftyChange.asStateFlow()

    private val _isLiveStreaming = MutableStateFlow(true)
    val isLiveStreaming: StateFlow<Boolean> = _isLiveStreaming.asStateFlow()

    private val _isNseApiLive = MutableStateFlow(true)
    val isNseApiLive: StateFlow<Boolean> = _isNseApiLive.asStateFlow()

    private val _isRefreshingNse = MutableStateFlow(false)
    val isRefreshingNse: StateFlow<Boolean> = _isRefreshingNse.asStateFlow()

    private val _lastNseSyncTime = MutableStateFlow("Synced")
    val lastNseSyncTime: StateFlow<String> = _lastNseSyncTime.asStateFlow()

    private val _streamSpeedMultiplier = MutableStateFlow(1) // 1x, 2x, 5x
    val streamSpeedMultiplier: StateFlow<Int> = _streamSpeedMultiplier.asStateFlow()

    private var streamingJob: Job? = null
    private var nsePollingJob: Job? = null
    private val stockCandleCache = ConcurrentHashMap<String, MutableMap<Timeframe, List<Candle>>>()

    init {
        initializeStocks()
        startLiveStream()
        startNseLivePolling()
        observeWatchlist()
        refreshRealNsePrices()
    }

    private fun initializeStocks() {
        val rawData = getInitialStockData()
        val stockList = rawData.map { raw ->
            val candles = generateHistoricalCandles(raw.basePrice, raw.volatility, raw.trendBias, Timeframe.DAILY)
            stockCandleCache.computeIfAbsent(raw.symbol) { ConcurrentHashMap() }[Timeframe.DAILY] = candles

            val ltp = candles.last().close
            val prevClose = candles[candles.size - 2].close
            val change = ltp - prevClose
            val changePercent = (change / prevClose) * 100.0
            val sma44 = candles.last().sma44
            val smaSlope = computeSmaSlope(candles)
            val distance = ((ltp - sma44) / sma44) * 100.0
            val signal = evaluateSignal(candles, smaSlope, distance)
            val sparkline = candles.takeLast(25).map { it.close }

            Stock(
                symbol = raw.symbol,
                name = raw.name,
                sector = raw.sector,
                exchange = "NSE",
                ltp = ltp,
                change = change,
                changePercent = changePercent,
                open = candles.last().open,
                high = candles.last().high,
                low = candles.last().low,
                prevClose = prevClose,
                volume = candles.last().volume,
                sma44 = sma44,
                smaSlope = smaSlope,
                distanceFromSmaPercent = distance,
                signal = signal,
                sparkline = sparkline,
                candles = candles,
                isWatchlist = false,
                peRatio = raw.pe,
                week52High = ltp * 1.25,
                week52Low = ltp * 0.72
            )
        }
        _stocks.value = stockList
    }

    private fun observeWatchlist() {
        scope.launch(Dispatchers.IO) {
            watchlistDao.getWatchlist().collect { watchlistEntities ->
                val watchlistSymbols = watchlistEntities.map { it.symbol }.toSet()
                _stocks.value = _stocks.value.map { stock ->
                    stock.copy(isWatchlist = watchlistSymbols.contains(stock.symbol))
                }
            }
        }
    }

    fun toggleWatchlist(symbol: String) {
        scope.launch(Dispatchers.IO) {
            val isCurrentWatchlist = _stocks.value.find { it.symbol == symbol }?.isWatchlist ?: false
            if (isCurrentWatchlist) {
                watchlistDao.removeFromWatchlist(symbol)
            } else {
                watchlistDao.addToWatchlist(WatchlistEntity(symbol))
            }
        }
    }

    fun setLiveStreaming(enabled: Boolean) {
        _isLiveStreaming.value = enabled
        if (enabled && streamingJob?.isActive != true) {
            startLiveStream()
        }
    }

    fun setStreamSpeed(speed: Int) {
        _streamSpeedMultiplier.value = speed
    }

    fun refreshRealNsePrices() {
        scope.launch(Dispatchers.IO) {
            _isRefreshingNse.value = true
            try {
                // Fetch Nifty 50 and Bank Nifty
                val niftyQuote = nseService.fetchQuote("^NSEI")
                if (niftyQuote != null) {
                    _nifty50Ltp.value = niftyQuote.regularMarketPrice
                    _nifty50Change.value = niftyQuote.regularMarketPrice - niftyQuote.previousClose
                    _isNseApiLive.value = true
                }

                val bankQuote = nseService.fetchQuote("^NSEBANK")
                if (bankQuote != null) {
                    _bankNiftyLtp.value = bankQuote.regularMarketPrice
                    _bankNiftyChange.value = bankQuote.regularMarketPrice - bankQuote.previousClose
                }

                // Batch fetch active stocks from NSE
                val currentList = _stocks.value.toMutableList()
                val targetSymbols = currentList.take(15).map { it.symbol }
                for (sym in targetSymbols) {
                    val quote = nseService.fetchQuote(sym)
                    if (quote != null) {
                        val idx = currentList.indexOfFirst { it.symbol == sym }
                        if (idx != -1) {
                            val st = currentList[idx]
                            val newLtp = quote.regularMarketPrice
                            val newChange = newLtp - quote.previousClose
                            val newPct = (newChange / quote.previousClose) * 100.0
                            val newHigh = max(st.high, quote.regularMarketDayHigh.coerceAtLeast(newLtp))
                            val newLow = min(st.low, quote.regularMarketDayLow.coerceAtMost(newLtp))
                            val newVol = max(st.volume, quote.regularMarketVolume)

                            // Recalculate 44 SMA with real quote
                            val candles = st.candles.toMutableList()
                            if (candles.isNotEmpty()) {
                                val last = candles.last().copy(
                                    close = newLtp,
                                    high = newHigh,
                                    low = newLow,
                                    volume = newVol
                                )
                                candles[candles.lastIndex] = last
                            }
                            val updatedSma44 = if (candles.size >= 44) {
                                candles.takeLast(44).map { it.close }.average()
                            } else st.sma44

                            val newDistance = ((newLtp - updatedSma44) / updatedSma44) * 100.0
                            val newSignal = evaluateSignal(candles, st.smaSlope, newDistance)

                            currentList[idx] = st.copy(
                                ltp = newLtp,
                                prevClose = quote.previousClose,
                                change = newChange,
                                changePercent = newPct,
                                high = newHigh,
                                low = newLow,
                                volume = newVol,
                                sma44 = updatedSma44,
                                distanceFromSmaPercent = newDistance,
                                signal = newSignal,
                                bidPrice = newLtp - 0.20,
                                askPrice = newLtp + 0.20
                            )
                        }
                    }
                }
                _stocks.value = currentList
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
                _lastNseSyncTime.value = sdf.format(Date())
            } catch (_: Exception) {
                // Keep streaming smoothly
            } finally {
                _isRefreshingNse.value = false
            }
        }
    }

    private fun startNseLivePolling() {
        nsePollingJob?.cancel()
        nsePollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(8000) // Poll real NSE quotes every 8 seconds
                if (_isLiveStreaming.value) {
                    refreshRealNsePrices()
                }
            }
        }
    }

    private fun startLiveStream() {
        streamingJob?.cancel()
        streamingJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (_isLiveStreaming.value) {
                    val delayMs = (1200L / _streamSpeedMultiplier.value.coerceAtLeast(1))
                    delay(delayMs)
                    updateMarketTicks()
                } else {
                    delay(500)
                }
            }
        }
    }

    private fun updateMarketTicks() {
        // Nifty & BankNifty index ticks
        val niftyDelta = (Random.nextDouble(-0.12, 0.15) / 100.0) * _nifty50Ltp.value
        _nifty50Ltp.value = (_nifty50Ltp.value + niftyDelta)
        _nifty50Change.value = _nifty50Change.value + niftyDelta

        val bankDelta = (Random.nextDouble(-0.15, 0.18) / 100.0) * _bankNiftyLtp.value
        _bankNiftyLtp.value = (_bankNiftyLtp.value + bankDelta)
        _bankNiftyChange.value = _bankNiftyChange.value + bankDelta

        // Pick 4-7 stocks to tick on each pulse for realistic streaming
        val currentList = _stocks.value.toMutableList()
        val numToUpdate = Random.nextInt(4, 9)
        val indicesToUpdate = (currentList.indices).shuffled().take(numToUpdate)

        for (idx in indicesToUpdate) {
            val stock = currentList[idx]
            val pctChange = Random.nextDouble(-0.35, 0.38) / 100.0
            val tickDelta = stock.ltp * pctChange
            val newLtp = max(1.0, stock.ltp + tickDelta)
            val newChange = newLtp - stock.prevClose
            val newChangePct = (newChange / stock.prevClose) * 100.0
            val newHigh = max(stock.high, newLtp)
            val newLow = min(stock.low, newLtp)
            val newVol = stock.volume + Random.nextLong(200, 3500)

            // Update latest candle close & high/low
            val candles = stock.candles.toMutableList()
            if (candles.isNotEmpty()) {
                val lastCandle = candles.last()
                val updatedCandle = lastCandle.copy(
                    close = newLtp,
                    high = max(lastCandle.high, newLtp),
                    low = min(lastCandle.low, newLtp),
                    volume = lastCandle.volume + (newVol - stock.volume)
                )
                candles[candles.lastIndex] = updatedCandle
            }

            // Recalculate 44 SMA on updated candles
            val updatedSma44 = if (candles.size >= 44) {
                candles.takeLast(44).map { it.close }.average()
            } else {
                stock.sma44
            }
            val newDistance = ((newLtp - updatedSma44) / updatedSma44) * 100.0
            val updatedSparkline = stock.sparkline.drop(1) + newLtp
            val newSignal = evaluateSignal(candles, stock.smaSlope, newDistance)

            currentList[idx] = stock.copy(
                ltp = newLtp,
                change = newChange,
                changePercent = newChangePct,
                high = newHigh,
                low = newLow,
                volume = newVol,
                sma44 = updatedSma44,
                distanceFromSmaPercent = newDistance,
                signal = newSignal,
                sparkline = updatedSparkline,
                candles = candles,
                bidPrice = newLtp - 0.15,
                askPrice = newLtp + 0.15
            )
        }
        _stocks.value = currentList
    }

    fun getCandlesForTimeframe(symbol: String, timeframe: Timeframe): List<Candle> {
        val cached = stockCandleCache[symbol]?.get(timeframe)
        if (cached != null) return cached

        val stock = _stocks.value.find { it.symbol == symbol } ?: return emptyList()
        val raw = getInitialStockData().find { it.symbol == symbol }
        val vol = raw?.volatility ?: 0.015
        val bias = raw?.trendBias ?: 0.001

        val generated = generateHistoricalCandles(stock.ltp, vol, bias, timeframe)
        stockCandleCache.computeIfAbsent(symbol) { ConcurrentHashMap() }[timeframe] = generated
        return generated
    }

    fun getStockBySymbol(symbol: String): Stock? {
        return _stocks.value.find { it.symbol == symbol }
    }

    private fun computeSmaSlope(candles: List<Candle>): Double {
        if (candles.size < 50) return 0.0
        val lastIdx = candles.lastIndex
        val smaNow = candles[lastIdx].sma44
        val sma5Ago = candles[lastIdx - 5].sma44
        return (smaNow - sma5Ago) / 5.0
    }

    private fun evaluateSignal(
        candles: List<Candle>,
        smaSlope: Double,
        distancePercent: Double
    ): SmaSignal {
        if (candles.isEmpty()) return SmaSignal.NEUTRAL
        val lastCandle = candles.last()
        val prevCandle = if (candles.size > 1) candles[candles.size - 2] else lastCandle

        val isRisingSma = smaSlope > 0.05
        val isFallingSma = smaSlope < -0.05

        val touchesSma = abs(distancePercent) < 0.6
        val crossedAbove = prevCandle.close <= prevCandle.sma44 && lastCandle.close > lastCandle.sma44
        val crossedBelow = prevCandle.close >= prevCandle.sma44 && lastCandle.close < lastCandle.sma44

        return when {
            isRisingSma && touchesSma && lastCandle.isBullish -> SmaSignal.RISING_BOUNCE
            isRisingSma && crossedAbove -> SmaSignal.RISING_BREAKOUT
            isRisingSma && distancePercent > 0 -> SmaSignal.RISING_TREND
            isFallingSma && touchesSma && !lastCandle.isBullish -> SmaSignal.FALLING_REJECT
            isFallingSma && crossedBelow -> SmaSignal.FALLING_BREAKDOWN
            isFallingSma && distancePercent < 0 -> SmaSignal.FALLING_TREND
            abs(distancePercent) < 0.8 -> SmaSignal.NEAR_SMA
            else -> SmaSignal.NEUTRAL
        }
    }

    private fun generateHistoricalCandles(
        basePrice: Double,
        volatility: Double,
        trendBias: Double,
        timeframe: Timeframe
    ): List<Candle> {
        val count = when (timeframe) {
            Timeframe.FIVE_MIN -> 120
            Timeframe.FIFTEEN_MIN -> 100
            Timeframe.ONE_HOUR -> 90
            Timeframe.DAILY -> 80
        }
        val intervalMs = when (timeframe) {
            Timeframe.FIVE_MIN -> 5 * 60 * 1000L
            Timeframe.FIFTEEN_MIN -> 15 * 60 * 1000L
            Timeframe.ONE_HOUR -> 60 * 60 * 1000L
            Timeframe.DAILY -> 24 * 60 * 60 * 1000L
        }

        val candles = ArrayList<Candle>(count)
        val now = System.currentTimeMillis()
        var currentClose = basePrice * (1.0 - (count * trendBias * 0.7))

        for (i in 0 until count) {
            val stepTrend = trendBias + Random.nextDouble(-volatility, volatility)
            val open = currentClose
            val close = max(1.0, open * (1.0 + stepTrend))
            val high = max(open, close) * (1.0 + Random.nextDouble(0.001, volatility * 0.8))
            val low = min(open, close) * (1.0 - Random.nextDouble(0.001, volatility * 0.8))
            val volume = (Random.nextLong(50_000, 1_500_000) * (if (abs(close - open) / open > 0.015) 1.8 else 1.0)).toLong()
            val timestamp = now - ((count - i) * intervalMs)

            currentClose = close
            candles.add(Candle(timestamp, open, high, low, close, volume))
        }

        // Compute 44 SMA for each candle
        val result = ArrayList<Candle>(count)
        for (i in candles.indices) {
            val sma = if (i >= 43) {
                candles.subList(i - 43, i + 1).map { it.close }.average()
            } else {
                // Approximate early SMA for visual continuity
                candles.subList(0, i + 1).map { it.close }.average()
            }
            result.add(candles[i].copy(sma44 = sma))
        }
        return result
    }

    private data class StockInitial(
        val symbol: String,
        val name: String,
        val sector: String,
        val basePrice: Double,
        val volatility: Double,
        val trendBias: Double, // positive bias = rising 44 SMA, negative = falling 44 SMA
        val pe: Double
    )

    private fun getInitialStockData(): List<StockInitial> {
        return listOf(
            // Rising 44 SMA Heavyweights (Bullish support/bounce candidates)
            StockInitial("RELIANCE", "Reliance Industries Ltd", "Energy", 2985.40, 0.012, 0.0012, 28.4),
            StockInitial("TCS", "Tata Consultancy Services", "IT", 4210.80, 0.011, 0.0010, 31.2),
            StockInitial("HDFCBANK", "HDFC Bank Ltd", "Banking", 1682.30, 0.014, 0.0009, 19.5),
            StockInitial("BHARTIARTL", "Bharti Airtel Ltd", "Telecom", 1640.50, 0.013, 0.0015, 42.1),
            StockInitial("TATAMOTORS", "Tata Motors Ltd", "Auto", 975.20, 0.018, 0.0018, 16.3),
            StockInitial("SBIN", "State Bank of India", "Banking", 824.60, 0.015, 0.0011, 10.8),
            StockInitial("SUNPHARMA", "Sun Pharmaceutical Ind", "Pharma", 1910.40, 0.012, 0.0014, 37.6),
            StockInitial("BAJFINANCE", "Bajaj Finance Ltd", "Financial Services", 7420.00, 0.017, 0.0008, 29.8),
            StockInitial("LT", "Larsen & Toubro Ltd", "Infrastructure", 3640.20, 0.013, 0.0013, 34.0),
            StockInitial("TITAN", "Titan Company Ltd", "Consumer", 3450.80, 0.014, 0.0011, 84.5),
            StockInitial("NTPC", "NTPC Limited", "Energy", 412.30, 0.014, 0.0016, 17.2),
            StockInitial("M&M", "Mahindra & Mahindra Ltd", "Auto", 3120.50, 0.016, 0.0017, 32.4),
            StockInitial("BEL", "Bharat Electronics Ltd", "Defence", 298.40, 0.021, 0.0020, 44.8),
            StockInitial("TRENT", "Trent Limited", "Retail", 7640.00, 0.022, 0.0025, 120.4),
            StockInitial("POWERGRID", "Power Grid Corp of India", "Energy", 338.20, 0.011, 0.0012, 18.5),
            StockInitial("COALINDIA", "Coal India Ltd", "Metals & Mining", 504.60, 0.016, 0.0014, 8.9),
            StockInitial("DIVISLAB", "Divi's Laboratories Ltd", "Pharma", 5380.00, 0.015, 0.0013, 68.2),
            StockInitial("CIPLA", "Cipla Limited", "Pharma", 1620.40, 0.012, 0.0012, 27.5),
            StockInitial("EICHERMOT", "Eicher Motors Ltd", "Auto", 4890.00, 0.014, 0.0013, 33.1),
            StockInitial("PERSISTENT", "Persistent Systems Ltd", "IT", 5210.00, 0.019, 0.0016, 52.3),

            // Falling 44 SMA Candidates (Bearish resistance/breakdown candidates)
            StockInitial("INFY", "Infosys Limited", "IT", 1885.60, 0.014, -0.0011, 26.8),
            StockInitial("WIPRO", "Wipro Limited", "IT", 532.40, 0.015, -0.0013, 21.4),
            StockInitial("TECHM", "Tech Mahindra Ltd", "IT", 1580.20, 0.016, -0.0010, 48.2),
            StockInitial("ASIANPAINT", "Asian Paints Ltd", "Consumer", 2840.50, 0.013, -0.0014, 51.2),
            StockInitial("HINDUNILVR", "Hindustan Unilever Ltd", "FMCG", 2680.00, 0.011, -0.0008, 56.4),
            StockInitial("NESTLEIND", "Nestle India Ltd", "FMCG", 2510.30, 0.010, -0.0009, 72.1),
            StockInitial("INDUSINDBK", "IndusInd Bank Ltd", "Banking", 1420.80, 0.018, -0.0015, 12.4),
            StockInitial("BPCL", "Bharat Petroleum Corp", "Energy", 348.60, 0.017, -0.0012, 9.4),
            StockInitial("TATASTEEL", "Tata Steel Ltd", "Metals", 158.40, 0.019, -0.0009, 38.6),
            StockInitial("JSWSTEEL", "JSW Steel Ltd", "Metals", 982.50, 0.018, -0.0010, 24.3),
            StockInitial("DRREDDY", "Dr. Reddy's Laboratories", "Pharma", 6540.00, 0.013, -0.0008, 19.8),
            StockInitial("TATACONSUM", "Tata Consumer Products", "FMCG", 1140.20, 0.014, -0.0007, 78.4),

            // Near 44 SMA & Active Swings
            StockInitial("ICICIBANK", "ICICI Bank Ltd", "Banking", 1245.50, 0.013, 0.0002, 17.8),
            StockInitial("ITC", "ITC Limited", "FMCG", 498.30, 0.010, 0.0003, 27.2),
            StockInitial("KOTAKBANK", "Kotak Mahindra Bank", "Banking", 1790.60, 0.012, 0.0001, 18.9),
            StockInitial("AXISBANK", "Axis Bank Ltd", "Banking", 1210.40, 0.015, 0.0004, 14.5),
            StockInitial("MARUTI", "Maruti Suzuki India", "Auto", 12480.00, 0.014, 0.0002, 27.8),
            StockInitial("ADANIENT", "Adani Enterprises Ltd", "Metals & Mining", 3120.00, 0.024, 0.0003, 88.0),
            StockInitial("ULTRACEMCO", "UltraTech Cement Ltd", "Materials", 11240.00, 0.013, 0.0004, 42.1),
            StockInitial("BAJAJFINSV", "Bajaj Finserv Ltd", "Financial Services", 1870.20, 0.015, 0.0002, 35.1),
            StockInitial("HCLTECH", "HCL Technologies Ltd", "IT", 1760.40, 0.013, 0.0003, 28.5),
            StockInitial("GRASIM", "Grasim Industries Ltd", "Materials", 2640.00, 0.014, 0.0002, 31.0),
            StockInitial("HEROMOTOCO", "Hero MotoCorp Ltd", "Auto", 5480.00, 0.016, 0.0004, 25.4),
            StockInitial("APOLLOHOSP", "Apollo Hospitals Enterprise", "Healthcare", 6920.00, 0.015, 0.0005, 86.2),
            StockInitial("BRITANNIA", "Britannia Industries Ltd", "FMCG", 6120.00, 0.011, 0.0001, 58.9),
            StockInitial("SBILIFE", "SBI Life Insurance Co", "Insurance", 1780.40, 0.012, 0.0003, 76.5),
            StockInitial("HDFCLIFE", "HDFC Life Insurance Co", "Insurance", 710.20, 0.013, -0.0002, 79.2),
            StockInitial("ADANIPORTS", "Adani Ports and SEZ", "Infrastructure", 1430.50, 0.018, 0.0005, 34.2),
            StockInitial("DIXON", "Dixon Technologies Ltd", "Electronics", 12890.00, 0.023, 0.0006, 95.0),
            StockInitial("POLYCAB", "Polycab India Ltd", "Industrial", 6780.00, 0.018, 0.0004, 52.0)
        )
    }
}
