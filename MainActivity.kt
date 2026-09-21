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

    private lateinit var rootLayout: FrameLayout
    private lateinit var webView: WebView

    private var gameFullscreen = false

    // ============================================================
    // WEB CHROME CLIENT
    // ============================================================

    private val chromeClient = object : WebChromeClient() {

        override fun onShowCustomView(
            view: View?,
            callback: WebChromeClient.CustomViewCallback?
        ) {

            /*
             * IMPORTANT:
             *
             * We deliberately DO NOT put the customView over
             * the WebView.
             *
             * Doing that makes the controller disappear.
             *
             * Instead, we keep the WebView itself visible and
             * make it occupy the complete screen.
             */

            gameFullscreen = true

            enterFullscreenMode()

            webView.requestFocus()

            /*
             * Tell Chrome that we handled fullscreen.
             *
             * We don't add the supplied customView because
             * the controller must remain over the game.
             */
            callback?.onCustomViewHidden()
        }

        override fun onHideCustomView() {

            gameFullscreen = false

            exitFullscreenMode()

            webView.requestFocus()
        }
    }

    // ============================================================
    // ANDROID KEYBOARD BRIDGE
    // ============================================================

    inner class KeyBridge {

        @JavascriptInterface
        fun key(
            name: String,
            down: Boolean
        ) {

            /*
             * Special command from controller.js.
             *
             * This is used by the controller's Full button.
             */
            if (name == "__REQUEST_GAME_FULLSCREEN__") {

                runOnUiThread {

                    if (!gameFullscreen) {

                        requestGameFullscreenFromAndroid()

                    } else {

                        exitGameFullscreenFromAndroid()
                    }
                }

                return
            }

            val keyCode =
                toKeyCode(name)
                    ?: return

            runOnUiThread {

                sendRealKey(
                    keyCode,
                    down
                )
            }
        }
    }

    // ============================================================
    // SEND REAL ANDROID KEY
    // ============================================================

    private fun sendRealKey(
        keyCode: Int,
        down: Boolean
    ) {

        /*
         * Keep the WebView as the keyboard target.
         *
         * This is important both in normal and fullscreen mode.
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
                keyCode,
                0,
                0
            )

        /*
         * First send the event through the WebView.
         */
        webView.dispatchKeyEvent(event)

        /*
         * Also inject a JavaScript event for games that listen
         * specifically on window/document.
         */
        injectJavascriptKey(
            keyCode,
            down
        )
    }

    // ============================================================
    // JAVASCRIPT KEY FALLBACK
    // ============================================================

    private fun injectJavascriptKey(
        keyCode: Int,
        down: Boolean
    ) {

        val jsKey =
            when (keyCode) {

                KeyEvent.KEYCODE_A -> "a"
                KeyEvent.KEYCODE_B -> "b"
                KeyEvent.KEYCODE_C -> "c"
                KeyEvent.KEYCODE_D -> "d"
                KeyEvent.KEYCODE_E -> "e"
                KeyEvent.KEYCODE_F -> "f"
                KeyEvent.KEYCODE_G -> "g"
                KeyEvent.KEYCODE_H -> "h"
                KeyEvent.KEYCODE_I -> "i"
                KeyEvent.KEYCODE_J -> "j"
                KeyEvent.KEYCODE_K -> "k"
                KeyEvent.KEYCODE_L -> "l"
                KeyEvent.KEYCODE_M -> "m"
                KeyEvent.KEYCODE_N -> "n"
                KeyEvent.KEYCODE_O -> "o"
                KeyEvent.KEYCODE_P -> "p"
                KeyEvent.KEYCODE_Q -> "q"
                KeyEvent.KEYCODE_R -> "r"
                KeyEvent.KEYCODE_S -> "s"
                KeyEvent.KEYCODE_T -> "t"
                KeyEvent.KEYCODE_U -> "u"
                KeyEvent.KEYCODE_V -> "v"
                KeyEvent.KEYCODE_W -> "w"
                KeyEvent.KEYCODE_X -> "x"
                KeyEvent.KEYCODE_Y -> "y"
                KeyEvent.KEYCODE_Z -> "z"

                KeyEvent.KEYCODE_SPACE -> " "

                KeyEvent.KEYCODE_ENTER -> "Enter"

                KeyEvent.KEYCODE_ESCAPE -> "Escape"

                KeyEvent.KEYCODE_TAB -> "Tab"

                KeyEvent.KEYCODE_DEL -> "Backspace"

                KeyEvent.KEYCODE_SHIFT_LEFT -> "Shift"

                KeyEvent.KEYCODE_CTRL_LEFT -> "Control"

                KeyEvent.KEYCODE_ALT_LEFT -> "Alt"

                KeyEvent.KEYCODE_DPAD_UP -> "ArrowUp"

                KeyEvent.KEYCODE_DPAD_DOWN -> "ArrowDown"

                KeyEvent.KEYCODE_DPAD_LEFT -> "ArrowLeft"

                KeyEvent.KEYCODE_DPAD_RIGHT -> "ArrowRight"

                else -> return
            }

        val code =
            when (keyCode) {

                in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z ->
                    "Key" + jsKey.uppercase()

                KeyEvent.KEYCODE_SPACE ->
                    "Space"

                KeyEvent.KEYCODE_ENTER ->
                    "Enter"

                KeyEvent.KEYCODE_ESCAPE ->
                    "Escape"

                KeyEvent.KEYCODE_TAB ->
                    "Tab"

                KeyEvent.KEYCODE_DEL ->
                    "Backspace"

                KeyEvent.KEYCODE_SHIFT_LEFT ->
                    "ShiftLeft"

                KeyEvent.KEYCODE_CTRL_LEFT ->
                    "ControlLeft"

                KeyEvent.KEYCODE_ALT_LEFT ->
                    "AltLeft"

                KeyEvent.KEYCODE_DPAD_UP ->
                    "ArrowUp"

                KeyEvent.KEYCODE_DPAD_DOWN ->
                    "ArrowDown"

                KeyEvent.KEYCODE_DPAD_LEFT ->
                    "ArrowLeft"

                KeyEvent.KEYCODE_DPAD_RIGHT ->
                    "ArrowRight"

                else -> ""
            }

        val js =
            """
            (function() {
                try {

                    var type =
                        ${if (down) "'keydown'" else "'keyup'"};

                    var ev =
                        new KeyboardEvent(type, {
                            key: ${jsKey.quoteJs()},
                            code: ${code.quoteJs()},
                            bubbles: true,
                            cancelable: true
                        });

                    try {
                        Object.defineProperty(
                            ev,
                            'keyCode',
                            { get: function() { return $keyCode; } }
                        );

                        Object.defineProperty(
                            ev,
                            'which',
                            { get: function() { return $keyCode; } }
                        );
                    } catch(e) {}

                    window.dispatchEvent(ev);
                    document.dispatchEvent(ev);

                    if (document.activeElement) {
                        document.activeElement.dispatchEvent(ev);
                    }

                } catch(e) {}
            })();
            """.trimIndent()

        webView.evaluateJavascript(
            js,
            null
        )
    }

    // ============================================================
    // KEY NAME -> ANDROID KEYCODE
    // ============================================================

    private fun toKeyCode(
        name: String
    ): Int? {

        if (name.length == 1) {

            val c =
                name[0].uppercaseChar()

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
            name.length in 2..3 &&
            name[0] == 'F'
        ) {

            val number =
                name.substring(1)
                    .toIntOrNull()

            if (
                number != null &&
                number in 1..12
            ) {

                return KeyEvent.KEYCODE_F1 +
                    (number - 1)
            }
        }

        return when (name) {

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

    // ============================================================
    // REQUEST FULLSCREEN
    // ============================================================

    private fun requestGameFullscreenFromAndroid() {

        gameFullscreen = true

        enterFullscreenMode()

        webView.requestFocus()
    }

    // ============================================================
    // EXIT FULLSCREEN
    // ============================================================

    private fun exitGameFullscreenFromAndroid() {

        gameFullscreen = false

        exitFullscreenMode()

        webView.requestFocus()
    }

    // ============================================================
    // ENTER FULLSCREEN
    // ============================================================

    @Suppress("DEPRECATION")
    private fun enterFullscreenMode() {

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        /*
         * Make WebView occupy the entire available screen.
         */
        val params =
            webView.layoutParams

        params.width =
            FrameLayout.LayoutParams.MATCH_PARENT

        params.height =
            FrameLayout.LayoutParams.MATCH_PARENT

        webView.layoutParams =
            params

        webView.requestLayout()

        webView.requestFocus()

        /*
         * Tell the page that the viewport changed.
         */
        webView.evaluateJavascript(
            """
            (function() {
                try {
                    window.dispatchEvent(new Event('resize'));
                } catch(e) {}
            })();
            """.trimIndent(),
            null
        )
    }

    // ============================================================
    // EXIT FULLSCREEN
    // ============================================================

    @Suppress("DEPRECATION")
    private fun exitFullscreenMode() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        webView.requestFocus()

        webView.evaluateJavascript(
            """
            (function() {
                try {
                    window.dispatchEvent(new Event('resize'));
                } catch(e) {}
            })();
            """.trimIndent(),
            null
        )
    }

    // ============================================================
    // CREATE
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

        // --------------------------------------------------------
        // ROOT FRAME
        // --------------------------------------------------------

        rootLayout =
            FrameLayout(this)

        rootLayout.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        // --------------------------------------------------------
        // WEBVIEW
        // --------------------------------------------------------

        webView =
            WebView(this)

        webView.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true

        // --------------------------------------------------------
        // ADD WEBVIEW TO ROOT
        // --------------------------------------------------------

        rootLayout.addView(
            webView
        )

        setContentView(
            rootLayout
        )

        // --------------------------------------------------------
        // WEBVIEW SETTINGS
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // ANDROID BRIDGE
        // --------------------------------------------------------

        webView.addJavascriptInterface(
            KeyBridge(),
            "AndroidKeys"
        )

        // --------------------------------------------------------
        // CHROME
        // --------------------------------------------------------

        webView.webChromeClient =
            chromeClient

        // --------------------------------------------------------
        // WEBVIEW CLIENT
        // --------------------------------------------------------

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
                            assets.open(
                                "controller.js"
                            )
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

        // --------------------------------------------------------
        // LOAD GAME
        // --------------------------------------------------------

        webView.loadUrl(
            GAME_URL
        )

        webView.requestFocus()

        enterFullscreenMode()
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

            enterFullscreenMode()

            webView.requestFocus()
        }
    }

    // ============================================================
    // RESUME
    // ============================================================

    override fun onResume() {

        super.onResume()

        webView.onResume()

        enterFullscreenMode()

        webView.requestFocus()
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

        if (gameFullscreen) {

            exitGameFullscreenFromAndroid()

            return
        }

        if (webView.canGoBack()) {

            webView.goBack()

            return
        }

        super.onBackPressed()
    }

    // ============================================================
    // DESTROY
    // ============================================================

    override fun onDestroy() {

        webView.stopLoading()

        webView.destroy()

        super.onDestroy()
    }

    // ============================================================
    // KOTLIN STRING -> JAVASCRIPT STRING
    // ============================================================

    private fun String.quoteJs(): String {

        return "'" +
            this
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r") +
            "'"
    }
}
