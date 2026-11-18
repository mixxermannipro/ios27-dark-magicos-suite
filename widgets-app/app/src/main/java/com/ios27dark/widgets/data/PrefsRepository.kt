package com.ios27dark.widgets.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HourlyForecast(
    val hourLabel: String,
    val tempC: Int,
    val weatherCode: Int
)

data class WeatherData(
    val city: String,
    val tempC: Int,
    val weatherCode: Int,
    val highC: Int,
    val lowC: Int,
    val isDay: Boolean,
    val hourly: List<HourlyForecast>,
    val updatedAt: Long
)

data class StockQuote(
    val symbol: String,
    val displayName: String,
    val price: Double,
    val changePct: Double,
    val sparkline: List<Float>,
    val updatedAt: Long
)

data class ReminderItem(
    val id: String,
    val title: String,
    val done: Boolean
)

data class WorldZone(
    val city: String,
    val zoneId: String
)

data class BtBatteryDevice(
    val name: String,
    val address: String,
    val level: Int,
    val kind: String // "audio", "watch", "other"
)

data class MediaState(
    val title: String,
    val artist: String,
    val sourceApp: String,
    val isPlaying: Boolean,
    val hasActiveSession: Boolean,
    val hasPermission: Boolean
)

object PrefsRepository {
    private const val PREFS_NAME = "ios27_widgets_prefs"

    private const val KEY_WEATHER = "weather_json"
    private const val KEY_MANUAL_CITY = "manual_city"
    private const val KEY_MANUAL_LAT = "manual_lat"
    private const val KEY_MANUAL_LON = "manual_lon"
    private const val KEY_STOCK_TICKERS = "stock_tickers"
    private const val KEY_STOCK_QUOTES = "stock_quotes"
    private const val KEY_REMINDERS = "reminders_json"
    private const val KEY_PHOTO_URIS = "photo_uris"
    private const val KEY_PHOTO_INDEX = "photo_index"
    private const val KEY_WORLD_ZONES = "world_zones"
    private const val KEY_BT_DEVICES = "bt_devices"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Weather ---
    fun getWeather(ctx: Context): WeatherData {
        val raw = prefs(ctx).getString(KEY_WEATHER, null)
        if (raw != null) {
            try {
                val obj = JSONObject(raw)
                val arr = obj.optJSONArray("hourly") ?: JSONArray()
                val hourly = (0 until arr.length()).map { i ->
                    val h = arr.getJSONObject(i)
                    HourlyForecast(h.getString("hourLabel"), h.getInt("tempC"), h.getInt("weatherCode"))
                }
                return WeatherData(
                    city = obj.getString("city"),
                    tempC = obj.getInt("tempC"),
                    weatherCode = obj.getInt("weatherCode"),
                    highC = obj.getInt("highC"),
                    lowC = obj.getInt("lowC"),
                    isDay = obj.optBoolean("isDay", true),
                    hourly = hourly,
                    updatedAt = obj.optLong("updatedAt", 0L)
                )
            } catch (_: Exception) {
            }
        }
        return WeatherData(
            city = getManualCity(ctx) ?: "München",
            tempC = 12,
            weatherCode = 2,
            highC = 15,
            lowC = 6,
            isDay = true,
            hourly = listOf(
                HourlyForecast("17h", 12, 2),
                HourlyForecast("18h", 11, 2),
                HourlyForecast("19h", 10, 3),
                HourlyForecast("20h", 9, 3),
                HourlyForecast("21h", 8, 1)
            ),
            updatedAt = 0L
        )
    }

    fun saveWeather(ctx: Context, w: WeatherData) {
        val arr = JSONArray()
        w.hourly.forEach { h ->
            arr.put(JSONObject().apply {
                put("hourLabel", h.hourLabel)
                put("tempC", h.tempC)
                put("weatherCode", h.weatherCode)
            })
        }
        val obj = JSONObject().apply {
            put("city", w.city)
            put("tempC", w.tempC)
            put("weatherCode", w.weatherCode)
            put("highC", w.highC)
            put("lowC", w.lowC)
            put("isDay", w.isDay)
            put("hourly", arr)
            put("updatedAt", w.updatedAt)
        }
        prefs(ctx).edit().putString(KEY_WEATHER, obj.toString()).apply()
    }

    fun setManualLocation(ctx: Context, city: String, lat: Double, lon: Double) {
        prefs(ctx).edit()
            .putString(KEY_MANUAL_CITY, city)
            .putFloat(KEY_MANUAL_LAT, lat.toFloat())
            .putFloat(KEY_MANUAL_LON, lon.toFloat())
            .apply()
    }

    fun getManualCity(ctx: Context): String? = prefs(ctx).getString(KEY_MANUAL_CITY, null)

    fun getManualCoords(ctx: Context): Pair<Double, Double>? {
        val p = prefs(ctx)
        if (!p.contains(KEY_MANUAL_LAT) || !p.contains(KEY_MANUAL_LON)) return null
        return p.getFloat(KEY_MANUAL_LAT, 48.137f).toDouble() to p.getFloat(KEY_MANUAL_LON, 11.576f).toDouble()
    }

