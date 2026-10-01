package com.example.data.repository

import com.example.data.local.dao.AlertDao
import com.example.data.local.entity.AlertEntity
import com.example.data.model.AlertConditionType
import com.example.data.model.AlertRule
import com.example.data.model.Stock
import com.example.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class AlertRepository(
    private val alertDao: AlertDao,
    private val notificationHelper: NotificationHelper,
    private val scope: CoroutineScope
) {
    val alerts: Flow<List<AlertRule>> = alertDao.getAllAlerts().map { list ->
        list.map { it.toModel() }
    }

    private val _alertTriggeredEvents = MutableSharedFlow<AlertRule>(extraBufferCapacity = 10)
    val alertTriggeredEvents: SharedFlow<AlertRule> = _alertTriggeredEvents.asSharedFlow()

    private val activeAlertsCache = ConcurrentHashMap<Long, AlertRule>()

    init {
        scope.launch(Dispatchers.IO) {
            alertDao.getActiveAlerts().collect { list ->
                activeAlertsCache.clear()
                list.forEach { entity ->
                    val model = entity.toModel()
                    activeAlertsCache[model.id] = model
                }
            }
        }
    }

    suspend fun createAlert(alert: AlertRule): Long {
        val entity = AlertEntity.fromModel(alert)
        return alertDao.insertAlert(entity)
    }

    suspend fun toggleAlertStatus(alert: AlertRule) {
        val updated = alert.copy(isActive = !alert.isActive)
        alertDao.updateAlert(AlertEntity.fromModel(updated))
    }

    suspend fun deleteAlert(alertId: Long) {
        alertDao.deleteAlertById(alertId)
    }

    suspend fun clearAllAlerts() {
        alertDao.clearAll()
    }

    fun checkMarketPrices(stocks: List<Stock>) {
        if (activeAlertsCache.isEmpty()) return
        val stockMap = stocks.associateBy { it.symbol }

        for ((_, alert) in activeAlertsCache) {
            val stock = stockMap[alert.symbol] ?: continue
            val isTriggered = evaluateCondition(alert, stock)

            if (isTriggered) {
                scope.launch(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    alertDao.markAlertTriggered(alert.id, now)
                    activeAlertsCache.remove(alert.id)

                    val triggeredAlert = alert.copy(
                        isTriggered = true,
                        isActive = false,
                        triggeredAt = now,
                        currentLtp = stock.ltp,
                        smaValue = stock.sma44
                    )
                    _alertTriggeredEvents.tryEmit(triggeredAlert)
                    notificationHelper.triggerAlertNotification(triggeredAlert)
                }
            }
        }
    }

    private fun evaluateCondition(alert: AlertRule, stock: Stock): Boolean {
        val distancePct = abs(stock.distanceFromSmaPercent)
        return when (alert.condition) {
            AlertConditionType.PRICE_TOUCH_SMA44 -> {
                distancePct <= 0.4
            }
            AlertConditionType.PRICE_CROSS_ABOVE_SMA44 -> {
                stock.ltp > stock.sma44 && (stock.ltp - stock.sma44) / stock.sma44 < 0.015
            }
            AlertConditionType.PRICE_CROSS_BELOW_SMA44 -> {
                stock.ltp < stock.sma44 && (stock.sma44 - stock.ltp) / stock.sma44 < 0.015
            }
            AlertConditionType.SMA44_BOUNCE_CONFIRMED -> {
                stock.smaSlope > 0.04 && distancePct <= 0.5 && stock.changePercent > 0.3
            }
            AlertConditionType.SMA44_REJECT_CONFIRMED -> {
                stock.smaSlope < -0.04 && distancePct <= 0.5 && stock.changePercent < -0.3
            }
            AlertConditionType.PRICE_TARGET_HIT -> {
                if (alert.targetValue > alert.currentLtp) {
                    stock.ltp >= alert.targetValue
                } else {
                    stock.ltp <= alert.targetValue
                }
            }
            AlertConditionType.STOP_LOSS_HIT -> {
                if (alert.targetValue < alert.currentLtp) {
                    stock.ltp <= alert.targetValue
                } else {
                    stock.ltp >= alert.targetValue
                }
            }
        }
    }
}
