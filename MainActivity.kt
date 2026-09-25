package com.gamewrap.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    companion object {
        const val GAME_URL = "https://www.helmet-heroes.com/"
        private const val TAG = "VPAD"
    }

    private lateinit var webView: WebView

    /*
     * Prevent Android's native fullscreen UI from taking over.
     * The controller.js fullscreen system handles the game fullscreen
     * inside the webpage.
     */
    private val chromeClient = object : WebChromeClient() {
        override fun onShowCustomView(
            view: View?,
            callback: CustomViewCallback?
        ) {
            callback?.onCustomViewHidden()
        }

        override fun onHideCustomView() {}
    }

    /*
     * JavaScript -> Android bridge.
     *
     * The important part is that we now create a native Android KeyEvent
     * and send it through the WebView rather than creating a JavaScript
     * KeyboardEvent and trying to inject it into the cross-origin iframe.
     */
    inner class KeyBridge {

        @JavascriptInterface
        fun key(name: String, down: Boolean) {

            val code = toKeyCode(name)

            Log.d(
                TAG,
                "key() called: name=$name down=$down resolvedCode=$code"
            )

            if (code == null) {
                reportToJs(
                    "ERROR: unresolvable key name=$name"
                )
                return
            }

            runOnUiThread {

                /*
                 * Make sure the WebView has native focus.
                 */
                webView.isFocusable = true
                webView.isFocusableInTouchMode = true

                val focusResult =
                    webView.requestFocus(View.FOCUS_DOWN)

                Log.d(
                    TAG,
                    "requestFocus result=$focusResult"
                )

                val now = SystemClock.uptimeMillis()

                val action =
                    if (down) {
                        KeyEvent.ACTION_DOWN
                    } else {
                        KeyEvent.ACTION_UP
                    }

                /*
                 * Native Android keyboard event.
                 *
                 * This is intentionally NOT a JavaScript KeyboardEvent.
                 */
                val event = KeyEvent(
                    now,
                    now,
                    action,
                    code,
                    0,
                    0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_SOFT_KEYBOARD
                )

                val handled =
                    webView.dispatchKeyEvent(event)

                Log.d(
                    TAG,
                    "NATIVE dispatch: " +
                            "name=$name " +
                            "down=$down " +
                            "code=$code " +
                            "action=$action " +
                            "handled=$handled"
                )

                reportToJs(
                    "NATIVE dispatch " +
                            "name=$name " +
                            "down=$down " +
                            "code=$code " +
                            "handled=$handled"
                )
            }
        }

        /*
         * Ask Android to give the WebView native focus.
         */
        @JavascriptInterface
        fun ensureFocus() {

            runOnUiThread {

                webView.isFocusable = true
                webView.isFocusableInTouchMode = true

                val focused =
                    webView.requestFocus(View.FOCUS_DOWN)

                Log.d(
                    TAG,
                    "ensureFocus() requestFocus=$focused"
                )

                reportToJs(
                    "ensureFocus requestFocus=$focused"
                )
            }
        }

        /*
         * Send native debugging information back to controller.js.
         */
        private fun reportToJs(msg: String) {

            val escaped = msg
                .replace("\\", "\\\\")
                .replace("'", "\\'")

            val js =
                "if(window.__vpadNativeLog){" +
                        "window.__vpadNativeLog('$escaped');" +
                        "}"

            runOnUiThread {
                webView.evaluateJavascript(
                    js,
                    null
                )
            }
        }
    }

    /*
     * Convert our JavaScript key names to Android KEYCODE values.
     */
    private fun toKeyCode(n: String): Int? {

        if (n.length == 1) {

            val c = n[0].uppercaseChar()

            if (c in 'A'..'Z') {
                return KeyEvent.KEYCODE_A + (c - 'A')
            }

            if (c in '0'..'9') {
                return KeyEvent.KEYCODE_0 + (c - '0')
            }
        }

        if (n.length in 2..3 && n[0] == 'F') {

            val num = n
                .substring(1)
                .toIntOrNull()

            if (num != null && num in 1..12) {
                return KeyEvent.KEYCODE_F1 + (num - 1)
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

            else ->
                null
        }
    }

    @SuppressLint(
        "SetJavaScriptEnabled",
        "AddJavascriptInterface"
    )
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        /*
         * Enable Chrome remote debugging in debug builds.
         */
        if (
            0 != (
                applicationInfo.flags and
                        ApplicationInfo.FLAG_DEBUGGABLE
                )
        ) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        /*
         * Create WebView.
         */
        webView = WebView(this)

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true

        setContentView(webView)

        /*
         * WebView settings.
         */
        with(webView.settings) {

            javaScriptEnabled = true

            domStorageEnabled = true

            mediaPlaybackRequiresUserGesture = false

            useWideViewPort = true

            loadWithOverviewMode = true

            /*
             * Desktop Chrome user agent.
             */
            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36"
        }

        /*
         * Expose:
         *
         * window.AndroidKeys
         *
         * to controller.js.
         */
        webView.addJavascriptInterface(
            KeyBridge(),
            "AndroidKeys"
        )

        webView.webChromeClient = chromeClient

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {

                super.onPageFinished(view, url)

                try {

                    val js = assets
                        .open("controller.js")
                        .bufferedReader()
                        .use { it.readText() }

                    view?.evaluateJavascript(
                        js,
                        null
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Failed to load controller.js",
                        e
                    )
                }
            }
        }

        /*
         * Load Helmet Heroes directly into the WebView.
         */
        webView.loadUrl(GAME_URL)

        /*
         * Give the WebView initial focus.
         */
        webView.requestFocus(View.FOCUS_DOWN)
    }

    @Suppress("DEPRECATION")
    private fun goImmersive() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            goImmersive()
        }
    }

    override fun onResume() {

        super.onResume()

        webView.onResume()
    }

    override fun onPause() {

        webView.onPause()

        super.onPause()
    }

    @Suppress(
        "DEPRECATION",
        "OVERRIDE_DEPRECATION"
    )
    override fun onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }
}
