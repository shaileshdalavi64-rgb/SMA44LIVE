package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class NseQuote(
    val symbol: String,
    val regularMarketPrice: Double,
    val previousClose: Double,
    val regularMarketDayHigh: Double,
    val regularMarketDayLow: Double,
    val regularMarketVolume: Long,
    val exchangeTimezone: String = "IST"
)

class NseLivePriceService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun fetchQuote(symbol: String): NseQuote? = withContext(Dispatchers.IO) {
        val querySymbol = if (symbol.startsWith("^")) symbol else "$symbol.NS"
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$querySymbol?range=1d&interval=1m"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val chart = json.optJSONObject("chart") ?: return@withContext null
                val resultArr = chart.optJSONArray("result") ?: return@withContext null
                if (resultArr.length() == 0) return@withContext null

                val first = resultArr.getJSONObject(0)
                val meta = first.getJSONObject("meta")

                val price = meta.optDouble("regularMarketPrice", 0.0)
                val prevClose = meta.optDouble("previousClose", price)
                val high = meta.optDouble("regularMarketDayHigh", price)
                val low = meta.optDouble("regularMarketDayLow", price)
                val vol = meta.optLong("regularMarketVolume", 0L)

                if (price <= 0.0) return@withContext null

                return@withContext NseQuote(
                    symbol = symbol,
                    regularMarketPrice = price,
                    previousClose = prevClose,
                    regularMarketDayHigh = high,
                    regularMarketDayLow = low,
                    regularMarketVolume = vol
                )
            }
        } catch (_: Exception) {
            return@withContext null
        }
    }
}
