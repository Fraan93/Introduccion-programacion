package com.wallcraft4k.app.parallax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

/**
 * A simple live wallpaper that shows the chosen image and shifts it slightly based
 * on the device tilt (accelerometer), producing a parallax / depth effect that
 * "moves when you move the phone". The image path is written by
 * [com.wallcraft4k.app.util.WallpaperActions.setParallaxWallpaper] into prefs.
 */
class ParallaxWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ParallaxEngine()

    inner class ParallaxEngine : Engine(), SensorEventListener {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var bitmap: Bitmap? = null
        private var sensorManager: SensorManager? = null

        private var width = 0
        private var height = 0

        // Current and target normalised offsets [-1, 1].
        private var offsetX = 0f
        private var offsetY = 0f
        private var targetX = 0f
        private var targetY = 0f

        // How far (in px) the image may shift from the centre.
        private val maxShift get() = (width * 0.08f)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            loadBitmap()
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }

        private fun loadBitmap() {
            val path = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PATH, null)
            bitmap = runCatching {
                if (path != null && File(path).exists()) BitmapFactory.decodeFile(path) else null
            }.getOrNull()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            val sm = sensorManager ?: return
            if (visible) {
                loadBitmap()
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                }
                draw()
            } else {
                sm.unregisterListener(this)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            width = w
            height = h
            draw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            sensorManager?.unregisterListener(this)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            sensorManager?.unregisterListener(this)
            bitmap = null
            super.onDestroy()
        }

        override fun onSensorChanged(event: SensorEvent) {
            // values[0] = left/right tilt, values[1] = up/down tilt (range ~ -9.8..9.8)
            targetX = (event.values[0] / 9.8f).coerceIn(-1f, 1f)
            targetY = (event.values[1] / 9.8f).coerceIn(-1f, 1f)
            draw()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        private fun draw() {
            val holder = surfaceHolder
            val bmp = bitmap
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                canvas.drawColor(Color.BLACK)
                if (bmp == null || width == 0 || height == 0) return

                // Ease current offset towards the target for smooth motion.
                offsetX += (targetX - offsetX) * 0.12f
                offsetY += (targetY - offsetY) * 0.12f

                // Scale the bitmap to cover the screen plus a margin for the shift.
                val margin = maxShift
                val scale = maxOf(
                    (width + margin * 2) / bmp.width.toFloat(),
                    (height + margin * 2) / bmp.height.toFloat()
                )
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale

                val left = (width - drawW) / 2f - offsetX * maxShift
                val top = (height - drawH) / 2f - offsetY * maxShift
                val dst = RectF(left, top, left + drawW, top + drawH)
                canvas.drawBitmap(bmp, null, dst, paint)
            } catch (_: Throwable) {
                // ignore transient surface errors
            } finally {
                if (canvas != null) runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }

    companion object {
        const val PREFS = "parallax_wallpaper"
        const val KEY_PATH = "path"
    }
}
