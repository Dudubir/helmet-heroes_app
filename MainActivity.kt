package com.gamewrap.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class MainActivity : Activity() {

    companion object {
        const val GAME_URL = "https://www.helmet-heroes.com/"
    }

    // ============================================================
    // VIEWS
    // ============================================================

    private lateinit var rootLayout: FrameLayout
    private lateinit var gameWebView: WebView
    private lateinit var controllerWebView: WebView

    // ============================================================
    // HTML5 FULLSCREEN
    // ============================================================

    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    // ============================================================
    // CHROME CLIENT
    // ============================================================

    private val chromeClient = object : WebChromeClient() {

        override fun onShowCustomView(
            view: View?,
            callback: CustomViewCallback?
        ) {

            if (view == null) {
                callback?.onCustomViewHidden()
                return
            }

            // Already fullscreen.
            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }

            customView = view
            customCallback = callback

            // ----------------------------------------------------
            // Put game's fullscreen view into the root.
            // ----------------------------------------------------

            rootLayout.addView(
                view,
                0,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()

            // ----------------------------------------------------
            // Controller stays ABOVE the fullscreen game.
            // ----------------------------------------------------

            controllerWebView.visibility = View.VISIBLE
            controllerWebView.bringToFront()

            // ----------------------------------------------------
            // Full Android immersive mode.
            // ----------------------------------------------------

            enterFullscreen()

            // ----------------------------------------------------
            // Tell controller that the game is fullscreen.
            // ----------------------------------------------------

            controllerWebView.evaluateJavascript(
                "window.__gameFullscreen = true;",
                null
            )
        }

        override fun onHideCustomView() {

            val oldView = customView

            if (oldView != null) {
                rootLayout.removeView(oldView)
            }

            customView = null

            customCallback?.onCustomViewHidden()
            customCallback = null

            // Controller remains visible.
            controllerWebView.visibility = View.VISIBLE
            controllerWebView.bringToFront()

            enterFullscreen()

            // Return focus to game.
            focusGame()

            controllerWebView.evaluateJavascript(
                "window.__gameFullscreen = false;",
                null
            )
        }
    }

    // ============================================================
    // KEY BRIDGE
    // ============================================================

    inner class KeyBridge {

        @JavascriptInterface
        fun key(name: String, down: Boolean) {

            val code = toKeyCode(name) ?: return

            runOnUiThread {

                // VERY IMPORTANT:
                // Always send keyboard events to the GAME WebView.
                //
                // Do NOT send them to customView.
                //
                // customView is only the fullscreen display.
                // The game page itself lives in gameWebView.
                // ------------------------------------------------

                focusGame()

                val now = SystemClock.uptimeMillis()

                val action =
                    if (down) {
                        KeyEvent.ACTION_DOWN
                    } else {
                        KeyEvent.ACTION_UP
                    }

                val event = KeyEvent(
                    now,
                    now,
                    action,
                    code,
                    0,
                    0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_SOFT_KEYBOARD or
                            KeyEvent.FLAG_KEEP_TOUCH_MODE,
                    android.view.InputDevice.SOURCE_KEYBOARD
                )

                // Send the real Android keyboard event
                // directly into the game WebView.
                gameWebView.dispatchKeyEvent(event)

                // Also send a JavaScript keyboard event as a
                // compatibility fallback.
                sendJavaScriptKey(name, down)
            }
        }
    }

    // ============================================================
    // KEY CODE CONVERSION
    // ============================================================

    private fun toKeyCode(name: String): Int? {

        if (name.length == 1) {

            val c = name[0].uppercaseChar()

            if (c in 'A'..'Z') {
                return KeyEvent.KEYCODE_A + (c - 'A')
            }

            if (c in '0'..'9') {
                return KeyEvent.KEYCODE_0 + (c - '0')
            }
        }

        if (name.length in 2..3 && name[0] == 'F') {

            val number = name.substring(1).toIntOrNull()

            if (number != null && number in 1..12) {
                return KeyEvent.KEYCODE_F1 + number - 1
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
    // JAVASCRIPT KEY FALLBACK
    // ============================================================

    private fun sendJavaScriptKey(
        name: String,
        down: Boolean
    ) {

        val jsName =
            name
                .replace("\\", "\\\\")
                .replace("'", "\\'")

        val js = """
            (function(){
                try {
                    var key = '$jsName';

                    var keyMap = {
                        'Space': {
                            key: ' ',
                            code: 'Space',
                            keyCode: 32
                        },
                        'Enter': {
                            key: 'Enter',
                            code: 'Enter',
                            keyCode: 13
                        },
                        'Escape': {
                            key: 'Escape',
                            code: 'Escape',
                            keyCode: 27
                        },
                        'Tab': {
                            key: 'Tab',
                            code: 'Tab',
                            keyCode: 9
                        },
                        'ArrowUp': {
                            key: 'ArrowUp',
                            code: 'ArrowUp',
                            keyCode: 38
                        },
                        'ArrowDown': {
                            key: 'ArrowDown',
                            code: 'ArrowDown',
                            keyCode: 40
                        },
                        'ArrowLeft': {
                            key: 'ArrowLeft',
                            code: 'ArrowLeft',
                            keyCode: 37
                        },
                        'ArrowRight': {
                            key: 'ArrowRight',
                            code: 'ArrowRight',
                            keyCode: 39
                        }
                    };

                    var info = keyMap[key];

                    if (!info && key.length === 1) {

                        var upper = key.toUpperCase();

                        if (
                            upper >= 'A' &&
                            upper <= 'Z'
                        ) {
                            info = {
                                key: key.toLowerCase(),
                                code: 'Key' + upper,
                                keyCode: upper.charCodeAt(0)
                            };
                        }

                        if (
                            upper >= '0' &&
                            upper <= '9'
                        ) {
                            info = {
                                key: upper,
                                code: 'Digit' + upper,
                                keyCode: upper.charCodeAt(0)
                            };
                        }
                    }

                    if (!info) return;

                    var type = ${if (down) "'keydown'" else "'keyup'"};

                    var ev = new KeyboardEvent(type, {
                        key: info.key,
                        code: info.code,
                        keyCode: info.keyCode,
                        which: info.keyCode,
                        bubbles: true,
                        cancelable: true,
                        view: window
                    });

                    try {
                        Object.defineProperty(
                            ev,
                            'keyCode',
                            {
                                get: function(){
                                    return info.keyCode;
                                }
                            }
                        );

                        Object.defineProperty(
                            ev,
                            'which',
                            {
                                get: function(){
                                    return info.keyCode;
                                }
                            }
                        );
                    } catch(e) {}

                    window.dispatchEvent(ev);
                    document.dispatchEvent(ev);

                    var active = document.activeElement;

                    if (
                        active &&
                        active !== document.body &&
                        active !== document.documentElement
                    ) {
                        active.dispatchEvent(ev);
                    }

                } catch(e) {}
            })();
        """.trimIndent()

        gameWebView.evaluateJavascript(js, null)
    }

    // ============================================================
    // GAME FOCUS
    // ============================================================

    private fun focusGame() {

        gameWebView.isFocusable = true
        gameWebView.isFocusableInTouchMode = true

        gameWebView.requestFocus()

        gameWebView.requestFocusFromTouch()
    }

    // ============================================================
    // IMMERSIVE FULLSCREEN
    // ============================================================

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

    // ============================================================
    // CREATE ACTIVITY
    // ============================================================

    @SuppressLint(
        "SetJavaScriptEnabled",
        "AddJavascriptInterface"
    )
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        // Keep display awake.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // ========================================================
        // ROOT LAYOUT
        // ========================================================

        rootLayout = FrameLayout(this)

        rootLayout.setBackgroundColor(Color.BLACK)

        setContentView(rootLayout)

        // ========================================================
        // GAME WEBVIEW
        // ========================================================

        gameWebView = WebView(this)

        gameWebView.isFocusable = true
        gameWebView.isFocusableInTouchMode = true

        rootLayout.addView(
            gameWebView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // ========================================================
        // GAME WEBVIEW SETTINGS
        // ========================================================

        with(gameWebView.settings) {

            javaScriptEnabled = true

            domStorageEnabled = true

            databaseEnabled = true

            mediaPlaybackRequiresUserGesture = false

            useWideViewPort = true

            loadWithOverviewMode = true

            builtInZoomControls = false

            displayZoomControls = false

            setSupportZoom(false)

            javaScriptCanOpenWindowsAutomatically = true

            allowFileAccess = true

            allowContentAccess = true

            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36"
        }

        // ========================================================
        // KEY BRIDGE
        // ========================================================

        gameWebView.addJavascriptInterface(
            KeyBridge(),
            "AndroidKeys"
        )

        // ========================================================
        // CHROME CLIENT
        // ========================================================

        gameWebView.webChromeClient = chromeClient

        // ========================================================
        // GAME CLIENT
        // ========================================================

        gameWebView.webViewClient =
            object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    super.onPageFinished(view, url)

                    // ------------------------------------------------
                    // Focus the game.
                    // ------------------------------------------------

                    focusGame()
                }
            }

        // ========================================================
        // CONTROLLER WEBVIEW
        // ========================================================

        controllerWebView = WebView(this)

        controllerWebView.setBackgroundColor(
            Color.TRANSPARENT
        )

        controllerWebView.isOpaque = false

        controllerWebView.isFocusable = false
        controllerWebView.isFocusableInTouchMode = false

        controllerWebView.settings.javaScriptEnabled = true
        controllerWebView.settings.domStorageEnabled = true
        controllerWebView.settings.allowFileAccess = true

        // --------------------------------------------------------
        // IMPORTANT:
        // Controller WebView does NOT receive keyboard focus.
        // It only sends keys to gameWebView.
        // --------------------------------------------------------

        controllerWebView.addJavascriptInterface(
            KeyBridge(),
            "AndroidKeys"
        )

        controllerWebView.webChromeClient =
            WebChromeClient()

        controllerWebView.webViewClient =
            object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    super.onPageFinished(view, url)

                    loadController()
                }
            }

        // --------------------------------------------------------
        // Add controller ABOVE game.
        // --------------------------------------------------------

        rootLayout.addView(
            controllerWebView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        controllerWebView.bringToFront()

        // ========================================================
        // LOAD CONTROLLER
        // ========================================================

        loadController()

        // ========================================================
        // LOAD GAME
        // ========================================================

        gameWebView.loadUrl(GAME_URL)

        // ========================================================
        // IMMERSIVE
        // ========================================================

        enterFullscreen()
    }

    // ============================================================
    // LOAD CONTROLLER JAVASCRIPT
    // ============================================================

    private fun loadController() {

        try {

            val controller =
                assets.open("controller.js")
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            controllerWebView.loadDataWithBaseURL(
                "https://controller.local/",
                """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta
                            name="viewport"
                            content="width=device-width,
                            initial-scale=1.0,
                            maximum-scale=1.0,
                            user-scalable=no"
                        >

                        <style>
                            html,
                            body {
                                margin: 0;
                                padding: 0;
                                width: 100%;
                                height: 100%;
                                background: transparent;
                                overflow: hidden;
                            }
                        </style>
                    </head>

                    <body>

                    <script>
                    $controller
                    </script>

                    </body>
                    </html>
                """.trimIndent(),
                "text/html",
                "UTF-8",
                null
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    // ============================================================
    // WINDOW FOCUS
    // ============================================================

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {

            enterFullscreen()

            controllerWebView.bringToFront()

            focusGame()
        }
    }

    // ============================================================
    // RESUME
    // ============================================================

    override fun onResume() {

        super.onResume()

        gameWebView.onResume()
        controllerWebView.onResume()

        enterFullscreen()

        controllerWebView.bringToFront()

        focusGame()
    }

    // ============================================================
    // PAUSE
    // ============================================================

    override fun onPause() {

        controllerWebView.onPause()
        gameWebView.onPause()

        super.onPause()
    }

    // ============================================================
    // BACK BUTTON
    // ============================================================

    @Suppress(
        "DEPRECATION",
        "OVERRIDE_DEPRECATION"
    )
    override fun onBackPressed() {

        // --------------------------------------------------------
        // Exit Helmet Heroes fullscreen first.
        // --------------------------------------------------------

        if (customView != null) {

            chromeClient.onHideCustomView()

            return
        }

        // --------------------------------------------------------
        // Otherwise WebView history.
        // --------------------------------------------------------

        if (gameWebView.canGoBack()) {

            gameWebView.goBack()

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

        gameWebView.stopLoading()
        controllerWebView.stopLoading()

        gameWebView.destroy()
        controllerWebView.destroy()

        super.onDestroy()
    }
}
