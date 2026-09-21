package com.gamewrap.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {

    // ================================================================
    // CONSTANTS
    // ================================================================

    companion object {
        private const val GAME_URL =
            "https://www.helmet-heroes.com/"

        private const val PREFS_NAME =
            "controller_preferences"

        private const val PREF_SCALE =
            "controller_scale"

        private const val IMMERSIVE_DELAY =
            250L
    }

    // ================================================================
    // MAIN VIEWS
    // ================================================================

    private lateinit var rootLayout: FrameLayout
    private lateinit var gameWebView: WebView
    private lateinit var controller: ControllerOverlay

    private var fullscreenView: View? = null
    private var fullscreenCallback:
        WebChromeClient.CustomViewCallback? = null

    private val mainHandler =
        Handler(Looper.getMainLooper())

    // ================================================================
    // ACTIVE KEYS
    // ================================================================

    private val activeKeys =
        mutableSetOf<Int>()

    // ================================================================
    // BUTTON DATA
    // ================================================================

    private data class ButtonData(
        val id: String,
        val label: String,
        val keyCode: Int,
        var x: Float,
        var y: Float,
        var widthDp: Int,
        var heightDp: Int,
        val hold: Boolean = false
    )

    // ================================================================
    // ON CREATE
    // ================================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(
            Window.FEATURE_NO_TITLE
        )

        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManagerFlags.FLAG_FULLSCREEN,
            WindowManagerFlags.FLAG_FULLSCREEN
        )

        rootLayout =
            FrameLayout(this)

        rootLayout.setBackgroundColor(
            Color.BLACK
        )

        setContentView(rootLayout)

        setupGameWebView()

        controller =
            ControllerOverlay(this)

        rootLayout.addView(
            controller,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        controller.bringToFront()
    }

    // ================================================================
    // WEBVIEW SETUP
    // ================================================================

    private fun setupGameWebView() {

        gameWebView =
            WebView(this)

        gameWebView.setBackgroundColor(
            Color.BLACK
        )

        gameWebView.isFocusable = true
        gameWebView.isFocusableInTouchMode = true

        gameWebView.settings.apply {

            javaScriptEnabled = true

            domStorageEnabled = true

            databaseEnabled = true

            mediaPlaybackRequiresUserGesture =
                false

            allowFileAccess = true

            allowContentAccess = true

            javaScriptCanOpenWindowsAutomatically =
                true

            setSupportMultipleWindows(false)

            loadWithOverviewMode = false

            useWideViewPort = true

            cacheMode =
                WebSettings.LOAD_DEFAULT

            builtInZoomControls = false

            displayZoomControls = false

            setSupportZoom(false)

            userAgentString =
                userAgentString +
                        " GameWrapper/1.0"
        }

        gameWebView.webViewClient =
            object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean {

                    if (
                        view != null &&
                        !url.isNullOrEmpty()
                    ) {
                        view.loadUrl(url)
                    }

                    return true
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {

                    super.onPageFinished(
                        view,
                        url
                    )

                    injectKeyboardBridge()

                    controller.bringToFront()
                }
            }

        gameWebView.webChromeClient =
            object : WebChromeClient() {

                override fun onShowCustomView(
                    view: View?,
                    callback:
                        CustomViewCallback?
                ) {

                    if (view == null) {
                        return
                    }

                    showFullscreenView(
                        view,
                        callback
                    )
                }

                override fun onHideCustomView() {
                    hideFullscreenView()
                }
            }

        gameWebView.addJavascriptInterface(
            AndroidBridge(),
            "AndroidKeys"
        )

        rootLayout.addView(
            gameWebView,
            0,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        gameWebView.loadUrl(GAME_URL)
    }

    // ================================================================
    // JAVASCRIPT BRIDGE
    // ================================================================

    private inner class AndroidBridge {

        @JavascriptInterface
        fun key(
            name: String?,
            down: Boolean
        ) {

            if (name.isNullOrEmpty()) {
                return
            }

            if (
                name ==
                "__REQUEST_GAME_FULLSCREEN__"
            ) {

                mainHandler.post {
                    enterBrowserFullscreen()
                }

                return
            }

            val keyCode =
                keyCodeFromName(name)

            if (
                keyCode ==
                KeyEvent.KEYCODE_UNKNOWN
            ) {
                return
            }

            mainHandler.post {

                if (down) {
                    pressKey(keyCode)
                } else {
                    releaseKey(keyCode)
                }
            }
        }
    }

    // ================================================================
    // KEY NAME TO KEY CODE
    // ================================================================

    private fun keyCodeFromName(
        name: String
    ): Int {

        return when (
            name.uppercase()
        ) {

            "W" ->
                KeyEvent.KEYCODE_W

            "A" ->
                KeyEvent.KEYCODE_A

            "S" ->
                KeyEvent.KEYCODE_S

            "D" ->
                KeyEvent.KEYCODE_D

            "E" ->
                KeyEvent.KEYCODE_E

            "M" ->
                KeyEvent.KEYCODE_M

            "N" ->
                KeyEvent.KEYCODE_N

            "B" ->
                KeyEvent.KEYCODE_B

            "ESC",
            "ESCAPE" ->
                KeyEvent.KEYCODE_ESCAPE

            "SPACE",
            "ATTACK" ->
                KeyEvent.KEYCODE_SPACE

            "ENTER" ->
                KeyEvent.KEYCODE_ENTER

            "SHIFT" ->
                KeyEvent.KEYCODE_SHIFT_LEFT

            "CTRL",
            "CONTROL" ->
                KeyEvent.KEYCODE_CTRL_LEFT

            "ALT" ->
                KeyEvent.KEYCODE_ALT_LEFT

            "TAB" ->
                KeyEvent.KEYCODE_TAB

            "UP" ->
                KeyEvent.KEYCODE_DPAD_UP

            "DOWN" ->
                KeyEvent.KEYCODE_DPAD_DOWN

            "LEFT" ->
                KeyEvent.KEYCODE_DPAD_LEFT

            "RIGHT" ->
                KeyEvent.KEYCODE_DPAD_RIGHT

            else ->
                KeyEvent.KEYCODE_UNKNOWN
        }
    }

    // ================================================================
    // PRESS KEY
    // ================================================================

    private fun pressKey(
        keyCode: Int
    ) {

        if (
            keyCode ==
            KeyEvent.KEYCODE_UNKNOWN
        ) {
            return
        }

        if (
            activeKeys.contains(keyCode)
        ) {
            return
        }

        activeKeys.add(keyCode)

        sendKeyToGame(
            keyCode,
            KeyEvent.ACTION_DOWN
        )
    }

    // ================================================================
    // RELEASE KEY
    // ================================================================

    private fun releaseKey(
        keyCode: Int
    ) {

        if (
            keyCode ==
            KeyEvent.KEYCODE_UNKNOWN
        ) {
            return
        }

        activeKeys.remove(keyCode)

        sendKeyToGame(
            keyCode,
            KeyEvent.ACTION_UP
        )
    }

    // ================================================================
    // TAP KEY
    // ================================================================

    private fun tapKey(
        keyCode: Int
    ) {

        pressKey(keyCode)

        releaseKey(keyCode)
    }

    // ================================================================
    // RELEASE ALL
    // ================================================================

    private fun releaseAllKeys() {

        val keys =
            activeKeys.toList()

        for (key in keys) {
            releaseKey(key)
        }

        activeKeys.clear()
    }

    // ================================================================
    // SEND KEY TO GAME
    // ================================================================

    private fun sendKeyToGame(
        keyCode: Int,
        action: Int
    ) {

        val target =
            fullscreenView
                ?: gameWebView

        val eventTime =
            System.currentTimeMillis()

        val event =
            KeyEvent(
                eventTime,
                eventTime,
                action,
                keyCode,
                0
            )

        try {
            target.dispatchKeyEvent(
                event
            )
        } catch (_: Exception) {
        }

        if (
            target is WebView
        ) {

            injectKeyboardEvent(
                target,
                keyCode,
                action ==
                        KeyEvent.ACTION_DOWN
            )
        }
    }

    // ================================================================
    // JAVASCRIPT KEYBOARD BRIDGE
    // ================================================================

    private fun injectKeyboardBridge() {

        val javascript = """
            (function() {
                try {
                    if (!window.__AndroidKeyboardBridgeInstalled) {
                        window.__AndroidKeyboardBridgeInstalled = true;

                        window.__AndroidGameKey = function(name, down) {
                            try {
                                if (window.AndroidController &&
                                    typeof window.AndroidController.sendKey === "function") {

                                    window.AndroidController.sendKey(
                                        name,
                                        down
                                    );
                                }
                            } catch (e) {}
                        };
                    }
                } catch (e) {}
            })();
        """.trimIndent()

        try {

            gameWebView.post {

                gameWebView.evaluateJavascript(
                    javascript,
                    null
                )
            }

        } catch (_: Exception) {
        }
    }

    // ================================================================
    // JAVASCRIPT KEY EVENT
    // ================================================================

    private fun injectKeyboardEvent(
        webView: WebView,
        keyCode: Int,
        down: Boolean
    ) {

        val keyName: String
        val codeName: String

        when (keyCode) {

            KeyEvent.KEYCODE_W -> {
                keyName = "w"
                codeName = "KeyW"
            }

            KeyEvent.KEYCODE_A -> {
                keyName = "a"
                codeName = "KeyA"
            }

            KeyEvent.KEYCODE_S -> {
                keyName = "s"
                codeName = "KeyS"
            }

            KeyEvent.KEYCODE_D -> {
                keyName = "d"
                codeName = "KeyD"
            }

            KeyEvent.KEYCODE_E -> {
                keyName = "e"
                codeName = "KeyE"
            }

            KeyEvent.KEYCODE_M -> {
                keyName = "m"
                codeName = "KeyM"
            }

            KeyEvent.KEYCODE_N -> {
                keyName = "n"
                codeName = "KeyN"
            }

            KeyEvent.KEYCODE_B -> {
                keyName = "b"
                codeName = "KeyB"
            }

            KeyEvent.KEYCODE_ESCAPE -> {
                keyName = "Escape"
                codeName = "Escape"
            }

            KeyEvent.KEYCODE_SPACE -> {
                keyName = " "
                codeName = "Space"
            }

            KeyEvent.KEYCODE_ENTER -> {
                keyName = "Enter"
                codeName = "Enter"
            }

            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                keyName = "Shift"
                codeName = "ShiftLeft"
            }

            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT -> {
                keyName = "Control"
                codeName = "ControlLeft"
            }

            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT -> {
                keyName = "Alt"
                codeName = "AltLeft"
            }

            KeyEvent.KEYCODE_TAB -> {
                keyName = "Tab"
                codeName = "Tab"
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                keyName = "ArrowUp"
                codeName = "ArrowUp"
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                keyName = "ArrowDown"
                codeName = "ArrowDown"
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                keyName = "ArrowLeft"
                codeName = "ArrowLeft"
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                keyName = "ArrowRight"
                codeName = "ArrowRight"
            }

            else -> {
                return
            }
        }

        val type =
            if (down) {
                "keydown"
            } else {
                "keyup"
            }

        val safeKey =
            keyName
                .replace(
                    "\\",
                    "\\\\"
                )
                .replace(
                    "'",
                    "\\'"
                )

        val safeCode =
            codeName
                .replace(
                    "\\",
                    "\\\\"
                )
                .replace(
                    "'",
                    "\\'"
                )

        val javascript = """
            (function() {
                try {

                    var event =
                        new KeyboardEvent(
                            '$type',
                            {
                                key: '$safeKey',
                                code: '$safeCode',
                                bubbles: true,
                                cancelable: true,
                                composed: true
                            }
                        );

                    document.dispatchEvent(event);

                    window.dispatchEvent(event);

                    if (document.activeElement) {
                        document.activeElement.dispatchEvent(event);
                    }

                } catch (e) {}
            })();
        """.trimIndent()

        try {

            webView.post {

                webView.evaluateJavascript(
                    javascript,
                    null
                )
            }

        } catch (_: Exception) {
        }
    }

    // ================================================================
    // REQUEST BROWSER FULLSCREEN
    // ================================================================

    private fun enterBrowserFullscreen() {

        try {

            gameWebView.evaluateJavascript(
                """
                (function() {
                    try {

                        var element =
                            document.documentElement ||
                            document.body;

                        if (
                            element &&
                            element.requestFullscreen
                        ) {

                            element.requestFullscreen();

                        } else if (
                            element &&
                            element.webkitRequestFullscreen
                        ) {

                            element.webkitRequestFullscreen();
                        }

                    } catch (e) {}
                })();
                """.trimIndent(),
                null
            )

        } catch (_: Exception) {
        }
    }

    // ================================================================
    // SHOW ANDROID WEBVIEW FULLSCREEN
    // ================================================================

    private fun showFullscreenView(
        view: View,
        callback:
            WebChromeClient.CustomViewCallback?
    ) {

        if (
            fullscreenView != null
        ) {
            hideFullscreenView()
        }

        fullscreenView = view

        fullscreenCallback =
            callback

        try {

            (
                view.parent
                    as? ViewGroup
                )?.removeView(view)

        } catch (_: Exception) {
        }

        rootLayout.addView(
            view,
            0,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        gameWebView.visibility =
            View.GONE

        view.isFocusable = true

        view.isFocusableInTouchMode =
            true

        view.requestFocus()

        enterImmersiveMode()

        controller.visibility =
            View.VISIBLE

        controller.bringToFront()

        mainHandler.postDelayed(
            {
                controller.bringToFront()
            },
            IMMERSIVE_DELAY
        )
    }

    // ================================================================
    // HIDE ANDROID WEBVIEW FULLSCREEN
    // ================================================================

    private fun hideFullscreenView() {

        val view =
            fullscreenView

        fullscreenView = null

        if (view != null) {

            try {
                rootLayout.removeView(
                    view
                )
            } catch (_: Exception) {
            }
        }

        try {

            fullscreenCallback
                ?.onCustomViewHidden()

        } catch (_: Exception) {
        }

        fullscreenCallback =
            null

        gameWebView.visibility =
            View.VISIBLE

        exitImmersiveMode()

        gameWebView.requestFocus()

        controller.visibility =
            View.VISIBLE

        controller.bringToFront()
    }

    // ================================================================
    // ENTER IMMERSIVE
    // ================================================================

    private fun enterImmersiveMode() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {

            window.insetsController
                ?.let { insetsController ->

                    insetsController.hide(
                        WindowInsets.Type.statusBars() or
                                WindowInsets.Type.navigationBars()
                    )

                    insetsController.systemBarsBehavior =
                        WindowInsetsController
                            .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }

        } else {

            @Suppress("DEPRECATION")

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    // ================================================================
    // EXIT IMMERSIVE
    // ================================================================

    private fun exitImmersiveMode() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {

            window.insetsController
                ?.show(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )

        } else {

            @Suppress("DEPRECATION")

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    // ================================================================
    // CONTROLLER
    // ================================================================

    inner class ControllerOverlay(
        context: Context
    ) : FrameLayout(context) {

        private val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        private val buttons =
            mutableMapOf<String, TextView>()

        private val dataList =
            mutableListOf<ButtonData>()

        private var editMode =
            false

        private var controllerScale =
            prefs.getFloat(
                PREF_SCALE,
                1.0f
            )

        // Toolbar is a LinearLayout because
        // it contains multiple child buttons.
        private lateinit var toolbar:
            LinearLayout

        private lateinit var editButton:
            TextView

        private lateinit var resetButton:
            TextView

        private lateinit var minusButton:
            TextView

        private lateinit var scaleText:
            TextView

        private lateinit var plusButton:
            TextView

        private val toolbarWidthDp =
            310

        private val toolbarHeightDp =
            48

        init {

            setWillNotDraw(false)

            isClickable = false

            isFocusable = false

            setBackgroundColor(
                Color.TRANSPARENT
            )

            createDefaultButtonData()

            createControllerButtons()

            createToolbar()

            applyScale()

            post {

                applyPositions()

                applyToolbarPosition()

                bringToFront()
            }
        }

        // ============================================================
        // DEFAULT BUTTONS
        // ============================================================

        private fun createDefaultButtonData() {

            dataList.clear()

            dataList.add(
                ButtonData(
                    "W",
                    "W",
                    KeyEvent.KEYCODE_W,
                    15f,
                    58f,
                    56,
                    56
                )
            )

            dataList.add(
                ButtonData(
                    "A",
                    "A",
                    KeyEvent.KEYCODE_A,
                    8f,
                    76f,
                    56,
                    56
                )
            )

            dataList.add(
                ButtonData(
                    "S",
                    "S",
                    KeyEvent.KEYCODE_S,
                    15f,
                    94f,
                    56,
                    56
                )
            )

            dataList.add(
                ButtonData(
                    "D",
                    "D",
                    KeyEvent.KEYCODE_D,
                    22f,
                    76f,
                    56,
                    56
                )
            )

            dataList.add(
                ButtonData(
                    "ATTACK",
                    "SPACE",
                    KeyEvent.KEYCODE_SPACE,
                    90f,
                    80f,
                    76,
                    60,
                    true
                )
            )

            dataList.add(
                ButtonData(
                    "E",
                    "E",
                    KeyEvent.KEYCODE_E,
                    76f,
                    86f,
                    50,
                    50
                )
            )

            dataList.add(
                ButtonData(
                    "M",
                    "M",
                    KeyEvent.KEYCODE_M,
                    68f,
                    64f,
                    54,
                    54
                )
            )

            dataList.add(
                ButtonData(
                    "N",
                    "N",
                    KeyEvent.KEYCODE_N,
                    78f,
                    58f,
                    54,
                    54
                )
            )

            dataList.add(
                ButtonData(
                    "B",
                    "B",
                    KeyEvent.KEYCODE_B,
                    88f,
                    56f,
                    54,
                    54
                )
            )

            dataList.add(
                ButtonData(
                    "ESC",
                    "ESC",
                    KeyEvent.KEYCODE_ESCAPE,
                    94f,
                    30f,
                    50,
                    44
                )
            )

            loadSavedPositions()
        }

        // ============================================================
        // LOAD POSITIONS
        // ============================================================

        private fun loadSavedPositions() {

            for (data in dataList) {

                data.x =
                    prefs.getFloat(
                        "x_${data.id}",
                        data.x
                    )

                data.y =
                    prefs.getFloat(
                        "y_${data.id}",
                        data.y
                    )
            }
        }

        // ============================================================
        // CREATE BUTTONS
        // ============================================================

        private fun createControllerButtons() {

            for (data in dataList) {

                val button =
                    TextView(context)

                button.text =
                    data.label

                button.gravity =
                    Gravity.CENTER

                button.setTextColor(
                    Color.WHITE
                )

                button.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )

                button.textSize =
                    if (
                        data.id ==
                        "ATTACK"
                    ) {
                        13f
                    } else {
                        18f
                    }

                button.setPadding(
                    2,
                    2,
                    2,
                    2
                )

                button.background =
                    createButtonBackground()

                button.elevation =
                    dp(5).toFloat()

                button.isClickable =
                    true

                button.isFocusable =
                    false

                button.tag =
                    data.id

                addView(
                    button,
                    FrameLayout.LayoutParams(
                        dp(data.widthDp),
                        dp(data.heightDp)
                    )
                )

                buttons[data.id] =
                    button

                attachButtonTouchListener(
                    button,
                    data
                )
            }
        }

        // ============================================================
        // BUTTON BACKGROUND
        // ============================================================

        private fun createButtonBackground():
            android.graphics.drawable.GradientDrawable {

            val background =
                android.graphics.drawable.GradientDrawable()

            background.shape =
                android.graphics.drawable.GradientDrawable.RECTANGLE

            background.cornerRadius =
                dp(12).toFloat()

            background.setColor(
                Color.argb(
                    175,
                    25,
                    25,
                    25
                )
            )

            background.setStroke(
                dp(2),
                Color.argb(
                    210,
                    255,
                    255,
                    255
                )
            )

            return background
        }

        // ============================================================
        // BUTTON TOUCH
        // ============================================================

        private fun attachButtonTouchListener(
            button: TextView,
            data: ButtonData
        ) {

            var downX = 0f
            var downY = 0f

            var originalLeft = 0
            var originalTop = 0

            var dragging = false

            button.setOnTouchListener {
                view,
                event ->

                when (
                    event.actionMasked
                ) {

                    MotionEvent.ACTION_DOWN -> {

                        downX =
                            event.rawX

                        downY =
                            event.rawY

                        originalLeft =
                            view.left

                        originalTop =
                            view.top

                        dragging =
                            false

                        if (!editMode) {

                            if (data.hold) {

                                pressKey(
                                    data.keyCode
                                )

                            } else {

                                tapKey(
                                    data.keyCode
                                )
                            }

                            view.alpha =
                                0.65f
                        }

                        true
                    }

                    MotionEvent.ACTION_MOVE -> {

                        if (editMode) {

                            val dx =
                                event.rawX -
                                        downX

                            val dy =
                                event.rawY -
                                        downY

                            if (
                                abs(dx) >
                                dp(4) ||
                                abs(dy) >
                                dp(4)
                            ) {
                                dragging =
                                    true
                            }

                            moveButton(
                                view,
                                originalLeft +
                                        dx.toInt(),
                                originalTop +
                                        dy.toInt()
                            )

                            true

                        } else {

                            true
                        }
                    }

                    MotionEvent.ACTION_UP -> {

                        if (editMode) {

                            if (dragging) {

                                saveButtonPosition(
                                    data,
                                    view
                                )
                            }

                        } else {

                            if (data.hold) {

                                releaseKey(
                                    data.keyCode
                                )
                            }

                            view.alpha =
                                1f
                        }

                        true
                    }

                    MotionEvent.ACTION_CANCEL -> {

                        if (!editMode) {

                            if (data.hold) {

                                releaseKey(
                                    data.keyCode
                                )
                            }

                            view.alpha =
                                1f
                        }

                        true
                    }

                    else -> true
                }
            }
        }

        // ============================================================
        // MOVE BUTTON
        // ============================================================

        private fun moveButton(
            view: View,
            requestedLeft: Int,
            requestedTop: Int
        ) {

            val maxLeft =
                max(
                    0,
                    width - view.width
                )

            val maxTop =
                max(
                    0,
                    height - view.height
                )

            val left =
                requestedLeft.coerceIn(
                    0,
                    maxLeft
                )

            val top =
                requestedTop.coerceIn(
                    0,
                    maxTop
                )

            view.layout(
                left,
                top,
                left + view.width,
                top + view.height
            )
        }

        // ============================================================
        // SAVE BUTTON POSITION
        // ============================================================

        private fun saveButtonPosition(
            data: ButtonData,
            view: View
        ) {

            if (
                width <= 0 ||
                height <= 0
            ) {
                return
            }

            data.x =
                view.left.toFloat() /
                        width.toFloat() *
                        100f

            data.y =
                view.top.toFloat() /
                        height.toFloat() *
                        100f

            prefs.edit()
                .putFloat(
                    "x_${data.id}",
                    data.x
                )
                .putFloat(
                    "y_${data.id}",
                    data.y
                )
                .apply()
        }

        // ============================================================
        // CREATE TOOLBAR
        // ============================================================

        private fun createToolbar() {

            toolbar =
                LinearLayout(context)

            toolbar.orientation =
                LinearLayout.HORIZONTAL

            toolbar.gravity =
                Gravity.CENTER

            toolbar.setPadding(
                dp(4),
                dp(3),
                dp(4),
                dp(3)
            )

            toolbar.background =
                createToolbarBackground()

            toolbar.elevation =
                dp(10).toFloat()

            addView(
                toolbar,
                FrameLayout.LayoutParams(
                    dp(toolbarWidthDp),
                    dp(toolbarHeightDp)
                )
            )

            editButton =
                createToolbarButton(
                    "EDIT"
                )

            resetButton =
                createToolbarButton(
                    "RESET"
                )

            minusButton =
                createToolbarButton(
                    "−"
                )

            scaleText =
                createToolbarButton(
                    "1.0x"
                )

            plusButton =
                createToolbarButton(
                    "+"
                )

            toolbar.addView(
                editButton,
                LinearLayout.LayoutParams(
                    dp(62),
                    dp(42)
                )
            )

            toolbar.addView(
                resetButton,
                LinearLayout.LayoutParams(
                    dp(72),
                    dp(42)
                )
            )

            toolbar.addView(
                minusButton,
                LinearLayout.LayoutParams(
                    dp(42),
                    dp(42)
                )
            )

            toolbar.addView(
                scaleText,
                LinearLayout.LayoutParams(
                    dp(52),
                    dp(42)
                )
            )

            toolbar.addView(
                plusButton,
                LinearLayout.LayoutParams(
                    dp(42),
                    dp(42)
                )
            )

            editButton.setOnClickListener {

                editMode =
                    !editMode

                updateEditModeUI()
            }

            resetButton.setOnClickListener {

                resetController()
            }

            minusButton.setOnClickListener {

                controllerScale =
                    max(
                        0.6f,
                        controllerScale -
                                0.1f
                    )

                saveScale()

                applyScale()

                applyPositions()

                applyToolbarPosition()
            }

            plusButton.setOnClickListener {

                controllerScale =
                    min(
                        1.8f,
                        controllerScale +
                                0.1f
                    )

                saveScale()

                applyScale()

                applyPositions()

                applyToolbarPosition()
            }

            scaleText.setOnClickListener {

                controllerScale =
                    1.0f

                saveScale()

                applyScale()

                applyPositions()

                applyToolbarPosition()
            }

            updateEditModeUI()
        }

        // ============================================================
        // TOOLBAR BUTTON
        // ============================================================

        private fun createToolbarButton(
            text: String
        ): TextView {

            val view =
                TextView(context)

            view.text =
                text

            view.gravity =
                Gravity.CENTER

            view.setTextColor(
                Color.WHITE
            )

            view.textSize =
                12f

            view.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

            view.background =
                createToolbarButtonBackground()

            return view
        }

        // ============================================================
        // TOOLBAR BACKGROUND
        // ============================================================

        private fun createToolbarBackground():
            android.graphics.drawable.GradientDrawable {

            val background =
                android.graphics.drawable.GradientDrawable()

            background.shape =
                android.graphics.drawable.GradientDrawable.RECTANGLE

            background.cornerRadius =
                dp(12).toFloat()

            background.setColor(
                Color.argb(
                    205,
                    15,
                    15,
                    15
                )
            )

            background.setStroke(
                dp(1),
                Color.argb(
                    150,
                    255,
                    255,
                    255
                )
            )

            return background
        }

        // ============================================================
        // TOOLBAR BUTTON BACKGROUND
        // ============================================================

        private fun createToolbarButtonBackground():
            android.graphics.drawable.GradientDrawable {

            val background =
                android.graphics.drawable.GradientDrawable()

            background.shape =
                android.graphics.drawable.GradientDrawable.RECTANGLE

            background.cornerRadius =
                dp(8).toFloat()

            background.setColor(
                Color.argb(
                    160,
                    55,
                    55,
                    55
                )
            )

            background.setStroke(
                dp(1),
                Color.argb(
                    130,
                    255,
                    255,
                    255
                )
            )

            return background
        }

        // ============================================================
        // EDIT MODE UI
        // ============================================================

        private fun updateEditModeUI() {

            if (editMode) {

                editButton.text =
                    "DONE"

                for (
                    button in
                    buttons.values
                ) {
                    button.alpha =
                        0.85f
                }

            } else {

                editButton.text =
                    "EDIT"

                for (
                    button in
                    buttons.values
                ) {
                    button.alpha =
                        1f
                }
            }
        }

        // ============================================================
        // SCALE
        // ============================================================

        private fun applyScale() {

            for (
                data in
                dataList
            ) {

                val button =
                    buttons[data.id]
                        ?: continue

                val newWidth =
                    (
                        dp(data.widthDp) *
                                controllerScale
                        ).toInt()

                val newHeight =
                    (
                        dp(data.heightDp) *
                                controllerScale
                        ).toInt()

                button.layoutParams =
                    FrameLayout.LayoutParams(
                        newWidth,
                        newHeight
                    )
            }

            scaleText.text =
                String.format(
                    "%.1fx",
                    controllerScale
                )

            requestLayout()
        }

        // ============================================================
        // APPLY POSITIONS
        // ============================================================

        private fun applyPositions() {

            if (
                width <= 0 ||
                height <= 0
            ) {
                return
            }

            for (
                data in
                dataList
            ) {

                val button =
                    buttons[data.id]
                        ?: continue

                if (
                    button.width <= 0 ||
                    button.height <= 0
                ) {
                    continue
                }

                val left =
                    (
                        width *
                                (
                                    data.x /
                                            100f
                                    )
                        ).toInt()

                val top =
                    (
                        height *
                                (
                                    data.y /
                                            100f
                                    )
                        ).toInt()

                val maxLeft =
                    max(
                        0,
                        width -
                                button.width
                    )

                val maxTop =
                    max(
                        0,
                        height -
                                button.height
                    )

                val finalLeft =
                    left.coerceIn(
                        0,
                        maxLeft
                    )

                val finalTop =
                    top.coerceIn(
                        0,
                        maxTop
                    )

                button.layout(
                    finalLeft,
                    finalTop,
                    finalLeft +
                            button.width,
                    finalTop +
                            button.height
                )
            }
        }

        // ============================================================
        // APPLY TOOLBAR POSITION
        // ============================================================

        private fun applyToolbarPosition() {

            if (
                !::toolbar.isInitialized
            ) {
                return
            }

            if (
                width <= 0 ||
                height <= 0
            ) {
                return
            }

            val toolbarWidth =
                if (toolbar.width > 0) {
                    toolbar.width
                } else {
                    dp(toolbarWidthDp)
                }

            val toolbarHeight =
                if (toolbar.height > 0) {
                    toolbar.height
                } else {
                    dp(toolbarHeightDp)
                }

            val left =
                (
                    width -
                            toolbarWidth
                    ) / 2

            val top =
                dp(8)

            toolbar.layout(
                left.coerceAtLeast(0),
                top.coerceAtLeast(0),
                (
                    left +
                            toolbarWidth
                    ).coerceAtMost(width),
                (
                    top +
                            toolbarHeight
                    ).coerceAtMost(height)
            )
        }

        // ============================================================
        // SIZE CHANGED
        // ============================================================

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int
        ) {

            super.onSizeChanged(
                w,
                h,
                oldw,
                oldh
            )

            post {

                applyPositions()

                applyToolbarPosition()
            }
        }

        // ============================================================
        // SAVE SCALE
        // ============================================================

        private fun saveScale() {

            prefs.edit()
                .putFloat(
                    PREF_SCALE,
                    controllerScale
                )
                .apply()
        }

        // ============================================================
        // RESET
        // ============================================================

        private fun resetController() {

            prefs.edit()
                .clear()
                .apply()

            controllerScale =
                1.0f

            createDefaultButtonData()

            applyScale()

            post {

                applyPositions()

                applyToolbarPosition()
            }

            Toast.makeText(
                context,
                "Controller reset",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ============================================================
        // DP
        // ============================================================

        private fun dp(
            value: Int
        ): Int {

            return (
                value *
                        resources
                            .displayMetrics
                            .density
                ).toInt()
        }
    }

    // ================================================================
    // WINDOW FOCUS
    // ================================================================

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(
            hasFocus
        )

        if (!hasFocus) {

            releaseAllKeys()

        } else {

            controller.bringToFront()

            if (
                fullscreenView != null
            ) {
                enterImmersiveMode()
            }
        }
    }

    // ================================================================
    // PAUSE
    // ================================================================

    override fun onPause() {

        releaseAllKeys()

        super.onPause()
    }

    // ================================================================
    // STOP
    // ================================================================

    override fun onStop() {

        releaseAllKeys()

        super.onStop()
    }

    // ================================================================
    // BACK
    // ================================================================

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (
            fullscreenView != null
        ) {

            hideFullscreenView()

            return
        }

        if (
            gameWebView.canGoBack()
        ) {

            gameWebView.goBack()

            return
        }

        super.onBackPressed()
    }

    // ================================================================
    // DESTROY
    // ================================================================

    override fun onDestroy() {

        releaseAllKeys()

        try {
            gameWebView.stopLoading()
        } catch (_: Exception) {
        }

        try {

            gameWebView.removeJavascriptInterface(
                "AndroidKeys"
            )

        } catch (_: Exception) {
        }

        try {

            gameWebView.destroy()

        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    // ================================================================
    // FULLSCREEN FLAG
    // ================================================================

    private object WindowManagerFlags {
        const val FLAG_FULLSCREEN =
            1024
    }
}
