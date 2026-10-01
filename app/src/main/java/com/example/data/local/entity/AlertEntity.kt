package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AlertConditionType
import com.example.data.model.AlertRule

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val stockName: String,
    val conditionName: String,
    val targetValue: Double,
    val currentLtp: Double,
    val smaValue: Double,
    val isActive: Boolean,
    val isTriggered: Boolean,
    val triggeredAt: Long?,
    val createdAt: Long
) {
    fun toModel(): AlertRule {
        val conditionType = try {
            AlertConditionType.valueOf(conditionName)
        } catch (e: Exception) {
            AlertConditionType.PRICE_TOUCH_SMA44
        }
        return AlertRule(
            id = id,
            symbol = symbol,
            stockName = stockName,
            condition = conditionType,
            targetValue = targetValue,
            currentLtp = currentLtp,
            smaValue = smaValue,
            isActive = isActive,
            isTriggered = isTriggered,
            triggeredAt = triggeredAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromModel(model: AlertRule): AlertEntity {
            return AlertEntity(
                id = model.id,
                symbol = model.symbol,
                stockName = model.stockName,
                conditionName = model.condition.name,
                targetValue = model.targetValue,
                currentLtp = model.currentLtp,
                smaValue = model.smaValue,
                isActive = model.isActive,
                isTriggered = model.isTriggered,
                triggeredAt = model.triggeredAt,
                createdAt = model.createdAt
            )
        }
    }
}
