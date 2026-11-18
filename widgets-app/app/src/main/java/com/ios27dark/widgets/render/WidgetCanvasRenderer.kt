package com.ios27dark.widgets.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object WidgetCanvasRenderer {

    private const val COLOR_GREEN = 0xFF30D158.toInt()
    private const val COLOR_ORANGE = 0xFFFF9F0A.toInt()
    private const val COLOR_RED = 0xFFFF3B30.toInt()
    private const val COLOR_BLUE = 0xFF0A84FF.toInt()
    private const val COLOR_TRACK = 0xFF2C2C2E.toInt()

    fun renderBatteryRing(
        level: Int,
        isCharging: Boolean,
        kind: String,
        sizePx: Int = 180,
        showPercentInside: Boolean = true
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val stroke = sizePx * 0.105f
        val radius = sizePx * 0.41f
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = COLOR_TRACK
        }
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)

        val ringColor = when {
            level < 0 -> COLOR_TRACK
            level < 20 -> COLOR_RED
            level < 40 -> COLOR_ORANGE
            else -> COLOR_GREEN
        }

        if (level >= 0) {
            val sweep = (level.coerceIn(0, 100) / 100f) * 360f
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                color = ringColor
            }
            canvas.drawArc(oval, -90f, sweep, false, arcPaint)
        }

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isCharging) COLOR_GREEN else Color.WHITE
            style = Paint.Style.FILL
        }

        if (showPercentInside) {
            val pctText = if (level >= 0) "$level%" else "–"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = sizePx * 0.22f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            // Small glyph above percent
            drawMiniDeviceGlyph(canvas, cx, cy - sizePx * 0.14f, sizePx * 0.13f, kind, isCharging, iconPaint)
            val fm = textPaint.fontMetrics
            val textY = cy + sizePx * 0.16f - (fm.ascent + fm.descent) / 2f
            canvas.drawText(pctText, cx, textY, textPaint)
        } else {
            drawMiniDeviceGlyph(canvas, cx, cy, sizePx * 0.20f, kind, isCharging, iconPaint)
        }

        return bmp
    }

    private fun drawMiniDeviceGlyph(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        kind: String,
        isCharging: Boolean,
        paint: Paint
    ) {
        if (isCharging) {
            val bolt = Path().apply {
                moveTo(cx + r * 0.15f, cy - r * 0.95f)
                lineTo(cx - r * 0.55f, cy + r * 0.1f)
                lineTo(cx + r * 0.02f, cy + r * 0.1f)
                lineTo(cx - r * 0.18f, cy + r * 0.95f)
                lineTo(cx + r * 0.58f, cy - r * 0.12f)
                lineTo(cx - r * 0.02f, cy - r * 0.12f)
                close()
            }
            canvas.drawPath(bolt, paint)
            return
        }

        when (kind) {
            "audio" -> {
                val strokeP = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = r * 0.22f
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawArc(RectF(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f), 180f, 180f, false, strokeP)
                canvas.drawRoundRect(RectF(cx - r * 0.85f, cy - r * 0.05f, cx - r * 0.45f, cy + r * 0.65f), r * 0.2f, r * 0.2f, paint)
                canvas.drawRoundRect(RectF(cx + r * 0.45f, cy - r * 0.05f, cx + r * 0.85f, cy + r * 0.65f), r * 0.2f, r * 0.2f, paint)
            }
            "watch" -> {
                canvas.drawRoundRect(RectF(cx - r * 0.3f, cy - r * 0.85f, cx + r * 0.3f, cy + r * 0.85f), r * 0.15f, r * 0.15f, paint)
                canvas.drawRoundRect(RectF(cx - r * 0.55f, cy - r * 0.5f, cx + r * 0.55f, cy + r * 0.5f), r * 0.22f, r * 0.22f, paint)
            }
            else -> { // phone
                val strokeP = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = r * 0.18f
                }
                canvas.drawRoundRect(RectF(cx - r * 0.45f, cy - r * 0.8f, cx + r * 0.45f, cy + r * 0.8f), r * 0.2f, r * 0.2f, strokeP)
            }
        }
    }

    fun renderSparkline(
        points: List<Float>,
        isPositive: Boolean,
        widthPx: Int = 260,
        heightPx: Int = 84
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (points.size < 2) return bmp

        val padY = heightPx * 0.12f
        val padX = widthPx * 0.03f
        val minVal = points.minOrNull() ?: 0f
        val maxVal = points.maxOrNull() ?: 1f
        val range = max(0.0001f, maxVal - minVal)

        val lineColor = if (isPositive) COLOR_GREEN else COLOR_RED
        val fillTopColor = if (isPositive) 0x5530D158 else 0x55FF3B30
        val fillBottomColor = 0x00161618

        // Dotted reference baseline at start value
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x448E8E93
            strokeWidth = 1.6f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        }
        val firstY = padY + (1f - (points.first() - minVal) / range) * (heightPx - 2 * padY)
        canvas.drawLine(padX, firstY, widthPx - padX, firstY, basePaint)

        val linePath = Path()
        val fillPath = Path()
        val usableW = widthPx - 2 * padX
        val usableH = heightPx - 2 * padY

        points.forEachIndexed { idx, v ->
            val x = padX + (idx.toFloat() / (points.size - 1)) * usableW
            val y = padY + (1f - (v - minVal) / range) * usableH
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, heightPx.toFloat())
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (idx == points.lastIndex) {
                fillPath.lineTo(x, heightPx.toFloat())
                fillPath.close()
            }
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(0f, padY, 0f, heightPx.toFloat(), fillTopColor, fillBottomColor, Shader.TileMode.CLAMP)
        }
        canvas.drawPath(fillPath, fillPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(2.5f, heightPx * 0.045f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = lineColor
        }
        canvas.drawPath(linePath, linePaint)

        return bmp
    }

    fun renderWeatherIcon(wmoCode: Int, isDay: Boolean, sizePx: Int = 96): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = sizePx / 100f

        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(25f * s, 20f * s, 75f * s, 70f * s, 0xFFFFE066.toInt(), COLOR_ORANGE, Shader.TileMode.CLAMP)
        }
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE5E7EB.toInt()
        }
        val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF4F4F5.toInt()
        }

        when {
            wmoCode == 0 || wmoCode == 1 -> {
                if (isDay) {
                    canvas.drawCircle(50f * s, 50f * s, 23f * s, sunPaint)
                    val rayP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFFFFD60A.toInt()
                        strokeWidth = 4.2f * s
                        strokeCap = Paint.Cap.ROUND
                    }
                    for (i in 0 until 8) {
                        val a = i * Math.PI / 4.0
                        canvas.drawLine(
                            (50 + 30 * cos(a)).toFloat() * s,
                            (50 + 30 * sin(a)).toFloat() * s,
                            (50 + 38 * cos(a)).toFloat() * s,
                            (50 + 38 * sin(a)).toFloat() * s,
                            rayP
                        )
                    }
                } else {
                    canvas.drawCircle(48f * s, 50f * s, 24f * s, moonPaint)
                    val cut = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    }
                    canvas.drawCircle(60f * s, 40f * s, 20f * s, cut)
                }
            }
            wmoCode == 2 -> {
                if (isDay) canvas.drawCircle(37f * s, 38f * s, 17f * s, sunPaint)
                else canvas.drawCircle(37f * s, 38f * s, 15f * s, moonPaint)
                drawCloud(canvas, 54f * s, 60f * s, 1.0f * s, cloudPaint)
            }
            wmoCode == 3 || wmoCode == 45 || wmoCode == 48 -> {
                drawCloud(canvas, 50f * s, 54f * s, 1.15f * s, cloudPaint)
            }
            wmoCode in 51..67 || wmoCode in 80..82 -> {
                drawCloud(canvas, 50f * s, 44f * s, 1.05f * s, cloudPaint)
                val rainP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF38BDF8.toInt()
                    strokeWidth = 4.5f * s
                    strokeCap = Paint.Cap.ROUND
                }
                for (dx in listOf(-14f, 0f, 14f)) {
                    canvas.drawLine((50f + dx) * s, 66f * s, (46f + dx) * s, 82f * s, rainP)
                }
            }
            wmoCode in 71..77 || wmoCode in 85..86 -> {
                drawCloud(canvas, 50f * s, 44f * s, 1.05f * s, cloudPaint)
                val snowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFBAE6FD.toInt() }
                for (dx in listOf(-15f, 0f, 15f)) {
                    canvas.drawCircle((50f + dx) * s, 74f * s, 4.2f * s, snowP)
                }
            }
            else -> { // Thunderstorm
                drawCloud(canvas, 50f * s, 42f * s, 1.05f * s, cloudPaint)
                val boltP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD60A.toInt() }
                val bolt = Path().apply {
                    moveTo(54f * s, 56f * s)
                    lineTo(42f * s, 72f * s)
                    lineTo(50f * s, 72f * s)
                    lineTo(46f * s, 88f * s)
                    lineTo(61f * s, 68f * s)
                    lineTo(52f * s, 68f * s)
                    close()
                }
                canvas.drawPath(bolt, boltP)
            }
        }
        return bmp
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, scale: Float, paint: Paint) {
        canvas.drawCircle(cx - 14f * scale, cy + 2f * scale, 12f * scale, paint)
        canvas.drawCircle(cx, cy - 6f * scale, 15f * scale, paint)
        canvas.drawCircle(cx + 16f * scale, cy + 3f * scale, 11f * scale, paint)
        canvas.drawRoundRect(
            RectF(cx - 26f * scale, cy + 2f * scale, cx + 27f * scale, cy + 15f * scale),
            7f * scale,
            7f * scale,
            paint
        )
    }

    fun renderRoundedBitmap(source: Bitmap, widthPx: Int, heightPx: Int, radiusPx: Float): Bitmap {
        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val clipPath = Path().apply {
            addRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), radiusPx, radiusPx, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        // Center-crop source
        val srcRatio = source.width.toFloat() / source.height.toFloat()
        val dstRatio = widthPx.toFloat() / heightPx.toFloat()
        val srcRect = if (srcRatio > dstRatio) {
            val cropW = (source.height * dstRatio).toInt()
            val left = (source.width - cropW) / 2
            Rect(left, 0, left + cropW, source.height)
        } else {
            val cropH = (source.width / dstRatio).toInt()
            val top = (source.height - cropH) / 2
            Rect(0, top, source.width, top + cropH)
        }
        canvas.drawBitmap(source, srcRect, Rect(0, 0, widthPx, heightPx), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = COLOR_TRACK
        }
        canvas.drawRoundRect(RectF(1f, 1f, widthPx - 1f, heightPx - 1f), radiusPx, radiusPx, borderPaint)
        return out
    }

    fun renderDefaultPhotoArt(widthPx: Int = 480, heightPx: Int = 480, radiusPx: Float = 48f): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        val path = Path().apply { addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)

        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), 0xFF0A0E1A.toInt(), 0xFF141122.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(rect, bg)

        val orb1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(widthPx * 0.25f, heightPx * 0.3f, widthPx * 0.6f, 0x660A84FF, 0x000A84FF, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(rect, orb1)

        val orb2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(widthPx * 0.75f, heightPx * 0.68f, widthPx * 0.55f, 0x55AF52DE, 0x00AF52DE, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(rect, orb2)

        val orb3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(widthPx * 0.45f, heightPx * 0.85f, widthPx * 0.45f, 0x4430D158, 0x0030D158, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(rect, orb3)
        canvas.restore()

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = COLOR_TRACK
        }
        canvas.drawRoundRect(RectF(1.25f, 1.25f, widthPx - 1.25f, heightPx - 1.25f), radiusPx, radiusPx, border)
        return bmp
    }
}
