package com.ios27dark.widgets.data

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object StocksClient {

    private val FRIENDLY_NAMES = mapOf(
        "^GSPC" to "S&P 500",
        "^DJI" to "Dow Jones",
        "^IXIC" to "NASDAQ",
        "^GDAXI" to "DAX",
        "NVDA" to "NVIDIA",
        "AAPL" to "Apple",
        "MSFT" to "Microsoft",
        "AMZN" to "Amazon",
        "GOOGL" to "Alphabet",
        "TSLA" to "Tesla",
        "META" to "Meta",
        "URTH" to "MSCI World",
        "IWDA.AS" to "MSCI World",
        "EUNL.DE" to "MSCI World",
        "BTC-USD" to "Bitcoin",
        "ETH-USD" to "Ethereum"
    )

    fun displayNameFor(symbol: String, apiShortName: String? = null): String {
        return FRIENDLY_NAMES[symbol.uppercase()]
            ?: apiShortName?.takeIf { it.isNotBlank() }
            ?: symbol.uppercase()
    }

    fun fetchAndCacheStocks(ctx: Context): List<StockQuote> {
        val tickers = PrefsRepository.getStockTickers(ctx)
        val cached = PrefsRepository.getStockQuotes(ctx).associateBy { it.symbol.uppercase() }
        val result = mutableListOf<StockQuote>()

        for (ticker in tickers) {
            val quote = fetchSingleQuote(ticker) ?: cached[ticker.uppercase()]
            if (quote != null) {
                result.add(quote)
            }
        }
        if (result.isNotEmpty()) {
            PrefsRepository.saveStockQuotes(ctx, result)
        }
        return if (result.isNotEmpty()) result else PrefsRepository.getStockQuotes(ctx)
    }

    private fun fetchSingleQuote(symbol: String): StockQuote? {
        val encoded = URLEncoder.encode(symbol.trim(), "UTF-8")
        val hosts = listOf("query1.finance.yahoo.com", "query2.finance.yahoo.com")
        for (host in hosts) {
            try {
                val url = "https://$host/v8/finance/chart/$encoded?range=5d&interval=1h"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    requestMethod = "GET"
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36"
                    )
                }
                val body = try {
                    if (conn.responseCode !in 200..299) continue
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }

                val root = JSONObject(body)
                val resArr = root.getJSONObject("chart").optJSONArray("result") ?: continue
                if (resArr.length() == 0) continue
                val resObj = resArr.getJSONObject(0)
                val meta = resObj.getJSONObject("meta")

                val price = meta.optDouble("regularMarketPrice", Double.NaN)
                if (price.isNaN()) continue
                val prevClose = meta.optDouble("chartPreviousClose",
                    meta.optDouble("previousClose", price))
                val pct = if (prevClose > 0.0) ((price - prevClose) / prevClose) * 100.0 else 0.0

                val closes = mutableListOf<Float>()
                val quotesArr = resObj.optJSONObject("indicators")?.optJSONArray("quote")
                if (quotesArr != null && quotesArr.length() > 0) {
                    val closeArr = quotesArr.getJSONObject(0).optJSONArray("close")
                    if (closeArr != null) {
                        for (i in 0 until closeArr.length()) {
                            if (!closeArr.isNull(i)) {
                                closes.add(closeArr.getDouble(i).toFloat())
                            }
                        }
                    }
                }
                if (closes.size < 2) {
                    closes.clear()
                    closes.add(prevClose.toFloat())
                    closes.add(price.toFloat())
                }
                // Downsample to max 28 points for crisp sparkline rendering
                val sampled = if (closes.size > 28) {
                    val step = closes.size / 28.0
                    (0 until 28).map { i -> closes[(i * step).toInt().coerceAtMost(closes.lastIndex)] }
                } else {
                    closes
                }

                val apiName = meta.optString("shortName", "")
                return StockQuote(
                    symbol = symbol.uppercase(),
                    displayName = displayNameFor(symbol, apiName),
                    price = price,
                    changePct = pct,
                    sparkline = sampled,
                    updatedAt = System.currentTimeMillis()
                )
            } catch (_: Exception) {
            }
        }
        return null
    }
}
