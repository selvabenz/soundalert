package com.bridgeconn.soundalert

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sin

class SoundAlertView(context: Context) : View(context) {
    interface Callbacks {
        fun onPowerToggle(turnOn: Boolean)
        fun onSensitivityChanged(value: Float)
        fun onFlashToggle()
    }

    var callbacks: Callbacks? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val handler = Handler(Looper.getMainLooper())
    private var snapshot = ServiceSnapshot()
    private var desiredOn = false
    private var sensitivity = 0.70f
    private var flashEnabled = false
    private var sliderDragging = false
    private var centerDown = false
    private var longPressTriggered = false
    private var downX = 0f
    private var downY = 0f

    private val longPress = Runnable {
        if (centerDown) {
            longPressTriggered = true
            callbacks?.onFlashToggle()
            invalidate()
        }
    }

    init {
        isFocusable = true
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "SoundAlert"
    }

    fun setSnapshot(value: ServiceSnapshot) {
        snapshot = value
        desiredOn = value.running
        sensitivity = value.sensitivity
        flashEnabled = value.flashEnabled
        invalidate()
    }

    fun setDesiredOn(value: Boolean) {
        desiredOn = value
        if (!value) snapshot = snapshot.copy(running = false, alert = AlertKind.NONE, status = "OFF")
        invalidate()
    }

    fun setSensitivity(value: Float) {
        sensitivity = value.coerceIn(0.30f, 0.95f)
        invalidate()
    }

    fun setFlashEnabled(value: Boolean) {
        flashEnabled = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.elapsedRealtime()
        val active = snapshot.running || desiredOn
        val alert = snapshot.alert

        canvas.drawColor(backgroundColor(now, active, alert))

        val w = width.toFloat()
        val h = height.toFloat()
        val unit = min(w, h)
        val cx = w / 2f
        val cy = h * 0.48f
        val radius = unit * 0.19f

        if (alert != AlertKind.NONE) {
            drawAlertIcon(canvas, alert, cx, h * 0.20f, unit * 0.18f)
        }

        drawPower(canvas, cx, cy, radius, active)
        drawSensitivity(canvas, w, h, unit)

        if (flashEnabled) drawFlashIndicator(canvas, w - unit * 0.075f, unit * 0.075f, unit * 0.032f)

        if (active || alert != AlertKind.NONE) postInvalidateOnAnimation()
    }

    private fun backgroundColor(now: Long, active: Boolean, alert: AlertKind): Int {
        val phase = now / 1000.0
        return when (alert) {
            AlertKind.HORN -> mix(Color.rgb(255, 92, 24), Color.rgb(255, 190, 24), wave(phase * 5.0))
            AlertKind.DOOR -> mix(Color.rgb(28, 92, 220), Color.rgb(118, 60, 230), wave(phase * 4.0))
            AlertKind.SIREN -> mix(Color.rgb(220, 0, 32), Color.rgb(255, 245, 245), wave(phase * 7.0))
            AlertKind.NONE -> if (active) {
                mix(Color.rgb(0, 95, 104), Color.rgb(0, 155, 142), 0.25f + wave(phase * 1.3) * 0.45f)
            } else {
                Color.rgb(7, 26, 29)
            }
        }
    }

