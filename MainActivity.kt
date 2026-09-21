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
import android.widget.FrameLayout

class MainActivity : Activity() {

    companion object {
        const val GAME_URL = "https://www.helmet-heroes.com/"
    }

    private lateinit var webView: WebView

    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    // ================================================================
    // FULLSCREEN HANDLER
    // ================================================================

    private val chromeClient = object : WebChromeClient() {

        override fun onShowCustomView(
            view: View?,
            callback: WebChromeClient.CustomViewCallback?
        ) {
            if (view == null) {
                callback?.onCustomViewHidden()
                return
            }

            // Already in fullscreen
            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }

            customView = view
            customCallback = callback

            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )

            enterFullscreen()

            val decor = window.decorView as FrameLayout

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            decor.addView(view, params)

            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
        }

        override fun onHideCustomView() {

            val view = customView

            if (view != null) {

                val parent = view.parent

                if (parent is FrameLayout) {
                    parent.removeView(view)
                } else {
                    (window.decorView as FrameLayout).removeView(view)
                }
            }

            customView = null

            customCallback?.onCustomViewHidden()
            customCallback = null

            enterFullscreen()

            webView.requestFocus()
        }
    }

    // ================================================================
    // ANDROID KEYBOARD BRIDGE
    // ================================================================

    inner class KeyBridge {

        @JavascriptInterface
        fun key(name: String, down: Boolean) {

            val keyCode = toKeyCode(name) ?: return

            runOnUiThread {

                val now = SystemClock.uptimeMillis()

                val action = if (down) {
                    KeyEvent.ACTION_DOWN
                } else {
                    KeyEvent.ACTION_UP
                }

                /*
                 * IMPORTANT:
                 *
                 * Always send the keyboard event to the WebView.
                 *
                 * Do NOT send it to customView.
                 *
                 * The fullscreen custom view is only the video/game
                 * fullscreen container. The WebView remains the actual
                 * page that receives keyboard input.
                 */

                webView.requestFocus()

                val event = KeyEvent(
                    now,
                    now,
                    action,
                    keyCode,
                    0,
                    0
                )

                webView.dispatchKeyEvent(event)
            }
        }
    }

    // ================================================================
    // KEY NAME -> ANDROID KEYCODE
    // ================================================================

    private fun toKeyCode(name: String): Int? {

        if (name.length == 1) {

            val c = name[0].uppercaseChar()

            // A-Z
            if (c in 'A'..'Z') {
                return KeyEvent.KEYCODE_A + (c - 'A')
            }

            // 0-9
            if (c in '0'..'9') {
                return KeyEvent.KEYCODE_0 + (c - '0')
            }
        }

        // F1-F12
        if (name.length in 2..3 && name[0] == 'F') {

            val number = name.substring(1).toIntOrNull()

            if (number != null && number in 1..12) {
                return KeyEvent.KEYCODE_F1 + (number - 1)
            }
        }

        return when (name) {

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

    // ================================================================
    // ANDROID IMMERSIVE FULLSCREEN
    // ================================================================

    @Suppress("DEPRECATION")
    private fun enterFullscreen() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    // ================================================================
    // WEBVIEW SETUP
    // ================================================================

    @SuppressLint(
        "SetJavaScriptEnabled",
        "AddJavascriptInterface"
    )
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // ------------------------------------------------------------
        // WEBVIEW
        // ------------------------------------------------------------

        webView = WebView(this)

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true

        setContentView(webView)

        // ------------------------------------------------------------
        // WEBVIEW SETTINGS
        // ------------------------------------------------------------

        with(webView.settings) {

            javaScriptEnabled = true

            domStorageEnabled = true

            databaseEnabled = true

            mediaPlaybackRequiresUserGesture = false

            useWideViewPort = true

            loadWithOverviewMode = true

            builtInZoomControls = false

            displayZoomControls = false

            setSupportZoom(false)

            allowFileAccess = true

            allowContentAccess = true

            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"
        }

        // ------------------------------------------------------------
        // JAVASCRIPT -> ANDROID KEY BRIDGE
        // ------------------------------------------------------------

        webView.addJavascriptInterface(
            KeyBridge(),
            "AndroidKeys"
        )

        // ------------------------------------------------------------
        // CHROME CLIENT
        // ------------------------------------------------------------

        webView.webChromeClient = chromeClient

        // ------------------------------------------------------------
        // WEBVIEW CLIENT
        // ------------------------------------------------------------

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {

                super.onPageFinished(view, url)

                try {

                    val controller =
                        assets.open("controller.js")
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    view?.evaluateJavascript(
                        controller,
                        null
                    )

                } catch (e: Exception) {

                    e.printStackTrace()
                }
            }
        }

        // ------------------------------------------------------------
        // LOAD HELMET HEROES
        // ------------------------------------------------------------

        webView.loadUrl(GAME_URL)

        webView.requestFocus()

        enterFullscreen()
    }

    // ================================================================
    // WINDOW FOCUS
    // ================================================================

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {

            enterFullscreen()

            if (customView != null) {
                customView?.requestFocus()
            } else {
                webView.requestFocus()
            }
        }
    }

    // ================================================================
    // RESUME
    // ================================================================

    override fun onResume() {

        super.onResume()

        webView.onResume()

        enterFullscreen()

        webView.requestFocus()
    }

    // ================================================================
    // PAUSE
    // ================================================================

    override fun onPause() {

        webView.onPause()

        super.onPause()
    }

    // ================================================================
    // BACK BUTTON
    // ================================================================

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {

        // If Helmet Heroes is in fullscreen,
        // exit the game's fullscreen first.
        if (customView != null) {

            chromeClient.onHideCustomView()

            return
        }

        // Otherwise go back in WebView history.
        if (webView.canGoBack()) {

            webView.goBack()

            return
        }

        super.onBackPressed()
    }

    // ================================================================
    // DESTROY
    // ================================================================

    override fun onDestroy() {

        if (customView != null) {
            chromeClient.onHideCustomView()
        }

        webView.stopLoading()

        webView.destroy()

        super.onDestroy()
    }
}
