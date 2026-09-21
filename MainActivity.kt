package com.gamewrap.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val GAME_URL =
            "https://www.helmet-heroes.com/"
    }

    private lateinit var rootLayout: FrameLayout
    private lateinit var gameWebView: WebView

    private var fullscreenView: View? = null
    private var fullscreenCallback:
            WebChromeClient.CustomViewCallback? = null

    private var controllerOverlay: ControllerOverlay? = null

    // ============================================================
    // HELMET HEROES FULLSCREEN
    // ============================================================

    private val chromeClient =
        object : WebChromeClient() {

            override fun onShowCustomView(
                view: View?,
                callback: WebChromeClient.CustomViewCallback?
            ) {

                if (view == null) {
                    callback?.onCustomViewHidden()
                    return
                }

                // Remove old fullscreen view.
                fullscreenView?.let {
                    try {
                        rootLayout.removeView(it)
                    } catch (_: Exception) {
                    }
                }

                fullscreenView = view
                fullscreenCallback = callback

                val params =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )

                params.gravity = Gravity.CENTER

                rootLayout.addView(
                    view,
                    0,
                    params
                )

                gameWebView.visibility =
                    View.GONE

                view.isFocusable = true
                view.isFocusableInTouchMode = true

                enterFullscreen()

                view.requestFocus()

                // ------------------------------------------------
                // CRITICAL:
                // Re-create native controller ABOVE fullscreen.
                // ------------------------------------------------

                showController()

                controllerOverlay?.bringToFront()
            }

            override fun onHideCustomView() {

                fullscreenView?.let {

                    try {
                        rootLayout.removeView(it)
                    } catch (_: Exception) {
                    }
                }

                fullscreenView = null
                fullscreenCallback = null

                gameWebView.visibility =
                    View.VISIBLE

                exitFullscreen()

                gameWebView.requestFocus()

                showController()

                controllerOverlay?.bringToFront()
            }
        }

    // ============================================================
    // ANDROID KEY BRIDGE
    // ============================================================

    inner class KeyBridge {

        @JavascriptInterface
        fun key(
            name: String,
            down: Boolean
        ) {

            if (
                name ==
                "__REQUEST_GAME_FULLSCREEN__"
            ) {

                runOnUiThread {

                    if (fullscreenView == null) {
                        requestGameFullscreen()
                    } else {
                        exitGameFullscreen()
                    }
                }

                return
            }

            val keyCode =
                convertKey(
                    name
                ) ?: return

            runOnUiThread {

                sendKeyToGame(
                    keyCode,
                    down
                )
            }
        }
    }

    // ============================================================
    // REQUEST GAME FULLSCREEN
    // ============================================================

    private fun requestGameFullscreen() {

        try {

            gameWebView.evaluateJavascript(
                """
                (function() {

                    try {

                        var e =
                            document.documentElement;

                        if (
                            e.requestFullscreen
                        ) {

                            e.requestFullscreen()
                                .catch(function(){});

                            return;
                        }

                        if (
                            e.webkitRequestFullscreen
                        ) {

                            e.webkitRequestFullscreen();

                            return;
                        }

                    } catch(err) {}

                })();
                """.trimIndent(),
                null
            )

        } catch (_: Exception) {
        }
    }

    // ============================================================
    // EXIT FULLSCREEN
    // ============================================================

    private fun exitGameFullscreen() {

        try {

            fullscreenCallback?.onCustomViewHidden()

        } catch (_: Exception) {
        }

        fullscreenView?.let {

            try {
                rootLayout.removeView(it)
            } catch (_: Exception) {
            }
        }

        fullscreenView = null
        fullscreenCallback = null

        gameWebView.visibility =
            View.VISIBLE

        exitFullscreen()

        gameWebView.requestFocus()

        showController()

        controllerOverlay?.bringToFront()
    }

    // ============================================================
    // SEND KEY DIRECTLY TO GAME
    // ============================================================

    private fun sendKeyToGame(
        keyCode: Int,
        down: Boolean
    ) {

        val target =
            fullscreenView
                ?: gameWebView

        try {

            target.isFocusable = true
            target.isFocusableInTouchMode = true

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

            // ----------------------------------------------------
            // Send to fullscreen game.
            // ----------------------------------------------------

            target.dispatchKeyEvent(
                event
            )

            // ----------------------------------------------------
            // If target is WebView, also send browser event.
            // ----------------------------------------------------

            if (target is WebView) {

                injectBrowserKey(
                    target,
                    keyCode,
                    down
                )
            }

        } catch (_: Exception) {
        }
    }

    // ============================================================
    // JAVASCRIPT KEY FALLBACK
    // ============================================================

    private fun injectBrowserKey(
        webView: WebView,
        keyCode: Int,
        down: Boolean
    ) {

        val key =
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

                KeyEvent.KEYCODE_SPACE ->
                    " "

                KeyEvent.KEYCODE_ENTER ->
                    "Enter"

                KeyEvent.KEYCODE_ESCAPE ->
                    "Escape"

                KeyEvent.KEYCODE_TAB ->
                    "Tab"

                KeyEvent.KEYCODE_DEL ->
                    "Backspace"

                KeyEvent.KEYCODE_SHIFT_LEFT ->
                    "Shift"

                KeyEvent.KEYCODE_CTRL_LEFT ->
                    "Control"

                KeyEvent.KEYCODE_ALT_LEFT ->
                    "Alt"

                KeyEvent.KEYCODE_DPAD_UP ->
                    "ArrowUp"

                KeyEvent.KEYCODE_DPAD_DOWN ->
                    "ArrowDown"

                KeyEvent.KEYCODE_DPAD_LEFT ->
                    "ArrowLeft"

                KeyEvent.KEYCODE_DPAD_RIGHT ->
                    "ArrowRight"

                else ->
                    return
            }

        val code =
            when (keyCode) {

                in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z ->
                    "Key" +
                            key.uppercase()

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

                else ->
                    ""
            }

        val type =
            if (down) {
                "keydown"
            } else {
                "keyup"
            }

        val js =
            """
            (function() {

                try {

                    var ev =
                        new KeyboardEvent(
                            '$type',
                            {
                                key: '${escapeJs(key)}',
                                code: '${escapeJs(code)}',
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

                } catch(e) {}

            })();
            """.trimIndent()

        webView.evaluateJavascript(
            js,
            null
        )
    }

    // ============================================================
    // KEY CONVERTER
    // ============================================================

    private fun convertKey(
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
    // CONTROLLER OVERLAY
    // ============================================================

    private fun showController() {

        if (controllerOverlay == null) {

            controllerOverlay =
                ControllerOverlay(
                    this
                )

            rootLayout.addView(
                controllerOverlay,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        controllerOverlay?.visibility =
            View.VISIBLE

        controllerOverlay?.bringToFront()
    }

    // ============================================================
    // FULLSCREEN SYSTEM UI
    // ============================================================

    @Suppress("DEPRECATION")
    private fun enterFullscreen() {

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

        controllerOverlay?.bringToFront()
    }

    @Suppress("DEPRECATION")
    private fun exitFullscreen() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    // ============================================================
    // ACTIVITY CREATE
    // ============================================================

    @SuppressLint(
        "SetJavaScriptEnabled",
        "AddJavascriptInterface"
    )
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

        setContentView(
            rootLayout
        )

        // --------------------------------------------------------
        // GAME
        // --------------------------------------------------------

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

            mediaPlaybackRequiresUserGesture =
                false

            useWideViewPort = true

            loadWithOverviewMode = true

            builtInZoomControls = false

            displayZoomControls = false

            setSupportZoom(false)

            allowFileAccess = true

            allowContentAccess = true

            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36"
        }

        gameWebView.webChromeClient =
            chromeClient

        gameWebView.webViewClient =
            WebViewClient()

        gameWebView.addJavascriptInterface(
            KeyBridge(),
            "AndroidKeys"
        )

        rootLayout.addView(
            gameWebView
        )

        gameWebView.loadUrl(
            GAME_URL
        )

        // --------------------------------------------------------
        // CONTROLLER
        // --------------------------------------------------------

        showController()

        controllerOverlay?.bringToFront()

        enterFullscreen()
    }

    // ============================================================
    // KEEP CONTROLLER ABOVE GAME
    // ============================================================

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(
            hasFocus
        )

        if (hasFocus) {

            enterFullscreen()

            controllerOverlay?.bringToFront()
        }
    }

    override fun onResume() {

        super.onResume()

        gameWebView.onResume()

        controllerOverlay?.visibility =
            View.VISIBLE

        controllerOverlay?.bringToFront()

        enterFullscreen()
    }

    override fun onPause() {

        gameWebView.onPause()

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

        if (fullscreenView != null) {

            exitGameFullscreen()

            return
        }

        if (gameWebView.canGoBack()) {

            gameWebView.goBack()

            return
        }

        super.onBackPressed()
    }

    // ============================================================
    // DESTROY
    // ============================================================

    override fun onDestroy() {

        try {

            fullscreenCallback?.onCustomViewHidden()

        } catch (_: Exception) {
        }

        controllerOverlay?.releaseKeys()

        controllerOverlay?.let {

            try {
                rootLayout.removeView(it)
            } catch (_: Exception) {
            }
        }

        gameWebView.stopLoading()
        gameWebView.destroy()

        super.onDestroy()
    }

    // ============================================================
    // JS ESCAPE
    // ============================================================

    private fun escapeJs(
        value: String
    ): String {

        return value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    // ============================================================
    // NATIVE CONTROLLER
    // ============================================================

    inner class ControllerOverlay(
        activity: Activity
    ) : FrameLayout(activity) {

        private val activeKeys =
            HashSet<Int>()

        private var attackPressed =
            false

        init {

            setWillNotDraw(false)

            isClickable = false

            isFocusable = false

            isFocusableInTouchMode = false

            setBackgroundColor(
                Color.TRANSPARENT
            )

            createButtons()
        }

        // --------------------------------------------------------
        // BUTTON CREATOR
        // --------------------------------------------------------

        private fun addControllerButton(
            text: String,
            keyCode: Int,
            leftPercent: Float,
            topPercent: Float,
            widthDp: Int,
            heightDp: Int,
            hold: Boolean = false
        ) {

            val button =
                TextView(
                    this@MainActivity
                )

            button.text =
                text

            button.textSize =
                if (text.length <= 2) {
                    18f
                } else {
                    13f
                }

            button.setTextColor(
                Color.WHITE
            )

            button.gravity =
                Gravity.CENTER

            button.setBackgroundColor(
                Color.argb(
                    145,
                    30,
                    30,
                    30
                )
            )

            button.setPadding(
                0,
                0,
                0,
                0
            )

            button.isClickable =
                true

            button.isFocusable =
                false

            val density =
                resources.displayMetrics.density

            val width =
                (
                    widthDp *
                        density
                    ).toInt()

            val height =
                (
                    heightDp *
                        density
                    ).toInt()

            val params =
                FrameLayout.LayoutParams(
                    width,
                    height
                )

            params.leftMargin =
                (
                    resources.displayMetrics.widthPixels *
                        leftPercent /
                        100f
                    ).toInt()

            params.topMargin =
                (
                    resources.displayMetrics.heightPixels *
                        topPercent /
                        100f
                    ).toInt()

            addView(
                button,
                params
            )

            button.setOnTouchListener {

                _,
                event ->

                when (
                    event.actionMasked
                ) {

                    MotionEvent.ACTION_DOWN -> {

                        button.alpha =
                            0.65f

                        if (hold) {

                            if (!attackPressed) {

                                attackPressed =
                                    true

                                pressKey(
                                    keyCode
                                )
                            }

                        } else {

                            // Normal buttons:
                            // DOWN immediately.
                            pressKey(
                                keyCode
                            )

                            // Normal buttons:
                            // release immediately.
                            releaseKey(
                                keyCode
                            )
                        }

                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {

                        button.alpha =
                            1.0f

                        if (hold) {

                            if (attackPressed) {

                                attackPressed =
                                    false

                                releaseKey(
                                    keyCode
                                )
                            }
                        }

                        true
                    }

                    else -> true
                }
            }
        }

        // --------------------------------------------------------
        // CONTROLLER BUTTON POSITIONS
        // --------------------------------------------------------

        private fun createButtons() {

            // W
            addControllerButton(
                "W",
                KeyEvent.KEYCODE_W,
                15f,
                58f,
                56,
                56
            )

            // A
            addControllerButton(
                "A",
                KeyEvent.KEYCODE_A,
                8f,
                76f,
                56,
                56
            )

            // S
            addControllerButton(
                "S",
                KeyEvent.KEYCODE_S,
                15f,
                94f,
                56,
                56
            )

            // D
            addControllerButton(
                "D",
                KeyEvent.KEYCODE_D,
                22f,
                76f,
                56,
                56
            )

            // ATTACK - ONLY HOLDABLE BUTTON
            addControllerButton(
                "ATK",
                KeyEvent.KEYCODE_SPACE,
                90f,
                80f,
                76,
                60,
                true
            )

            // E
            addControllerButton(
                "E",
                KeyEvent.KEYCODE_E,
                76f,
                86f,
                50,
                50
            )

            // M
            addControllerButton(
                "M",
                KeyEvent.KEYCODE_M,
                68f,
                64f,
                54,
                54
            )

            // N
            addControllerButton(
                "N",
                KeyEvent.KEYCODE_N,
                78f,
                58f,
                54,
                54
            )

            // B
            addControllerButton(
                "B",
                KeyEvent.KEYCODE_B,
                88f,
                56f,
                54,
                54
            )

            // ESC
            addControllerButton(
                "ESC",
                KeyEvent.KEYCODE_ESCAPE,
                94f,
                30f,
                50,
                44
            )

            // ----------------------------------------------------
            // FULLSCREEN BUTTON
            // ----------------------------------------------------

            val full =
                TextView(
                    this@MainActivity
                )

            full.text =
                "FULL"

            full.textSize =
                11f

            full.gravity =
                Gravity.CENTER

            full.setTextColor(
                Color.WHITE
            )

            full.setBackgroundColor(
                Color.argb(
                    145,
                    30,
                    30,
                    30
                )
            )

            val density =
                resources.displayMetrics.density

            val p =
                FrameLayout.LayoutParams(
                    (58 * density).toInt(),
                    (38 * density).toInt()
                )

            p.leftMargin =
                (
                    resources.displayMetrics.widthPixels *
                        2f /
                        100f
                    ).toInt()

            p.topMargin =
                (
                    resources.displayMetrics.heightPixels *
                        3f /
                        100f
                    ).toInt()

            addView(
                full,
                p
            )

            full.setOnClickListener {

                if (fullscreenView == null) {

                    requestGameFullscreen()

                } else {

                    exitGameFullscreen()
                }
            }
        }

        // --------------------------------------------------------
        // PRESS
        // --------------------------------------------------------

        private fun pressKey(
            keyCode: Int
        ) {

            if (
                activeKeys.contains(
                    keyCode
                )
            ) {
                return
            }

            activeKeys.add(
                keyCode
            )

            sendKeyToGame(
                keyCode,
                true
            )
        }

        // --------------------------------------------------------
        // RELEASE
        // --------------------------------------------------------

        private fun releaseKey(
            keyCode: Int
        ) {

            if (
                !activeKeys.contains(
                    keyCode
                )
            ) {
                return
            }

            activeKeys.remove(
                keyCode
            )

            sendKeyToGame(
                keyCode,
                false
            )
        }

        // --------------------------------------------------------
        // RELEASE ALL
        // --------------------------------------------------------

        fun releaseKeys() {

            val copy =
                activeKeys.toList()

            activeKeys.clear()

            for (
                keyCode in copy
            ) {

                sendKeyToGame(
                    keyCode,
                    false
                )
            }

            attackPressed =
                false
        }
    }
}