    private fun drawPower(canvas: Canvas, cx: Float, cy: Float, radius: Float, active: Boolean) {
        paint.style = Paint.Style.FILL
        paint.color = if (active) Color.argb(225, 255, 255, 255) else Color.argb(40, 255, 255, 255)
        canvas.drawCircle(cx, cy, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = radius * 0.105f
        paint.color = if (active) Color.rgb(0, 90, 85) else Color.WHITE
        val arc = RectF(cx - radius * 0.48f, cy - radius * 0.45f, cx + radius * 0.48f, cy + radius * 0.51f)
        canvas.drawArc(arc, -52f, 284f, false, paint)
        canvas.drawLine(cx, cy - radius * 0.67f, cx, cy - radius * 0.03f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textSize = radius * 0.29f
        paint.color = if (active) Color.rgb(0, 90, 85) else Color.WHITE
        canvas.drawText(if (active) "ON" else "OFF", cx, cy + radius * 0.72f, paint)
    }

    private fun drawSensitivity(canvas: Canvas, w: Float, h: Float, unit: Float) {
        val left = w * 0.17f
        val right = w * 0.83f
        val y = h * 0.86f
        val t = ((sensitivity - 0.30f) / 0.65f).coerceIn(0f, 1f)
        val thumbX = left + (right - left) * t

        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit * 0.018f
        paint.color = Color.argb(90, 255, 255, 255)
        canvas.drawLine(left, y, right, y, paint)
        paint.color = Color.WHITE
        canvas.drawLine(left, y, thumbX, y, paint)

        paint.style = Paint.Style.FILL
        canvas.drawCircle(thumbX, y, unit * 0.032f, paint)

        paint.textSize = unit * 0.055f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        canvas.drawText("−", left - unit * 0.075f, y + unit * 0.018f, paint)
        canvas.drawText("+", right + unit * 0.075f, y + unit * 0.018f, paint)
    }

    private fun drawAlertIcon(canvas: Canvas, kind: AlertKind, cx: Float, cy: Float, size: Float) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.08f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        when (kind) {
            AlertKind.HORN -> {
                path.reset()
                path.moveTo(cx - size * 0.48f, cy - size * 0.16f)
                path.lineTo(cx - size * 0.12f, cy - size * 0.16f)
                path.lineTo(cx + size * 0.34f, cy - size * 0.43f)
                path.lineTo(cx + size * 0.34f, cy + size * 0.43f)
                path.lineTo(cx - size * 0.12f, cy + size * 0.16f)
                path.lineTo(cx - size * 0.48f, cy + size * 0.16f)
                path.close()
                canvas.drawPath(path, paint)
                canvas.drawArc(RectF(cx + size * 0.26f, cy - size * 0.28f, cx + size * 0.78f, cy + size * 0.28f), -55f, 110f, false, paint)
            }
            AlertKind.DOOR -> {
                canvas.drawRect(cx - size * 0.34f, cy - size * 0.48f, cx + size * 0.34f, cy + size * 0.48f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx + size * 0.17f, cy, size * 0.045f, paint)
            }
            AlertKind.SIREN -> {
                canvas.drawArc(RectF(cx - size * 0.34f, cy - size * 0.25f, cx + size * 0.34f, cy + size * 0.43f), 180f, 180f, false, paint)
                canvas.drawLine(cx - size * 0.43f, cy + size * 0.42f, cx + size * 0.43f, cy + size * 0.42f, paint)
                canvas.drawLine(cx, cy - size * 0.56f, cx, cy - size * 0.38f, paint)
                canvas.drawLine(cx - size * 0.55f, cy - size * 0.34f, cx - size * 0.42f, cy - size * 0.24f, paint)
                canvas.drawLine(cx + size * 0.55f, cy - size * 0.34f, cx + size * 0.42f, cy - size * 0.24f, paint)
            }
            AlertKind.NONE -> Unit
        }
    }

    private fun drawFlashIndicator(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        path.reset()
        path.moveTo(cx + size * 0.10f, cy - size)
        path.lineTo(cx - size * 0.48f, cy + size * 0.05f)
        path.lineTo(cx - size * 0.05f, cy + size * 0.05f)
        path.lineTo(cx - size * 0.15f, cy + size)
        path.lineTo(cx + size * 0.50f, cy - size * 0.18f)
        path.lineTo(cx + size * 0.08f, cy - size * 0.18f)
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()
        val unit = min(w, h)
        val cx = w / 2f
        val cy = h * 0.48f
        val radius = unit * 0.23f
        val sliderY = h * 0.86f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                val dx = event.x - cx
                val dy = event.y - cy
                centerDown = dx * dx + dy * dy <= radius * radius
                sliderDragging = kotlin.math.abs(event.y - sliderY) <= unit * 0.10f
                longPressTriggered = false
                if (centerDown) handler.postDelayed(longPress, 1000L)
                if (sliderDragging) updateSlider(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (sliderDragging) updateSlider(event.x)
                if (centerDown) {
                    val moved = kotlin.math.abs(event.x - downX) + kotlin.math.abs(event.y - downY)
                    if (moved > unit * 0.06f) {
                        centerDown = false
                        handler.removeCallbacks(longPress)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                if (sliderDragging) {
                    updateSlider(event.x)
                    sliderDragging = false
                    performClick()
                    return true
                }
                if (centerDown && !longPressTriggered) {
                    callbacks?.onPowerToggle(!(snapshot.running || desiredOn))
                    performClick()
                }
                centerDown = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress)
                centerDown = false
                sliderDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateSlider(x: Float) {
        val left = width * 0.17f
        val right = width * 0.83f
        val t = ((x - left) / (right - left)).coerceIn(0f, 1f)
        sensitivity = 0.30f + t * 0.65f
        callbacks?.onSensitivityChanged(sensitivity)
        invalidate()
    }

    private fun wave(value: Double): Float = ((sin(value * Math.PI * 2.0) + 1.0) * 0.5).toFloat()

    private fun mix(a: Int, b: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt()
        )
    }
}
