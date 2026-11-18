package com.ios27dark.widgets

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ios27dark.widgets.data.BatteryHelper
import com.ios27dark.widgets.data.CalendarHelper
import com.ios27dark.widgets.data.MediaControllerHelper
import com.ios27dark.widgets.data.PrefsRepository
import com.ios27dark.widgets.data.ReminderItem
import com.ios27dark.widgets.data.StocksClient
import com.ios27dark.widgets.data.SystemEventReceiver
import com.ios27dark.widgets.data.WeatherClient
import com.ios27dark.widgets.render.WidgetCanvasRenderer
import com.ios27dark.widgets.widgets.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var contentLayout: LinearLayout

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshAllDataAndUi()
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val saved = mutableListOf<String>()
            for (uri in uris) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                saved.add(uri.toString())
            }
            PrefsRepository.savePhotoUris(this, saved)
            WidgetUpdater.updatePhotosWidgets(this)
            Toast.makeText(this, "${saved.size} Foto(s) für Widget gespeichert", Toast.LENGTH_SHORT).show()
            renderScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemEventReceiver.schedulePeriodicUpdates(this)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#09090B"))
            isFillViewport = true
        }
        contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(42), dp(18), dp(36))
        }
        scroll.addView(contentLayout)
        setContentView(scroll)

        renderScreen()
        refreshLiveDataInBackground()
    }

    override fun onResume() {
        super.onResume()
        renderScreen()
        WidgetUpdater.updateAllWidgets(this)
    }

    private fun refreshAllDataAndUi() {
        renderScreen()
        refreshLiveDataInBackground()
    }

    private fun refreshLiveDataInBackground() {
        CoroutineScope(Dispatchers.IO).launch {
            WeatherClient.fetchAndCacheWeather(applicationContext)
            StocksClient.fetchAndCacheStocks(applicationContext)
            withContext(Dispatchers.Main) {
                WidgetUpdater.updateAllWidgets(applicationContext)
                renderScreen()
            }
        }
    }

    private fun renderScreen() {
        contentLayout.removeAllViews()

        // Title header
        contentLayout.addView(TextView(this).apply {
            text = "iOS 27 Widgets"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        contentLayout.addView(TextView(this).apply {
            text = "Dark Suite für Honor MagicOS & Android · 16 Live-Widgets"
            textSize = 13f
            setTextColor(Color.parseColor("#AEAEB2"))
            setPadding(0, dp(4), 0, dp(14))
        })

        // Global action bar
        val topBtns = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(16))
        }
        topBtns.addView(pillButton("Alle Widgets aktualisieren", "#0A84FF") {
            refreshLiveDataInBackground()
            Toast.makeText(this, "Aktualisiere Wetter, Aktien & Widgets…", Toast.LENGTH_SHORT).show()
        })
        contentLayout.addView(topBtns)

        // 1. Permissions Card
        addSectionTitle("Berechtigungen (1-Klick)")
        contentLayout.addView(buildPermissionsCard())

        // 2. Widget Preview & Pin Section
        addSectionTitle("Widget-Vorschau & Homescreen")
        contentLayout.addView(buildWidgetPreviewCard())

        // 3. Weather City Card
        addSectionTitle("Wetter-Standort")
        contentLayout.addView(buildWeatherCard())

        // 4. Stocks Watchlist Card
        addSectionTitle("Aktien & Watchlist (Yahoo Finance)")
        contentLayout.addView(buildStocksCard())

        // 5. Reminders Card
        addSectionTitle("Erinnerungen / To-Do")
        contentLayout.addView(buildRemindersCard())

        // 6. Photos Card
        addSectionTitle("Fotos-Widget")
        contentLayout.addView(buildPhotosCard())
    }

    private fun buildPermissionsCard(): LinearLayout {
        val card = createCard()
        val hasLoc = hasPerm(Manifest.permission.ACCESS_FINE_LOCATION) || hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasCal = CalendarHelper.hasCalendarPermission(this)
        val hasMediaListener = MediaControllerHelper.hasNotificationAccess(this)
        val hasBt = BatteryHelper.hasBluetoothPermission(this)
        val hasPhotos = if (Build.VERSION.SDK_INT >= 33) {
            hasPerm(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            hasPerm(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        card.addView(permissionRow("Standort (Wetter)", hasLoc) {
            permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        })
        card.addView(permissionRow("Kalender (Termine)", hasCal) {
            permLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR))
        })
        card.addView(permissionRow("Musik / Benachrichtigungen", hasMediaListener) {
            MediaControllerHelper.openNotificationAccessSettings(this)
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            card.addView(permissionRow("Bluetooth-Geräte (Akku)", hasBt) {
                permLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
            })
        }
        card.addView(permissionRow("Speicher / Fotos", hasPhotos) {
            val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
            permLauncher.launch(arrayOf(p))
        })
        card.addView(permissionRow("Akku-Schutz (MagicOS Tipp)", false, buttonLabel = "Öffnen") {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        })
        return card
    }

    private fun buildWidgetPreviewCard(): LinearLayout {
        val card = createCard()
        val w = PrefsRepository.getWeather(this)
        val bat = BatteryHelper.getPhoneBattery(this)
        val quotes = PrefsRepository.getStockQuotes(this)
        val firstQuote = quotes.firstOrNull()
        val dateStr = SimpleDateFormat("EEEE, d. MMM", Locale.GERMANY).format(Date())

        // Live preview mini-dashboard inside app
        val previewRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        val ringBmp = WidgetCanvasRenderer.renderBatteryRing(bat.level, bat.isCharging, "phone", 140, true)
        previewRow.addView(ImageView(this).apply {
            setImageBitmap(ringBmp)
            layoutParams = LinearLayout.LayoutParams(dp(54), dp(54))
        })

        val summaryCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        summaryCol.addView(TextView(this).apply {
            text = "${w.city}: ${w.tempC}° (${WeatherClient.wmoDescriptionDe(w.weatherCode)})"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        summaryCol.addView(TextView(this).apply {
            val stockLine = if (firstQuote != null) "${firstQuote.displayName}: ${String.format(Locale.US, "%.2f", firstQuote.price)}" else ""
            text = "$dateStr · $stockLine"
            textSize = 12f
            setTextColor(Color.parseColor("#AEAEB2"))
        })
        previewRow.addView(summaryCol)
        card.addView(previewRow)

        val widgets = listOf(
            "Uhr Analog (2×2)" to ClockAnalogWidgetProvider::class.java,
            "Uhr Digital (2×2)" to ClockDigitalWidgetProvider::class.java,
            "Weltuhr (4×2)" to WorldClockWidgetProvider::class.java,
            "Wetter Klein (2×2)" to WeatherSmallWidgetProvider::class.java,
            "Wetter Mittel (4×2)" to WeatherMediumWidgetProvider::class.java,
            "Kalender Klein (2×2)" to CalendarSmallWidgetProvider::class.java,
            "Kalender Mittel (4×2)" to CalendarMediumWidgetProvider::class.java,
            "Batterie Klein (2×2)" to BatterySmallWidgetProvider::class.java,
            "Batterie Mittel (4×2)" to BatteryMediumWidgetProvider::class.java,
            "Aktien Klein (2×2)" to StocksSmallWidgetProvider::class.java,
            "Aktien Mittel (4×2)" to StocksMediumWidgetProvider::class.java,
            "Musik / Now Playing (4×2)" to MusicWidgetProvider::class.java,
            "Erinnerungen Klein (2×2)" to RemindersSmallWidgetProvider::class.java,
            "Erinnerungen Mittel (4×2)" to RemindersMediumWidgetProvider::class.java,
            "Fotos Klein (2×2)" to PhotosSmallWidgetProvider::class.java,
            "Fotos Groß (4×4)" to PhotosLargeWidgetProvider::class.java
        )

        for ((label, cls) in widgets) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            row.addView(TextView(this).apply {
                text = label
                textSize = 14f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(pillButton("Hinzufügen", "#2C2C2E", "#0A84FF") {
                pinWidget(cls, label)
            })
            card.addView(row)
        }
        return card
    }

    private fun buildWeatherCard(): LinearLayout {
        val card = createCard()
        val w = PrefsRepository.getWeather(this)
        card.addView(TextView(this).apply {
            text = "Aktuell: ${w.city} (${w.tempC}°C, H:${w.highC}° / T:${w.lowC}°)"
            textSize = 13f
            setTextColor(Color.parseColor("#AEAEB2"))
            setPadding(0, 0, 0, dp(10))
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = darkEditText("Stadt eingeben (z. B. Berlin, Wien, Zürich)")
        input.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        }
        row.addView(input)
        row.addView(pillButton("Setzen", "#0A84FF") {
            val q = input.text.toString().trim()
            if (q.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val ok = WeatherClient.searchAndSetCity(applicationContext, q)
                    withContext(Dispatchers.Main) {
                        if (ok) {
                            WidgetUpdater.updateWeatherWidgets(applicationContext)
                            renderScreen()
                            Toast.makeText(this@MainActivity, "Standort aktualisiert", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Ort nicht gefunden", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
        card.addView(row)
        return card
    }

    private fun buildStocksCard(): LinearLayout {
        val card = createCard()
        val tickers = PrefsRepository.getStockTickers(this)

        for (ticker in tickers) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, dp(4))
            }
            row.addView(TextView(this).apply {
                text = "${StocksClient.displayNameFor(ticker)} ($ticker)"
                textSize = 14f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (tickers.size > 1) {
                row.addView(pillButton("Entfernen", "#2C2C2E", "#FF3B30") {
                    val updated = tickers.filter { it != ticker }
                    PrefsRepository.saveStockTickers(this, updated)
                    refreshLiveDataInBackground()
                })
            }
            card.addView(row)
        }

        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        val input = darkEditText("Ticker (z. B. MSFT, ^GDAXI, BTC-USD)")
        input.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        }
        addRow.addView(input)
        addRow.addView(pillButton("+ Ticker", "#30D158", "#09090B") {
            val sym = input.text.toString().trim().uppercase()
            if (sym.isNotEmpty()) {
                PrefsRepository.saveStockTickers(this, tickers + sym)
                refreshLiveDataInBackground()
            }
        })
        card.addView(addRow)
        return card
    }

    private fun buildRemindersCard(): LinearLayout {
        val card = createCard()
        val items = PrefsRepository.getReminders(this)

        items.forEachIndexed { idx, item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, dp(5))
            }
            val titleView = TextView(this).apply {
                text = (if (item.done) "✓ " else "○ ") + item.title
                textSize = 14f
                setTextColor(if (item.done) Color.parseColor("#8E8E93") else Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    PrefsRepository.toggleReminder(this@MainActivity, idx)
                    WidgetUpdater.updateRemindersWidgets(this@MainActivity)
                    renderScreen()
                }
            }
            row.addView(titleView)
            row.addView(pillButton("×", "#2C2C2E", "#FF3B30") {
                val updated = items.toMutableList().apply { removeAt(idx) }
                PrefsRepository.saveReminders(this, updated)
                WidgetUpdater.updateRemindersWidgets(this)
                renderScreen()
            })
            card.addView(row)
        }

        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        val input = darkEditText("Neue Aufgabe hinzufügen…")
        input.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        }
        addRow.addView(input)
        addRow.addView(pillButton("+ Aufgabe", "#0A84FF") {
            val txt = input.text.toString().trim()
            if (txt.isNotEmpty()) {
                val updated = items + ReminderItem(UUID.randomUUID().toString(), txt, false)
                PrefsRepository.saveReminders(this, updated)
                WidgetUpdater.updateRemindersWidgets(this)
                renderScreen()
            }
        })
        card.addView(addRow)
        return card
    }

    private fun buildPhotosCard(): LinearLayout {
        val card = createCard()
        val uris = PrefsRepository.getPhotoUris(this)
        card.addView(TextView(this).apply {
            text = if (uris.isEmpty()) "Noch keine Galerie-Fotos gewählt (zeigt iOS 27 Dark Artwork)."
            else "${uris.size} Foto(s) ausgewählt. Tippen auf dem Homescreen wechselt zum nächsten Bild."
            textSize = 13f
            setTextColor(Color.parseColor("#AEAEB2"))
            setPadding(0, 0, 0, dp(10))
        })

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(pillButton("Fotos auswählen", "#0A84FF") {
            photoPickerLauncher.launch(arrayOf("image/*"))
        })
        if (uris.isNotEmpty()) {
            val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) }
            row.addView(spacer)
            row.addView(pillButton("Zurücksetzen", "#2C2C2E", "#FF3B30") {
                PrefsRepository.savePhotoUris(this, emptyList())
                WidgetUpdater.updatePhotosWidgets(this)
                renderScreen()
            })
        }
        card.addView(row)
        return card
    }

    private fun pinWidget(cls: Class<*>, label: String) {
        val mgr = AppWidgetManager.getInstance(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, cls), null, null)
        } else {
            Toast.makeText(
                this,
                "Bitte „$label“ direkt über die MagicOS Widget-Auswahl auf dem Homescreen hinzufügen.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun permissionRow(
        title: String,
        granted: Boolean,
        buttonLabel: String = if (granted) "Erteilt" else "Erlauben",
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 14f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            val bgHex = if (granted) "#1E3A24" else "#0A84FF"
            val fgHex = if (granted) "#30D158" else "#FFFFFF"
            addView(pillButton(buttonLabel, bgHex, fgHex) { onClick() })
        }
    }

    private fun addSectionTitle(title: String) {
        contentLayout.addView(TextView(this).apply {
            text = title.uppercase()
            textSize = 11.5f
            letterSpacing = 0.08f
            setTextColor(Color.parseColor("#8E8E93"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(4), dp(14), 0, dp(8))
        })
    }

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EB161618"))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.parseColor("#2C2C2E"))
            }
        }
    }

    private fun pillButton(
        text: String,
        bgColor: String,
        textColor: String = "#FFFFFF",
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12.5f
            setTextColor(Color.parseColor(textColor))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(13), dp(8), dp(13), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun darkEditText(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 13.5f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#636366"))
            setSingleLine(true)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#232327"))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.parseColor("#323236"))
            }
        }
    }

    private fun hasPerm(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
