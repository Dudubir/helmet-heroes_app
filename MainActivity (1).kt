package com.gamewrap.app

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    companion object {
        // ===== PALITAN MO ITO NG LINK NG LARO MO =====
        const val GAME_URL = "https://www.helmet-heroes.com/"
    }

    private lateinit var webView: WebView

    // Tinatanggihan ang native (Android-level) fullscreen para hindi kailanman
    // matakpan ng ibang layer ang HTML controller overlay. Ang mismong laro
    // ay mananatiling gagamit ng sariling CSS-based fullscreen polyfill na
    // nasa controller.js, na parte ng parehong web page kaya laging nasa
    // ibabaw ang overlay.
    private val chromeClient = object : WebChromeClient() {
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            callback?.onCustomViewHidden()
        }
        override fun onHideCustomView() {}
    }

    // Tulay: ang controller.js ay nagpapadala ng TOTOONG Android key events
    inner class KeyBridge {
        @JavascriptInterface
        fun key(name: String, down: Boolean) {
            val code = toKeyCode(name) ?: return
            runOnUiThread {
                val now = SystemClock.uptimeMillis()
                val action = if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
                webView.dispatchKeyEvent(KeyEvent(now, now, action, code, 0))
            }
        }

        // Tinatawag ng JS bago magpadala ng key, para siguraduhing may
        // input focus muna ang WebView mismo.
        @JavascriptInterface
        fun ensureFocus() {
            runOnUiThread {
                webView.requestFocus()
            }
        }
    }

    private fun toKeyCode(n: String): Int? {
        if (n.length == 1) {
            val c = n[0].uppercaseChar()
            if (c in 'A'..'Z') return KeyEvent.KEYCODE_A + (c - 'A')
            if (c in '0'..'9') return KeyEvent.KEYCODE_0 + (c - '0')
        }
        if (n.length in 2..3 && n[0] == 'F') {
            val num = n.substring(1).toIntOrNull()
            if (num != null && num in 1..12) return KeyEvent.KEYCODE_F1 + (num - 1)
        }
        return when (n) {
            "Space" -> KeyEvent.KEYCODE_SPACE
            "Enter" -> KeyEvent.KEYCODE_ENTER
            "Escape" -> KeyEvent.KEYCODE_ESCAPE
            "Tab" -> KeyEvent.KEYCODE_TAB
            "Backspace" -> KeyEvent.KEYCODE_DEL
            "Shift" -> KeyEvent.KEYCODE_SHIFT_LEFT
            "Control" -> KeyEvent.KEYCODE_CTRL_LEFT
            "Alt" -> KeyEvent.KEYCODE_ALT_LEFT
            "ArrowUp" -> KeyEvent.KEYCODE_DPAD_UP
            "ArrowDown" -> KeyEvent.KEYCODE_DPAD_DOWN
            "ArrowLeft" -> KeyEvent.KEYCODE_DPAD_LEFT
            "ArrowRight" -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> null
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webView = WebView(this)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        setContentView(webView)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            // Desktop UA para hindi i-redirect ng site sa app store
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }

        webView.addJavascriptInterface(KeyBridge(), "AndroidKeys")
        webView.webChromeClient = chromeClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val js = assets.open("controller.js").bufferedReader().use { it.readText() }
                view?.evaluateJavascript(js, null)
            }
        }

        webView.loadUrl(GAME_URL)
        webView.requestFocus()
    }

    @Suppress("DEPRECATION")
    private fun goImmersive() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goImmersive()
    }

    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onPause() { webView.onPause(); super.onPause() }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
