package com.propscreen.player

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var lockMode: Boolean = true
    private var currentHtmlContent: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private var isVolumeDownHeld = false
    private val staffMenuRunnable = Runnable {
        if (isVolumeDownHeld) {
            showStaffMenu()
        }
    }

    private var cornerTapCount = 0
    private var lastCornerTapTime = 0L

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on continuously for filming
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Extend into display cutout (punch-hole camera area)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val prefs = getSharedPreferences("propscreen_prefs", Context.MODE_PRIVATE)
        lockMode = prefs.getBoolean("lock_mode", true)

        requestedOrientation = when (prefs.getInt("orientation_mode", 0)) {
            0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            1 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

        setContentView(R.layout.activity_player)
        enterFullScreen()

        webView = findViewById(R.id.webView)
        val hiddenExitZone = findViewById<View>(R.id.hiddenExitZone)

        // Block back gesture if lockMode is ON
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!lockMode) {
                    finish()
                }
            }
        })

        // Hidden 5-tap corner exit fallback
        hiddenExitZone.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastCornerTapTime > 1200) {
                cornerTapCount = 1
            } else {
                cornerTapCount++
            }
            lastCornerTapTime = now
            if (cornerTapCount >= 5) {
                cornerTapCount = 0
                showStaffMenu()
            }
        }

        // Configure WebView for offline prop HTML
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        loadRequestedHtml()
    }

    private fun loadRequestedHtml() {
        val filePath = intent.getStringExtra("file_path")
        val dataUri = intent.data

        try {
            currentHtmlContent = when {
                filePath != null -> File(filePath).readText(Charsets.UTF_8)
                dataUri != null -> contentResolver.openInputStream(dataUri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() } ?: ""
                else -> ""
            }

            if (currentHtmlContent.isBlank()) {
                Toast.makeText(this, "HTML 파일을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Use a stable HTTPS base URL so localStorage & modern JS APIs work cleanly offline
            webView.loadDataWithBaseURL(
                "https://propscreen.local/",
                currentHtmlContent,
                "text/html",
                "UTF-8",
                null
            )
        } catch (e: Exception) {
            Toast.makeText(this, "로드 오류: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterFullScreen()
        }
    }

    private fun enterFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        isVolumeDownHeld = true
                        handler.postDelayed(staffMenuRunnable, 1500L)
                    }
                    return true // Consume so volume slider never pops up on camera
                }
                KeyEvent.ACTION_UP -> {
                    isVolumeDownHeld = false
                    handler.removeCallbacks(staffMenuRunnable)
                    return true
                }
            }
        }
        if (lockMode && event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true // Prevent accidental volume HUD on camera
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showStaffMenu() {
        AlertDialog.Builder(this)
            .setTitle("스태프 메뉴")
            .setItems(arrayOf("화면 새로고침 (NG 원복)", "소품 목록으로 나가기", "계속 촬영")) { _, which ->
                when (which) {
                    0 -> {
                        webView.loadDataWithBaseURL(
                            "https://propscreen.local/",
                            currentHtmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                        enterFullScreen()
                    }
                    1 -> finish()
                    else -> enterFullScreen()
                }
            }
            .setOnDismissListener { enterFullScreen() }
            .show()
    }

    override fun onDestroy() {
        handler.removeCallbacks(staffMenuRunnable)
        super.onDestroy()
    }
}
