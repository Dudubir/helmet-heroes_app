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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class MainActivity : Activity() {

    companion object {
        const val GAME_URL = "https://www.helmet-heroes.com/"
    }

    private lateinit var rootLayout: FrameLayout
    private lateinit var gameWebView: WebView
    private lateinit var controllerWebView: WebView

    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

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

            // Remove previous fullscreen view if one somehow remains.
            fullscreenView?.let {
                rootLayout.removeView(it)
            }

            fullscreenView = view
            fullscreenCallback = callback

            // --------------------------------------------------------
            // Put the actual Helmet Heroes fullscreen view into our
            // root layout.
            // --------------------------------------------------------

            val fullscreenParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

            rootLayout.addView(
                view,
                0,
                fullscreenParams
            )

            // Normal game WebView is behind the fullscreen view.
            gameWebView.visibility = View.GONE

            // --------------------------------------------------------
            // IMPORTANT:
            // Controller WebView stays LAST in the layout.
            // Therefore it remains ABOVE fullscreen game view.
            // --------------------------------------------------------

            controllerWebView.visibility = View.VISIBLE
            controllerWebView.bringToFront()

            gameFullscreenMode()

            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()

            controllerWebView.bringToFront()
        }

        override fun onHideCustomView() {

            fullscreenView?.let {
                rootLayout.removeView(it)
            }

            fullscreenView = null
            fullscreenCallback = null

            gameWebView.visibility = View.VISIBLE

            controllerWebView.visibility = View.VISIBLE
            controllerWebView.bringToFront()

            normalGameMode()

            gameWebView.requestFocus()
        }
    }

    // ================================================================
    // ANDROID CONTROLLER BRIDGE
    // ================================================================

    inner class KeyBridge {

        @JavascriptInterface
        fun key(
            name: String,
            down: Boolean
        ) {

            // --------------------------------------------------------
            // Controller fullscreen button
            // --------------------------------------------------------

            if (name == "__REQUEST_GAME_FULLSCREEN__") {

                runOnUiThread {

                    if (fullscreenView == null) {

                        requestHelmetHeroesFullscreen()

                    } else {

                        exitHelmetHeroesFullscreen()
                    }
                }

                return
            }

            // --------------------------------------------------------
            // Normal controller button
            // --------------------------------------------------------

            val keyCode =
                toKeyCode(name)
                    ?: return

            runOnUiThread {

                sendGameKey(
                    keyCode,
                    down
                )
            }
        }
    }

    // ================================================================
    // SEND KEY TO THE ACTUAL GAME
    // ================================================================

    private fun sendGameKey(
        keyCode: Int,
        down: Boolean
    ) {

        val target =
            fullscreenView ?: gameWebView

        target.isFocusable = true
        target.isFocusableInTouchMode = true
        target.requestFocus()

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

        // ------------------------------------------------------------
        // FIRST: send real Android KeyEvent to the actual game view.
        // ------------------------------------------------------------

        target.dispatchKeyEvent(event)

        // ------------------------------------------------------------
        // SECOND: if fullscreen view is a WebView, also inject the
        // keyboard event directly into that WebView.
        // ------------------------------------------------------------

        if (target is WebView) {

            injectJavascriptKey(
                target,
                keyCode,
                down
            )
        }
    }

    // ================================================================
    // JAVASCRIPT KEY FALLBACK
    // ================================================================

    private fun injectJavascriptKey(
        targetWebView: WebView,
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

        val jsCode =
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

        val eventType =
            if (down) {
                "keydown"
            } else {
                "keyup"
            }

        val js =
            """
            (function() {

                try {

                    var ev = new KeyboardEvent(
                        '$eventType',
                        {
                            key: ${jsKey.quoteJs()},
                            code: ${jsCode.quoteJs()},
                            bubbles: true,
                            cancelable: true
                        }
                    );

                    try {

                        Object.defineProperty(
                            ev,
                            'keyCode',
                            {
                                get: function() {
                                    return $keyCode;
                                }
                            }
                        );

                        Object.defineProperty(
                            ev,
                            'which',
                            {
                                get: function() {
                                    return $keyCode;
                                }
                            }
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

        targetWebView.evaluateJavascript(
            js,
            null
        )
    }

    // ================================================================
    // KEY NAME → ANDROID KEYCODE
    // ================================================================

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

    // ================================================================
    // REQUEST HELMET HEROES FULLSCREEN
    // ================================================================

    private fun requestHelmetHeroesFullscreen() {

        gameWebView.evaluateJavascript(
            """
            (function() {

                try {

                    var element =
                        document.documentElement;

                    if (
                        element.requestFullscreen
                    ) {

                        var p =
                            element.requestFullscreen();

                        if (
                            p &&
                            typeof p.catch === 'function'
                        ) {

                            p.catch(function() {});

                        }

                        return;

                    }

                    if (
                        element.webkitRequestFullscreen
                    ) {

                        element.webkitRequestFullscreen();

                        return;

                    }

                    if (
                        element.mozRequestFullScreen
                    ) {

                        element.mozRequestFullScreen();

                        return;

                    }

                    if (
                        element.msRequestFullscreen
                    ) {

                        element.msRequestFullscreen();

                        return;

                    }

                } catch(e) {}

            })();
            """.trimIndent(),
            null
        )
    }

    // ================================================================
    // EXIT HELMET HEROES FULLSCREEN
    // ================================================================

    private fun exitHelmetHeroesFullscreen() {

        fullscreenCallback?.onCustomViewHidden()

        fullscreenView?.let {

            rootLayout.removeView(it)
        }

        fullscreenView = null
        fullscreenCallback = null

        gameWebView.visibility = View.VISIBLE

        controllerWebView.visibility = View.VISIBLE
        controllerWebView.bringToFront()

        normalGameMode()

        gameWebView.requestFocus()
    }

    // ================================================================
    // FULLSCREEN SYSTEM UI
    // ================================================================

    @Suppress("DEPRECATION")
    private fun gameFullscreenMode() {

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

        controllerWebView.bringToFront()
    }

    @Suppress("DEPRECATION")
    private fun normalGameMode() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        controllerWebView.bringToFront()
    }

    // ================================================================
    // CREATE CONTROLLER WEBVIEW
    // ================================================================

    @SuppressLint(
        "SetJavaScriptEnabled",
        "AddJavascriptInterface"
    )
    private fun createControllerWebView() {

        controllerWebView =
            WebView(this)

        controllerWebView.setBackgroundColor(
            Color.TRANSPARENT
        )

        controllerWebView.setLayerType(
            View.LAYER_TYPE_HARDWARE,
            null
        )

        controllerWebView.isFocusable = false
        controllerWebView.isFocusableInTouchMode = false

        controllerWebView.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        with(controllerWebView.settings) {

            javaScriptEnabled = true

            domStorageEnabled = true

            databaseEnabled = true

            allowFileAccess = true

            allowContentAccess = true

            setSupportZoom(false)

            builtInZoomControls = false

            displayZoomControls = false

            useWideViewPort = false

            loadWithOverviewMode = false
        }

        controllerWebView.addJavascriptInterface(
            KeyBridge(),
            "AndroidKeys"
        )

        controllerWebView.webViewClient =
            object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    super.onPageFinished(
                        view,
                        url
                    )

                    loadControllerJavascript()
                }
            }

        rootLayout.addView(
            controllerWebView
        )

        controllerWebView.bringToFront()

        val controllerPage =
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

                        user-select: none;
                        -webkit-user-select: none;

                    }

                </style>

            </head>

            <body></body>

            </html>
            """.trimIndent()

        controllerWebView.loadDataWithBaseURL(
            "https://controller.local/",
            controllerPage,
            "text/html",
            "UTF-8",
            null
        )
    }

    // ================================================================
    // LOAD YOUR EXISTING CONTROLLER.JS
    // ================================================================

    private fun loadControllerJavascript() {

        try {

            val controller =
                assets.open(
                    "controller.js"
                )
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            controllerWebView.evaluateJavascript(
                controller,
                null
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    // ================================================================
    // CREATE GAME WEBVIEW
    // ================================================================

    @SuppressLint(
        "SetJavaScriptEnabled",
        "AddJavascriptInterface"
    )
    private fun createGameWebView() {

        gameWebView =
            WebView(this)

        gameWebView.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        gameWebView.isFocusable = true
        gameWebView.isFocusableInTouchMode = true

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

            allowFileAccess = true

            allowContentAccess = true

            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"
        }

        gameWebView.webChromeClient =
            chromeClient

        gameWebView.webViewClient =
            object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    super.onPageFinished(
                        view,
                        url
                    )

                    // Make sure controller is always on top.
                    controllerWebView.bringToFront()
                }
            }

        rootLayout.addView(
            gameWebView
        )

        gameWebView.loadUrl(
            GAME_URL
        )
    }

    // ================================================================
    // ACTIVITY CREATE
    // ================================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        rootLayout =
            FrameLayout(this)

        rootLayout.setBackgroundColor(
            Color.BLACK
        )

        rootLayout.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        setContentView(
            rootLayout
        )

        // ------------------------------------------------------------
        // ORDER IS IMPORTANT:
        //
        // 1. Game
        // 2. Controller
        //
        // Controller is therefore always above game.
        // ------------------------------------------------------------

        createGameWebView()

        createControllerWebView()

        controllerWebView.bringToFront()

        gameFullscreenMode()
    }

    // ================================================================
    // KEEP CONTROLLER ABOVE EVERYTHING
    // ================================================================

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(
            hasFocus
        )

        if (hasFocus) {

            gameFullscreenMode()

            controllerWebView.bringToFront()
        }
    }

    override fun onResume() {

        super.onResume()

        gameWebView.onResume()

        controllerWebView.onResume()

        gameFullscreenMode()

        controllerWebView.bringToFront()
    }

    override fun onPause() {

        controllerWebView.onPause()

        gameWebView.onPause()

        super.onPause()
    }

    // ================================================================
    // BACK BUTTON
    // ================================================================

    @Suppress(
        "DEPRECATION",
        "OVERRIDE_DEPRECATION"
    )
    override fun onBackPressed() {

        if (fullscreenView != null) {

            exitHelmetHeroesFullscreen()

            return
        }

        if (gameWebView.canGoBack()) {

            gameWebView.goBack()

            return
        }

        super.onBackPressed()
    }

    // ================================================================
    // CLEANUP
    // ================================================================

    override fun onDestroy() {

        fullscreenView?.let {

            rootLayout.removeView(it)
        }

        controllerWebView.stopLoading()
        gameWebView.stopLoading()

        controllerWebView.destroy()
        gameWebView.destroy()

        super.onDestroy()
    }

    // ================================================================
    // JS STRING ESCAPE
    // ================================================================

    private fun String.quoteJs(): String {

        return "'" +
                this
                    .replace(
                        "\\",
                        "\\\\"
                    )
                    .replace(
                        "'",
                        "\\'"
                    )
                    .replace(
                        "\n",
                        "\\n"
                    )
                    .replace(
                        "\r",
                        "\\r"
                    ) +
                "'"
    }
}
