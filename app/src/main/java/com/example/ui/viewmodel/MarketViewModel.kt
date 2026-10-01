package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Candle
import com.example.data.model.ScanType
import com.example.data.model.SmaSignal
import com.example.data.model.Stock
import com.example.data.model.Timeframe
import com.example.data.repository.AlertRepository
import com.example.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class MarketViewModel(
    private val stockRepository: StockRepository,
    private val alertRepository: AlertRepository
) : ViewModel() {

    val nifty50Ltp: StateFlow<Double> = stockRepository.nifty50Ltp
    val nifty50Change: StateFlow<Double> = stockRepository.nifty50Change
    val bankNiftyLtp: StateFlow<Double> = stockRepository.bankNiftyLtp
    val bankNiftyChange: StateFlow<Double> = stockRepository.bankNiftyChange
    val isLiveStreaming: StateFlow<Boolean> = stockRepository.isLiveStreaming
    val isNseApiLive: StateFlow<Boolean> = stockRepository.isNseApiLive
    val isRefreshingNse: StateFlow<Boolean> = stockRepository.isRefreshingNse
    val lastNseSyncTime: StateFlow<String> = stockRepository.lastNseSyncTime
    val streamSpeedMultiplier: StateFlow<Int> = stockRepository.streamSpeedMultiplier

    private val _scanType = MutableStateFlow(ScanType.ALL)
    val scanType: StateFlow<ScanType> = _scanType.asStateFlow()

    private val _timeframe = MutableStateFlow(Timeframe.DAILY)
    val timeframe: StateFlow<Timeframe> = _timeframe.asStateFlow()

    private val _selectedSector = MutableStateFlow("All")
    val selectedSector: StateFlow<String> = _selectedSector.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _maxDistanceFilter = MutableStateFlow(5.0f) // percent
    val maxDistanceFilter: StateFlow<Float> = _maxDistanceFilter.asStateFlow()

    private val _selectedStockSymbol = MutableStateFlow<String?>("RELIANCE")
    val selectedStockSymbol: StateFlow<String?> = _selectedStockSymbol.asStateFlow()

    private val _selectedStockCandles = MutableStateFlow<List<Candle>>(emptyList())
    val selectedStockCandles: StateFlow<List<Candle>> = _selectedStockCandles.asStateFlow()

    val allStocks: StateFlow<List<Stock>> = stockRepository.stocks

    init {
        // Monitor live price ticks to evaluate real-time alerts
        viewModelScope.launch {
            stockRepository.stocks.collect { stocks ->
                if (stocks.isNotEmpty()) {
                    alertRepository.checkMarketPrices(stocks)
                    // Update candles for currently viewed stock if open
                    val currentSym = _selectedStockSymbol.value
                    if (currentSym != null) {
                        val stock = stocks.find { it.symbol == currentSym }
                        if (stock != null) {
                            _selectedStockCandles.value = stockRepository.getCandlesForTimeframe(currentSym, _timeframe.value)
                        }
                    }
                }
            }
        }
    }

    val filteredStocks: StateFlow<List<Stock>> = combine(
        stockRepository.stocks,
        _scanType,
        _selectedSector,
        _searchQuery,
        _maxDistanceFilter
    ) { stocks, scan, sector, query, maxDist ->
        stocks.filter { stock ->
            // Search query filter
            val matchesQuery = query.isEmpty() ||
                    stock.symbol.contains(query, ignoreCase = true) ||
                    stock.name.contains(query, ignoreCase = true)

            // Sector filter
            val matchesSector = sector == "All" || stock.sector.equals(sector, ignoreCase = true)

            // Distance filter
            val matchesDistance = abs(stock.distanceFromSmaPercent) <= maxDist

            // Scanner setup filter
            val matchesScan = when (scan) {
                ScanType.ALL -> true
                ScanType.RISING_STOCKS -> stock.smaSlope > 0.03
                ScanType.FALLING_STOCKS -> stock.smaSlope < -0.03
                ScanType.BOUNCE_SETUPS -> stock.signal == SmaSignal.RISING_BOUNCE || stock.signal == SmaSignal.FALLING_REJECT
                ScanType.BREAKOUTS -> stock.signal == SmaSignal.RISING_BREAKOUT || stock.signal == SmaSignal.FALLING_BREAKDOWN
                ScanType.NEAR_SMA -> abs(stock.distanceFromSmaPercent) <= 0.9
            }

            matchesQuery && matchesSector && matchesDistance && matchesScan
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val risingCount: StateFlow<Int> = stockRepository.stocks.combine(_timeframe) { stocks, _ ->
        stocks.count { it.smaSlope > 0.03 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val fallingCount: StateFlow<Int> = stockRepository.stocks.combine(_timeframe) { stocks, _ ->
        stocks.count { it.smaSlope < -0.03 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bounceCount: StateFlow<Int> = stockRepository.stocks.combine(_timeframe) { stocks, _ ->
        stocks.count { it.signal == SmaSignal.RISING_BOUNCE || it.signal == SmaSignal.FALLING_REJECT }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val watchlistStocks: StateFlow<List<Stock>> = stockRepository.stocks.combine(_searchQuery) { stocks, _ ->
        stocks.filter { it.isWatchlist }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setScanType(type: ScanType) {
        _scanType.value = type
    }

    fun setTimeframe(tf: Timeframe) {
        _timeframe.value = tf
        val sym = _selectedStockSymbol.value
        if (sym != null) {
            _selectedStockCandles.value = stockRepository.getCandlesForTimeframe(sym, tf)
        }
    }

    fun setSector(sector: String) {
        _selectedSector.value = sector
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMaxDistanceFilter(dist: Float) {
        _maxDistanceFilter.value = dist
    }

    fun selectStock(symbol: String) {
        _selectedStockSymbol.value = symbol
        _selectedStockCandles.value = stockRepository.getCandlesForTimeframe(symbol, _timeframe.value)
    }

    fun toggleWatchlist(symbol: String) {
        stockRepository.toggleWatchlist(symbol)
    }

    fun toggleLiveStreaming() {
        stockRepository.setLiveStreaming(!isLiveStreaming.value)
    }

    fun setStreamSpeed(speed: Int) {
        stockRepository.setStreamSpeed(speed)
    }

    fun refreshRealNsePrices() {
        stockRepository.refreshRealNsePrices()
    }

    class Factory(
        private val stockRepository: StockRepository,
        private val alertRepository: AlertRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarketViewModel(stockRepository, alertRepository) as T
        }
    }
}
