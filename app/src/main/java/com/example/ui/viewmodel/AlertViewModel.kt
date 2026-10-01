package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlertConditionType
import com.example.data.model.AlertRule
import com.example.data.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertViewModel(
    private val alertRepository: AlertRepository
) : ViewModel() {

    val alerts: StateFlow<List<AlertRule>> = alertRepository.alerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _bannerAlert = MutableStateFlow<AlertRule?>(null)
    val bannerAlert: StateFlow<AlertRule?> = _bannerAlert.asStateFlow()

    init {
        viewModelScope.launch {
            alertRepository.alertTriggeredEvents.collect { triggered ->
                _bannerAlert.value = triggered
            }
        }
    }

    fun dismissBanner() {
        _bannerAlert.value = null
    }

    fun createAlert(
        symbol: String,
        stockName: String,
        condition: AlertConditionType,
        targetValue: Double,
        currentLtp: Double,
        smaValue: Double
    ) {
        viewModelScope.launch {
            val alert = AlertRule(
                symbol = symbol,
                stockName = stockName,
                condition = condition,
                targetValue = targetValue,
                currentLtp = currentLtp,
                smaValue = smaValue
            )
            alertRepository.createAlert(alert)
        }
    }

    fun toggleAlert(alert: AlertRule) {
        viewModelScope.launch {
            alertRepository.toggleAlertStatus(alert)
        }
    }

    fun deleteAlert(id: Long) {
        viewModelScope.launch {
            alertRepository.deleteAlert(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            alertRepository.clearAllAlerts()
        }
    }

    class Factory(
        private val alertRepository: AlertRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlertViewModel(alertRepository) as T
        }
    }
}