    // --- Stocks ---
    fun getStockTickers(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY_STOCK_TICKERS, null)
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                val list = (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {
            }
        }
        return listOf("^GSPC", "NVDA", "AAPL", "URTH")
    }

    fun saveStockTickers(ctx: Context, tickers: List<String>) {
        val clean = tickers.map { it.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
        val arr = JSONArray()
        clean.forEach { arr.put(it) }
        prefs(ctx).edit().putString(KEY_STOCK_TICKERS, arr.toString()).apply()
    }

    fun getStockQuotes(ctx: Context): List<StockQuote> {
        val raw = prefs(ctx).getString(KEY_STOCK_QUOTES, null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                val list = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    val sArr = o.optJSONArray("sparkline") ?: JSONArray()
                    val pts = (0 until sArr.length()).map { sArr.getDouble(it).toFloat() }
                    StockQuote(
                        symbol = o.getString("symbol"),
                        displayName = o.getString("displayName"),
                        price = o.getDouble("price"),
                        changePct = o.getDouble("changePct"),
                        sparkline = pts,
                        updatedAt = o.optLong("updatedAt", 0L)
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {
            }
        }
        // Fallback defaults before first live fetch
        return listOf(
            StockQuote("^GSPC", "S&P 500", 5982.40, 0.64, listOf(5910f, 5925f, 5918f, 5942f, 5955f, 5970f, 5982f), 0L),
            StockQuote("NVDA", "NVIDIA", 142.85, 2.15, listOf(137.2f, 138.9f, 138.1f, 140.4f, 141.0f, 142.1f, 142.85f), 0L),
            StockQuote("AAPL", "Apple", 228.10, -0.38, listOf(230.1f, 229.6f, 229.9f, 228.8f, 228.4f, 228.5f, 228.1f), 0L),
            StockQuote("URTH", "MSCI World", 156.40, 0.51, listOf(154.8f, 155.1f, 155.0f, 155.6f, 155.9f, 156.2f, 156.4f), 0L)
        )
    }

    fun saveStockQuotes(ctx: Context, quotes: List<StockQuote>) {
        val arr = JSONArray()
        quotes.forEach { q ->
            val sArr = JSONArray()
            q.sparkline.forEach { sArr.put(it.toDouble()) }
            arr.put(JSONObject().apply {
                put("symbol", q.symbol)
                put("displayName", q.displayName)
                put("price", q.price)
                put("changePct", q.changePct)
                put("sparkline", sArr)
                put("updatedAt", q.updatedAt)
            })
        }
        prefs(ctx).edit().putString(KEY_STOCK_QUOTES, arr.toString()).apply()
    }

    // --- Reminders ---
    fun getReminders(ctx: Context): List<ReminderItem> {
        val raw = prefs(ctx).getString(KEY_REMINDERS, null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                return (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    ReminderItem(o.getString("id"), o.getString("title"), o.getBoolean("done"))
                }
            } catch (_: Exception) {
            }
        }
        val defaults = listOf(
            ReminderItem("1", "Honor Theme .hnt anwenden", false),
            ReminderItem("2", "Benachrichtigungszugriff aktivieren", false),
            ReminderItem("3", "Aktien-Watchlist prüfen", true),
            ReminderItem("4", "Lieblingsfotos für Widget wählen", false)
        )
        saveReminders(ctx, defaults)
        return defaults
    }

    fun saveReminders(ctx: Context, items: List<ReminderItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("done", item.done)
            })
        }
        prefs(ctx).edit().putString(KEY_REMINDERS, arr.toString()).apply()
    }

    fun toggleReminder(ctx: Context, index: Int) {
        val list = getReminders(ctx).toMutableList()
        if (index in list.indices) {
            val cur = list[index]
            list[index] = cur.copy(done = !cur.done)
            saveReminders(ctx, list)
        }
    }

    // --- Photos ---
    fun getPhotoUris(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY_PHOTO_URIS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePhotoUris(ctx: Context, uris: List<String>) {
        val arr = JSONArray()
        uris.forEach { arr.put(it) }
        prefs(ctx).edit().putString(KEY_PHOTO_URIS, arr.toString()).putInt(KEY_PHOTO_INDEX, 0).apply()
    }

    fun getPhotoIndex(ctx: Context): Int = prefs(ctx).getInt(KEY_PHOTO_INDEX, 0)

    fun advancePhotoIndex(ctx: Context): Int {
        val uris = getPhotoUris(ctx)
        if (uris.isEmpty()) return 0
        val next = (getPhotoIndex(ctx) + 1) % uris.size
        prefs(ctx).edit().putInt(KEY_PHOTO_INDEX, next).apply()
        return next
    }

    // --- World Clock ---
    fun getWorldZones(ctx: Context): List<WorldZone> {
        val raw = prefs(ctx).getString(KEY_WORLD_ZONES, null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                val list = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    WorldZone(o.getString("city"), o.getString("zoneId"))
                }
                if (list.size >= 4) return list
            } catch (_: Exception) {
            }
        }
        return listOf(
            WorldZone("Berlin", "Europe/Berlin"),
            WorldZone("New York", "America/New_York"),
            WorldZone("Tokio", "Asia/Tokyo"),
            WorldZone("Shanghai", "Asia/Shanghai")
        )
    }

    fun saveWorldZones(ctx: Context, zones: List<WorldZone>) {
        val arr = JSONArray()
        zones.forEach { z ->
            arr.put(JSONObject().apply {
                put("city", z.city)
                put("zoneId", z.zoneId)
            })
        }
        prefs(ctx).edit().putString(KEY_WORLD_ZONES, arr.toString()).apply()
    }

    // --- Bluetooth Devices ---
    fun getCachedBtDevices(ctx: Context): List<BtBatteryDevice> {
        val raw = prefs(ctx).getString(KEY_BT_DEVICES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                BtBatteryDevice(
                    o.getString("name"),
                    o.getString("address"),
                    o.getInt("level"),
                    o.optString("kind", "audio")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCachedBtDevices(ctx: Context, devices: List<BtBatteryDevice>) {
        val arr = JSONArray()
        devices.forEach { d ->
            arr.put(JSONObject().apply {
                put("name", d.name)
                put("address", d.address)
                put("level", d.level)
                put("kind", d.kind)
            })
        }
        prefs(ctx).edit().putString(KEY_BT_DEVICES, arr.toString()).apply()
    }
}
