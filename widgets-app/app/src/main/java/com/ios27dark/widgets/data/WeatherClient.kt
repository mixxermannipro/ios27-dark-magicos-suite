package com.ios27dark.widgets.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object WeatherClient {

    fun wmoDescriptionDe(code: Int): String = when (code) {
        0 -> "Klar"
        1 -> "Überwiegend klar"
        2 -> "Leicht bewölkt"
        3 -> "Bewölkt"
        45, 48 -> "Nebel"
        51, 53, 55 -> "Nieselregen"
        56, 57 -> "Gefr. Niesel"
        61, 63, 65 -> "Regen"
        66, 67 -> "Eisregen"
        71, 73, 75, 77 -> "Schneefall"
        80, 81, 82 -> "Schauer"
        85, 86 -> "Schneeschauer"
        95, 96, 99 -> "Gewitter"
        else -> "Wechselhaft"
    }

    fun fetchAndCacheWeather(ctx: Context): WeatherData {
        val (lat, lon, resolvedCity) = resolveCoordinates(ctx)
        return try {
            val urlStr =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,is_day" +
                    "&hourly=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto&forecast_days=2"
            val json = httpGet(urlStr)
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            val curTemp = Math.round(current.getDouble("temperature_2m")).toInt()
            val curCode = current.getInt("weather_code")
            val isDay = current.optInt("is_day", 1) == 1
            val currentTimeStr = current.optString("time", "") // e.g. "2026-11-18T16:30"

            val daily = root.getJSONObject("daily")
            val high = Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)).toInt()
            val low = Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0)).toInt()

            val hourlyObj = root.getJSONObject("hourly")
            val times = hourlyObj.getJSONArray("time")
            val temps = hourlyObj.getJSONArray("temperature_2m")
            val codes = hourlyObj.getJSONArray("weather_code")

            val curHourPrefix = if (currentTimeStr.length >= 13) currentTimeStr.substring(0, 13) else ""
            var startIndex = 0
            if (curHourPrefix.isNotEmpty()) {
                for (i in 0 until times.length()) {
                    if (times.getString(i).startsWith(curHourPrefix)) {
                        startIndex = i + 1
                        break
                    }
                }
            }

            val nextFive = mutableListOf<HourlyForecast>()
            for (offset in 0 until 5) {
                val idx = startIndex + offset
                if (idx < times.length()) {
                    val tStr = times.getString(idx) // "2026-11-18T17:00"
                    val hourPart = if (tStr.length >= 13) tStr.substring(11, 13) + "h" else "${offset}h"
                    val tVal = Math.round(temps.getDouble(idx)).toInt()
                    val cVal = codes.getInt(idx)
                    nextFive.add(HourlyForecast(hourPart, tVal, cVal))
                }
            }

            val data = WeatherData(
                city = resolvedCity,
                tempC = curTemp,
                weatherCode = curCode,
                highC = high,
                lowC = low,
                isDay = isDay,
                hourly = nextFive,
                updatedAt = System.currentTimeMillis()
            )
            PrefsRepository.saveWeather(ctx, data)
            data
        } catch (_: Exception) {
            PrefsRepository.getWeather(ctx)
        }
    }

    fun searchAndSetCity(ctx: Context, query: String): Boolean {
        return try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val json = httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=de")
            val results = JSONObject(json).optJSONArray("results") ?: return false
            if (results.length() == 0) return false
            val first = results.getJSONObject(0)
            val name = first.getString("name")
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")
            PrefsRepository.setManualLocation(ctx, name, lat, lon)
            fetchAndCacheWeather(ctx)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveCoordinates(ctx: Context): Triple<Double, Double, String> {
        val hasFine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                var best: Location? = null
                for (p in providers) {
                    if (lm?.isProviderEnabled(p) == true) {
                        val loc = lm.getLastKnownLocation(p)
                        if (loc != null && (best == null || loc.time > best.time)) {
                            best = loc
                        }
                    }
                }
                if (best != null) {
                    val city = reverseGeocodeCity(ctx, best.latitude, best.longitude)
                        ?: PrefsRepository.getManualCity(ctx)
                        ?: "Aktueller Ort"
                    return Triple(best.latitude, best.longitude, city)
                }
            } catch (_: Exception) {
            }
        }

        val manual = PrefsRepository.getManualCoords(ctx)
        val city = PrefsRepository.getManualCity(ctx) ?: "München"
        return if (manual != null) {
            Triple(manual.first, manual.second, city)
        } else {
            Triple(48.1371, 11.5754, city)
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocodeCity(ctx: Context, lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(ctx, Locale.GERMAN)
            val list = geocoder.getFromLocation(lat, lon, 1)
            list?.firstOrNull()?.let { addr ->
                addr.locality ?: addr.subAdminArea ?: addr.adminArea
            }
        } catch (_: Exception) {
            null
        }
    }

    internal fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "iOS27DarkWidgets/1.0 (Android)")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw RuntimeException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
