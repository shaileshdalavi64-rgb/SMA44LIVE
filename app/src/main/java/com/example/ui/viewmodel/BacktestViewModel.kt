package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BacktestEntity
import com.example.data.model.BacktestConfig
import com.example.data.model.BacktestResult
import com.example.data.model.BacktestStrategy
import com.example.data.model.Timeframe
import com.example.data.model.TradeDirection
import com.example.data.repository.BacktestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BacktestViewModel(
    private val backtestRepository: BacktestRepository
) : ViewModel() {

    private val _config = MutableStateFlow(
        BacktestConfig(
            symbol = "RELIANCE",
            strategy = BacktestStrategy.SMA44_BOUNCE,
            direction = TradeDirection.BOTH,
            timeframe = Timeframe.DAILY,
            periodMonths = 12,
            initialCapital = 100000.0,
            riskPerTradePercent = 2.0,
            targetRatio = 2.0,
            useTrailingSma = false
        )
    )
    val config: StateFlow<BacktestConfig> = _config.asStateFlow()

    private val _result = MutableStateFlow<BacktestResult?>(null)
    val result: StateFlow<BacktestResult?> = _result.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _saveStatusMessage = MutableStateFlow<String?>(null)
    val saveStatusMessage: StateFlow<String?> = _saveStatusMessage.asStateFlow()

    val savedBacktests: StateFlow<List<BacktestEntity>> = backtestRepository.savedBacktests
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Run initial default backtest on startup so the screen is immediately populated with rich analytics
        runBacktest()
    }

    fun updateSymbol(symbol: String) {
        _config.value = _config.value.copy(symbol = symbol)
    }

    fun updateStrategy(strategy: BacktestStrategy) {
        _config.value = _config.value.copy(strategy = strategy)
    }

    fun updateDirection(direction: TradeDirection) {
        _config.value = _config.value.copy(direction = direction)
    }

    fun updateTimeframe(timeframe: Timeframe) {
        _config.value = _config.value.copy(timeframe = timeframe)
    }

    fun updatePeriodMonths(months: Int) {
        _config.value = _config.value.copy(periodMonths = months)
    }

    fun updateCapital(capital: Double) {
        _config.value = _config.value.copy(initialCapital = capital)
    }

    fun updateRiskPercent(risk: Double) {
        _config.value = _config.value.copy(riskPerTradePercent = risk)
    }

    fun updateTargetRatio(ratio: Double) {
        _config.value = _config.value.copy(targetRatio = ratio)
    }

    fun updateTrailingSma(enabled: Boolean) {
        _config.value = _config.value.copy(useTrailingSma = enabled)
    }

    fun runBacktest() {
        viewModelScope.launch {
            _isRunning.value = true
            try {
                val res = backtestRepository.runBacktest(_config.value)
                _result.value = res
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun saveCurrentResult() {
        val current = _result.value ?: return
        viewModelScope.launch {
            backtestRepository.saveBacktest(current)
            _saveStatusMessage.value = "Backtest saved to library!"
        }
    }

    fun clearSaveMessage() {
        _saveStatusMessage.value = null
    }

    fun deleteSavedBacktest(id: Long) {
        viewModelScope.launch {
            backtestRepository.deleteBacktest(id)
        }
    }

    class Factory(
        private val backtestRepository: BacktestRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BacktestViewModel(backtestRepository) as T
        }
    }
}
