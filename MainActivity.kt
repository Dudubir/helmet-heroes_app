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

    // ============================================================
    // HTML5 FULLSCREEN
    // ============================================================

    private val chromeClient = object : WebChromeClient() {

        override fun onShowCustomView(
            view: View?,
            callback: WebChromeClient.CustomViewCallback?
        ) {

            if (view == null) {
                callback?.onCustomViewHidden()
                return
            }

            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }

            customView = view
            customCallback = callback

            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )

            enterImmersiveFullscreen()

            val decor =
                window.decorView as FrameLayout

            decor.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

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
                }
            }

            customView = null

            customCallback?.onCustomViewHidden()

            customCallback = null

            enterImmersiveFullscreen()

            webView.requestFocus()
        }
    }

    // ============================================================
    // VIRTUAL KEYBOARD BRIDGE
    // ============================================================

    inner class KeyBridge {

        @JavascriptInterface
        fun key(
            name: String,
            down: Boolean
        ) {

            val code =
                toKeyCode(name)
                    ?: return

            runOnUiThread {

                /*
                 * IMPORTANT:
                 *
                 * Always send the key to the WebView.
                 *
                 * Do NOT use:
                 *
                 * customView ?: webView
                 *
                 * because customView is not the page's JavaScript
                 * keyboard target.
                 */

                webView.requestFocus()

                val now =
                    SystemClock.uptimeMillis()

                val action =
                    if (down) {
                        KeyEvent.ACTION_DOWN
                    } else {
                        KeyEvent.ACTION_UP
                    }

                val event =
                    KeyEvent(
                        now,
                        now,
                        action,
                        code,
                        0
                    )

                /*
                 * Send the real Android keyboard event
                 * directly to the WebView.
                 */
                webView.dispatchKeyEvent(event)
            }
        }
    }

    // ============================================================
    // KEY MAPPING
    // ============================================================

    private fun toKeyCode(
        n: String
    ): Int? {

        if (n.length == 1) {

            val c =
                n[0].uppercaseChar()

            if (c in 'A'..'Z') {

                return KeyEvent.KEYCODE_A +
                        (c - 'A')
            }

            if (c in '0'..'9') {

                return KeyEvent.KEYCODE_0 +
                        (c - '0')
            }
        }

        if (
            n.length in 2..3 &&
            n[0] == 'F'
        ) {

            val num =
                n.substring(1)
                    .toIntOrNull()

            if (
                num != null &&
                num in 1..12
            ) {

                return KeyEvent.KEYCODE_F1 +
                        (num - 1)
            }
        }

        return when (n) {

            "Space" ->
                KeyEvent.KEYCODE_SPACE

            "Enter" ->
                KeyEvent.KEYCODE_ENTER

            "Escape" ->
                KeyEvent.KEYCODE_ESCAPE

            "Tab" ->
                KeyEvent.KEYCODE_TAB

            "Backspace" ->
                KeyEvent.KEYCODE_DEL

            "Shift" ->
                KeyEvent.KEYCODE_SHIFT_LEFT

            "Control" ->
                KeyEvent.KEYCODE_CTRL_LEFT

            "Alt" ->
                KeyEvent.KEYCODE_ALT_LEFT

            "ArrowUp" ->
                KeyEvent.KEYCODE_DPAD_UP

            "ArrowDown" ->
                KeyEvent.KEYCODE_DPAD_DOWN

            "ArrowLeft" ->
                KeyEvent.KEYCODE_DPAD_LEFT

            "ArrowRight" ->
                KeyEvent.KEYCODE_DPAD_RIGHT

            else -> null
        }
    }

    // ============================================================
    // IMMERSIVE FULLSCREEN
    // ============================================================

    @Suppress("DEPRECATION")
    private fun enterImmersiveFullscreen() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    // ============================================================
    // ACTIVITY
    // ============================================================

    @SuppressLint(
        "SetJavaScriptEnabled",
        "AddJavascriptInterface"
    )
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // ========================================================
        // WEBVIEW
        // ========================================================

        webView = WebView(this)

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true

        setContentView(webView)

        // ========================================================
        // WEBVIEW SETTINGS
        // ========================================================

        with(webView.settings) {

            javaScriptEnabled = true

            domStorageEnabled = true

            mediaPlaybackRequiresUserGesture = false

            useWideViewPort = true

            loadWithOverviewMode = true

            builtInZoomControls = false

            displayZoomControls = false

            setSupportZoom(false)

            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"
        }

        // ========================================================
        // CONTROLLER BRIDGE
        // ========================================================

        webView.addJavascriptInterface(
            KeyBridge(),
            "AndroidKeys"
        )

        // ========================================================
        // FULLSCREEN
        // ========================================================

        webView.webChromeClient =
            chromeClient

        // ========================================================
        // WEBVIEW CLIENT
        // ========================================================

        webView.webViewClient =
            object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    super.onPageFinished(
                        view,
                        url
                    )

                    try {

                        val controller =
                            assets
                                .open("controller.js")
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

        // ========================================================
        // LOAD GAME
        // ========================================================

        webView.loadUrl(GAME_URL)

        webView.requestFocus()

        enterImmersiveFullscreen()
    }

    // ============================================================
    // WINDOW FOCUS
    // ============================================================

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(
            hasFocus
        )

        if (hasFocus) {

            enterImmersiveFullscreen()

            webView.requestFocus()
        }
    }

    // ============================================================
    // RESUME
    // ============================================================

    override fun onResume() {

        super.onResume()

        webView.onResume()

        webView.requestFocus()

        enterImmersiveFullscreen()
    }

    // ============================================================
    // PAUSE
    // ============================================================

    override fun onPause() {

        webView.onPause()

        super.onPause()
    }

    // ============================================================
    // BACK
    // ============================================================

    @Suppress(
        "DEPRECATION",
        "OVERRIDE_DEPRECATION"
    )
    override fun onBackPressed() {

        if (customView != null) {

            chromeClient.onHideCustomView()

            return
        }

        if (webView.canGoBack()) {

            webView.goBack()

            return
        }

        super.onBackPressed()
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    override fun onDestroy() {

        if (customView != null) {
            chromeClient.onHideCustomView()
        }

        webView.stopLoading()

        webView.destroy()

        super.onDestroy()
    }
}
