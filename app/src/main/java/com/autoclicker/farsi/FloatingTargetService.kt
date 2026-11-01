package com.autoclicker.farsi

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class FloatingTargetService : Service() {

    private var windowManager: WindowManager? = null
    private var targetView: View? = null
    private var menuView: View? = null
    private var targetParams: WindowManager.LayoutParams? = null
    private var menuParams: WindowManager.LayoutParams? = null
    private var clockHandler: android.os.Handler? = null

    companion object {
        var currentX: Int = 540
        var currentY: Int = 1100
        var intervalMs: Long = 100L
        var clickCount: Int = 40
        var scheduledEpochMs: Long = 0L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        setupFloatingTarget()
        setupFloatingMenu()
    }

    private fun setupFloatingTarget() {
        targetView = LayoutInflater.from(this).inflate(R.layout.layout_floating_target, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        targetParams = WindowManager.LayoutParams(
            140, 140,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentX - 70
            y = currentY - 70
        }

        // جابجایی بسیار روان نشانه‌گیر با کشیدن انگشت (Drag & Drop بدون نیاز به تایپ عدد)
        targetView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = targetParams!!.x
                        initialY = targetParams!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        targetParams!!.x = initialX + (event.rawX - initialTouchX).toInt()
                        targetParams!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        
                        // محاسبه دقیق مرکز نشانه‌گیر بر حسب پیکسل صفحه نمایش
                        currentX = targetParams!!.x + 70
                        currentY = targetParams!!.y + 70

                        windowManager?.updateViewLayout(targetView, targetParams)
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(targetView, targetParams)
    }

    private fun setupFloatingMenu() {
        menuView = LayoutInflater.from(this).inflate(R.layout.layout_floating_menu, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 300
        }

        val tvFloatingClock = menuView?.findViewById<TextView>(R.id.tvFloatingClock)
        val btnPlay = menuView?.findViewById<Button>(R.id.btnFloatingPlay)
        val btnStop = menuView?.findViewById<Button>(R.id.btnFloatingStop)
        val btnClose = menuView?.findViewById<Button>(R.id.btnFloatingClose)

        // ساعت زنده ۲۴ ساعته شامل میلی‌ثانیه روی نوار شناور
        val clockFormat = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
        clockHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val clockRunnable = object : Runnable {
            override fun run() {
                tvFloatingClock?.text = clockFormat.format(java.util.Date())
                clockHandler?.postDelayed(this, 30L)
            }
        }
        clockHandler?.post(clockRunnable)

        // دکمه شروع (پلی)
        btnPlay?.setOnClickListener {
            val startIntent = Intent(this, AutoClickService::class.java).apply {
                action = AutoClickService.ACTION_START_CLICKING
                putExtra(AutoClickService.EXTRA_X, currentX)
                putExtra(AutoClickService.EXTRA_Y, currentY)
                putExtra(AutoClickService.EXTRA_INTERVAL, intervalMs)
                putExtra(AutoClickService.EXTRA_COUNT, clickCount)
                putExtra(AutoClickService.EXTRA_SCHEDULE_MS, scheduledEpochMs)
            }
            startService(startIntent)
        }

        // دکمه توقف (استاپ)
        btnStop?.setOnClickListener {
            val stopIntent = Intent(this, AutoClickService::class.java).apply {
                action = AutoClickService.ACTION_STOP_CLICKING
            }
            startService(stopIntent)
        }

        // دکمه بستن کامل پنجره شناور
        btnClose?.setOnClickListener {
            val stopIntent = Intent(this, AutoClickService::class.java).apply {
                action = AutoClickService.ACTION_STOP_CLICKING
            }
            startService(stopIntent)
            stopSelf()
        }

        // جابجایی نوار منوی شناور در کنار صفحه
        menuView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = menuParams!!.x
                        initialY = menuParams!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        menuParams!!.x = initialX - (event.rawX - initialTouchX).toInt()
                        menuParams!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(menuView, menuParams)
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(menuView, menuParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler?.removeCallbacksAndMessages(null)
        if (targetView != null) windowManager?.removeView(targetView)
        if (menuView != null) windowManager?.removeView(menuView)
    }
}