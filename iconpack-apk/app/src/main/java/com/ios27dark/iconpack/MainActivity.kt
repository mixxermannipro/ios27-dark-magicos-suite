package com.ios27dark.iconpack

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.xmlpull.v1.XmlPullParser

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val iconNames = loadIconNamesFromXml()
        val isPickerMode = intent?.action == "org.adw.launcher.icons.ACTION_PICK_ICON"

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#09090B"))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(20)
            setPadding(p, dp(44), p, dp(32))
        }

        // Header
        container.addView(TextView(this).apply {
            text = "iOS 27 Dark Icons"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        container.addView(TextView(this).apply {
            text = "${iconNames.size} exakte Squircle-Icons · Auto-Masking aktiv"
            textSize = 14f
            setTextColor(Color.parseColor("#AEAEB2"))
            setPadding(0, dp(4), 0, dp(16))
        })

        // Info card
        container.addView(createCard().apply {
            addView(TextView(this@MainActivity).apply {
                text = "Für Honor MagicOS (Stock Launcher)"
                textSize = 15f
                setTextColor(Color.parseColor("#0A84FF"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Für den originalen MagicOS-Homescreen installiere bitte die Theme-Datei iOS27_Dark_MagicOS.hnt über den Honor Datei-Manager / Themes. Diese APK dient für Custom Launcher (Nova, Lawnchair, Niagara, Smart Launcher)."
                textSize = 13f
                setTextColor(Color.parseColor("#D4D4D8"))
                setPadding(0, dp(6), 0, dp(12))
            })

            val applyRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            applyRow.addView(createActionButton("Nova", "com.teslacoilsw.launcher"))
            applyRow.addView(createActionButton("Lawnchair", "app.lawnchair"))
            applyRow.addView(createActionButton("Niagara", "bitpit.launcher"))
            addView(applyRow)
        })

        // Grid of icons
        container.addView(TextView(this).apply {
            text = if (isPickerMode) "Icon auswählen" else "Enthaltene Icons (${iconNames.size})"
            textSize = 17f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(20), 0, dp(12))
        })

        val grid = GridLayout(this).apply {
            columnCount = 4
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val iconSize = dp(62)
        for (name in iconNames) {
            val resId = resources.getIdentifier(name, "drawable", packageName)
            if (resId == 0) continue

            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(8), dp(4), dp(8))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setOnClickListener {
                    if (isPickerMode) {
                        val result = Intent().apply {
                            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                Intent.ShortcutIconResource.fromContext(this@MainActivity, resId))
                        }
                        setResult(RESULT_OK, result)
                        finish()
                    } else {
                        Toast.makeText(this@MainActivity, name.removePrefix("ic_"), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            cell.addView(ImageView(this).apply {
                setImageResource(resId)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            })
            grid.addView(cell)
        }

        container.addView(grid)
        root.addView(container)
        setContentView(root)
    }

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(16)
            setPadding(p, p, p, p)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EB161618"))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.parseColor("#2C2C2E"))
            }
        }
    }

    private fun createActionButton(label: String, targetPkg: String): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0A84FF"))
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage(targetPkg)
                if (launch != null) {
                    startActivity(launch)
                } else {
                    Toast.makeText(this@MainActivity, "$label nicht installiert", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadIconNamesFromXml(): List<String> {
        val list = mutableListOf<String>()
        try {
            val parser = resources.getXml(R.xml.drawable)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val drw = parser.getAttributeValue(null, "drawable")
                    if (!drw.isNullOrBlank()) list.add(drw)
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
