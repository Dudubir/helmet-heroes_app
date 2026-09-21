package com.gamewrap.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {

    companion object {

        private const val GAME_URL =
            "https://www.helmet-heroes.com/"

        private const val PREFS =
            "editable_controller"

        private const val EDIT_MODE =
            "edit_mode"

        private const val CONTROLLER_SCALE =
            "controller_scale"
    }

    // ============================================================
    // EDITABLE CONTROLLER BUTTON DATA
    //
    // IMPORTANT:
    // This is outside ControllerOverlay because Kotlin does not
    // allow this nested class declaration in the previous scope.
    // ============================================================

    private data class ButtonData(
        val id: String,
        val label: String,
        val keyCode: Int,
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float,
        val hold: Boolean
    )

    private lateinit var rootLayout: FrameLayout
    private lateinit var gameWebView: WebView
    private lateinit var controller: ControllerOverlay

    private var fullscreenView: View? = null
    private var fullscreenCallback:
            WebChromeClient.CustomViewCallback? = null

    // ============================================================
    // FULLSCREEN WEBVIEW
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

                // Remove previous fullscreen view.
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

                params.gravity =
                    Gravity.CENTER

                /*
                 * Put fullscreen game at index 0.
                 *
                 * The editable controller is added later and therefore
                 * stays ABOVE the fullscreen game.
                 */
                rootLayout.addView(
                    view,
                    0,
                    params
                )

                gameWebView.visibility =
                    View.GONE

                view.isFocusable =
                    true

                view.isFocusableInTouchMode =
                    true

                enterImmersive()

                view.requestFocus()

                controller.visibility =
                    View.VISIBLE

                controller.bringToFront()
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

                exitImmersive()

                gameWebView.requestFocus()

                controller.visibility =
                    View.VISIBLE

                controller.bringToFront()
            }
        }

    // ============================================================
    // ANDROID JAVASCRIPT BRIDGE
    // ============================================================

    inner class AndroidBridge {

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
                        requestFullscreen()
                    } else {
                        exitFullscreen()
                    }
                }

                return
            }

            val keyCode =
                keyCodeFromName(name)
                    ?: return

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

    private fun requestFullscreen() {

        try {

            gameWebView.evaluateJavascript(
                """
                (function() {

                    try {

                        var el =
                            document.documentElement;

                        if (
                            el.requestFullscreen
                        ) {

                            var p =
                                el.requestFullscreen();

                            if (
                                p &&
                                p.catch
                            ) {

                                p.catch(
                                    function() {}
                                );
                            }

                            return;
                        }

                        if (
                            el.webkitRequestFullscreen
                        ) {

                            el.webkitRequestFullscreen();

                            return;
                        }

                    } catch(e) {}

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

    private fun exitFullscreen() {

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

        controller.releaseAllKeys()

        controller.visibility =
            View.VISIBLE

        controller.bringToFront()

        exitImmersive()

        gameWebView.requestFocus()
    }

    // ============================================================
    // SEND KEY TO GAME
    // ============================================================

    private fun sendKeyToGame(
        keyCode: Int,
        down: Boolean
    ) {

        val target =
            fullscreenView ?: gameWebView

        try {

            target.isFocusable =
                true

            target.isFocusableInTouchMode =
                true

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
             * Send directly to the fullscreen game view.
             */
            target.dispatchKeyEvent(
                event
            )

            /*
             * Additional JavaScript fallback when the target
             * is the normal WebView.
             */
            if (
                target is WebView
            ) {

                injectBrowserEvent(
                    target,
                    keyCode,
                    down
                )
            }

        } catch (_: Exception) {
        }
    }

    // ============================================================
    // JAVASCRIPT KEYBOARD FALLBACK
    // ============================================================

    private fun injectBrowserEvent(
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
                    "Key" + key.uppercase(Locale.US)

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

        val eventType =
            if (down) {
                "keydown"
            } else {
                "keyup"
            }

        val jsKey =
            escapeJs(key)

        val jsCode =
            escapeJs(code)

        val js =
            """
            (function() {

                try {

                    var ev =
                        new KeyboardEvent(
                            '$eventType',
                            {
                                key: '$jsKey',
                                code: '$jsCode',
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
    // KEY NAME -> ANDROID KEYCODE
    // ============================================================

    private fun keyCodeFromName(
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
    // EDITABLE CONTROLLER
    // ============================================================

    inner class ControllerOverlay(
        activity: Activity
    ) : FrameLayout(activity) {

        private val prefs =
            getSharedPreferences(
                PREFS,
                MODE_PRIVATE
            )

        private val buttons =
            mutableListOf<ButtonData>()

        private val buttonViews =
            HashMap<String, TextView>()

        private val activeKeys =
            HashSet<Int>()

        private var attackDown =
            false

        private var editMode =
            prefs.getBoolean(
                EDIT_MODE,
                false
            )

        private var controllerScale =
            prefs.getFloat(
                CONTROLLER_SCALE,
                1.0f
            )

        private var editButton:
                TextView? = null

        private var scaleLabel:
                TextView? = null

        private var toolbar:
                FrameLayout? = null

        init {

            isClickable =
                false

            isFocusable =
                false

            setBackgroundColor(
                Color.TRANSPARENT
            )

            createButtonData()

            post {

                buildController()

                loadSavedPositions()

                applyPositions()
            }
        }

        // ========================================================
        // DEFAULT BUTTONS
        // ========================================================

        private fun createButtonData() {

            buttons.clear()

            buttons.add(
                ButtonData(
                    "W",
                    "W",
                    KeyEvent.KEYCODE_W,
                    15f,
                    58f,
                    56f,
                    56f,
                    false
                )
            )

            buttons.add(
                ButtonData(
                    "A",
                    "A",
                    KeyEvent.KEYCODE_A,
                    8f,
                    76f,
                    56f,
                    56f,
                    false
                )
            )

            buttons.add(
                ButtonData(
                    "S",
                    "S",
                    KeyEvent.KEYCODE_S,
                    15f,
                    94f,
                    56f,
                    56f,
                    false
                )
            )

            buttons.add(
                ButtonData(
                    "D",
                    "D",
                    KeyEvent.KEYCODE_D,
                    22f,
                    76f,
                    56f,
                    56f,
                    false
                )
            )

            buttons.add(
                ButtonData(
                    "ATTACK",
                    "ATK",
                    KeyEvent.KEYCODE_SPACE,
                    90f,
                    80f,
                    76f,
                    60f,
                    true
                )
            )

            buttons.add(
                ButtonData(
                    "E",
                    "E",
                    KeyEvent.KEYCODE_E,
                    76f,
                    86f,
                    50f,
                    50f,
                    false
                )
            )

            buttons.add(
                ButtonData(
                    "M",
                    "M",
                    KeyEvent.KEYCODE_M,
                    68f,
                    64f,
                    54f,
                    54f,
                    false
                )
            )

            buttons.add(
                ButtonData(
                    "N",
                    "N",
                    KeyEvent.KEYCODE_N,
                    78f,
                    58f,
                    54f,
                    54f,
                    false
                )
            )

            buttons.add(
                ButtonData(
                    "B",
                    "B",
                    KeyEvent.KEYCODE_B,
                    88f,
                    56f,
                    54f,
                    54f,
                    false
                )
            )

            buttons.add(
                ButtonData(
                    "ESC",
                    "ESC",
                    KeyEvent.KEYCODE_ESCAPE,
                    94f,
                    30f,
                    50f,
                    44f,
                    false
                )
            )
        }

        // ========================================================
        // BUILD CONTROLLER
        // ========================================================

        private fun buildController() {

            removeAllViews()

            buttonViews.clear()

            toolbar = null
            editButton = null
            scaleLabel = null

            for (data in buttons) {

                val button =
                    makeButton(data)

                buttonViews[data.id] =
                    button

                addView(button)
            }

            createEditToolbar()
        }

        // ========================================================
        // CREATE GAME BUTTON
        // ========================================================

        private fun makeButton(
            data: ButtonData
        ): TextView {

            val button =
                TextView(
                    this@MainActivity
                )

            button.text =
                data.label

            button.gravity =
                Gravity.CENTER

            button.setTextColor(
                Color.WHITE
            )

            button.textSize =
                if (
                    data.id == "ATTACK"
                ) {
                    12f
                } else {
                    17f
                }

            button.isClickable =
                true

            button.isFocusable =
                false

            button.setBackground(
                createButtonBackground()
            )

            button.setOnTouchListener(
                createTouchHandler(
                    button,
                    data
                )
            )

            return button
        }

        // ========================================================
        // BUTTON TOUCH HANDLER
        // ========================================================

        private fun createTouchHandler(
            button: TextView,
            data: ButtonData
        ): OnTouchListener {

            var downX =
                0f

            var downY =
                0f

            var originalX =
                0f

            var originalY =
                0f

            var moved =
                false

            return OnTouchListener {

                _,
                event ->

                when (
                    event.actionMasked
                ) {

                    MotionEvent.ACTION_DOWN -> {

                        downX =
                            event.rawX

                        downY =
                            event.rawY

                        originalX =
                            data.x

                        originalY =
                            data.y

                        moved =
                            false

                        button.alpha =
                            0.65f

                        if (editMode) {

                            true

                        } else {

                            if (data.hold) {

                                if (!attackDown) {

                                    attackDown =
                                        true

                                    pressKey(
                                        data.keyCode
                                    )
                                }

                            } else {

                                /*
                                 * W/A/S/D/E/M/N/B/ESC:
                                 * quick tap only.
                                 */
                                pressKey(
                                    data.keyCode
                                )

                                releaseKey(
                                    data.keyCode
                                )
                            }

                            true
                        }
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
                                abs(dx) > 4f ||
                                abs(dy) > 4f
                            ) {

                                moved =
                                    true
                            }

                            if (moved) {

                                moveButton(
                                    data,
                                    originalX,
                                    originalY,
                                    dx,
                                    dy
                                )
                            }

                            true

                        } else {

                            /*
                             * ATTACK remains held until
                             * ACTION_UP / ACTION_CANCEL.
                             */
                            true
                        }
                    }

                    MotionEvent.ACTION_UP -> {

                        button.alpha =
                            1.0f

                        if (editMode) {

                            saveButton(
                                data
                            )

                            true

                        } else {

                            if (data.hold) {

                                if (attackDown) {

                                    attackDown =
                                        false

                                    releaseKey(
                                        data.keyCode
                                    )
                                }
                            }

                            true
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {

                        button.alpha =
                            1.0f

                        if (!editMode) {

                            if (data.hold) {

                                if (attackDown) {

                                    attackDown =
                                        false

                                    releaseKey(
                                        data.keyCode
                                    )
                                }
                            }
                        }

                        true
                    }

                    else -> true
                }
            }
        }

        // ========================================================
        // MOVE BUTTON
        // ========================================================

        private fun moveButton(
            data: ButtonData,
            originalX: Float,
            originalY: Float,
            dx: Float,
            dy: Float
        ) {

            val screenWidth =
                width.toFloat()

            val screenHeight =
                height.toFloat()

            if (
                screenWidth <= 0f ||
                screenHeight <= 0f
            ) {
                return
            }

            val dxPercent =
                (
                    dx /
                            screenWidth
                ) * 100f

            val dyPercent =
                (
                    dy /
                            screenHeight
                ) * 100f

            data.x =
                clamp(
                    originalX +
                            dxPercent,
                    0f,
                    100f
                )

            data.y =
                clamp(
                    originalY +
                            dyPercent,
                    0f,
                    100f
                )

            applyPosition(
                data
            )
        }

        // ========================================================
        // APPLY ALL POSITIONS
        // ========================================================

        private fun applyPositions() {

            for (data in buttons) {

                applyPosition(
                    data
                )
            }

            applyToolbarPosition()
        }

        // ========================================================
        // APPLY INDIVIDUAL BUTTON POSITION
        // ========================================================

        private fun applyPosition(
            data: ButtonData
        ) {

            val button =
                buttonViews[data.id]
                    ?: return

            if (
                width <= 0 ||
                height <= 0
            ) {
                return
            }

            val density =
                resources.displayMetrics.density

            val widthPx =
                (
                    data.width *
                            density *
                            controllerScale
                    ).toInt()

            val heightPx =
                (
                    data.height *
                            density *
                            controllerScale
                    ).toInt()

            val left =
                (
                    width *
                            data.x /
                            100f
                    ).toInt()

            val top =
                (
                    height *
                            data.y /
                            100f
                    ).toInt()

            val params =
                button.layoutParams
                    as? FrameLayout.LayoutParams
                    ?: FrameLayout.LayoutParams(
                        widthPx,
                        heightPx
                    )

            params.width =
                widthPx

            params.height =
                heightPx

            params.leftMargin =
                left

            params.topMargin =
                top

            button.layoutParams =
                params
        }

        // ========================================================
        // CREATE EDIT TOOLBAR
        // ========================================================

        private fun createEditToolbar() {

            val newToolbar =
                FrameLayout(
                    this@MainActivity
                )

            toolbar =
                newToolbar

            newToolbar.tag =
                "controller_toolbar"

            addView(
                newToolbar
            )

            editButton =
                createToolbarButton(
                    if (editMode) {
                        "DONE"
                    } else {
                        "EDIT"
                    }
                )

            val resetButton =
                createToolbarButton(
                    "RESET"
                )

            val minusButton =
                createToolbarButton(
                    "−"
                )

            scaleLabel =
                createToolbarButton(
                    String.format(
                        Locale.US,
                        "%.1fx",
                        controllerScale
                    )
                )

            val plusButton =
                createToolbarButton(
                    "+"
                )

            newToolbar.addView(
                editButton
            )

            newToolbar.addView(
                resetButton
            )

            newToolbar.addView(
                minusButton
            )

            newToolbar.addView(
                scaleLabel
            )

            newToolbar.addView(
                plusButton
            )

            editButton?.setOnClickListener {

                editMode =
                    !editMode

                prefs.edit()
                    .putBoolean(
                        EDIT_MODE,
                        editMode
                    )
                    .apply()

                editButton?.text =
                    if (editMode) {
                        "DONE"
                    } else {
                        "EDIT"
                    }

                updateEditAppearance()
            }

            resetButton.setOnClickListener {

                resetController()
            }

            minusButton.setOnClickListener {

                controllerScale =
                    clamp(
                        controllerScale - 0.1f,
                        0.6f,
                        1.8f
                    )

                saveScale()

                updateScaleLabel()

                applyPositions()
            }

            plusButton.setOnClickListener {

                controllerScale =
                    clamp(
                        controllerScale + 0.1f,
                        0.6f,
                        1.8f
                    )

                saveScale()

                updateScaleLabel()

                applyPositions()
            }

            updateEditAppearance()

            post {

                applyToolbarPosition()
            }
        }

        // ========================================================
        // FIXED TOOLBAR POSITION FUNCTION
        //
        // This was the second compile error:
        // "Unresolved reference: applyToolbarPosition"
        //
        // The toolbar is always centered horizontally at the top.
        // ========================================================

        private fun applyToolbarPosition() {

            val currentToolbar =
                toolbar
                    ?: return

            if (
                width <= 0 ||
                height <= 0
            ) {
                return
            }

            val params =
                currentToolbar.layoutParams
                    as? FrameLayout.LayoutParams
                    ?: FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(36)
                    )

            params.width =
                ViewGroup.LayoutParams.WRAP_CONTENT

            params.height =
                ViewGroup.LayoutParams.WRAP_CONTENT

            params.gravity =
                Gravity.TOP or
                        Gravity.CENTER_HORIZONTAL

            params.topMargin =
                dp(8)

            params.leftMargin =
                0

            params.rightMargin =
                0

            currentToolbar.layoutParams =
                params

            currentToolbar.measure(
                MeasureSpec.makeMeasureSpec(
                    width,
                    MeasureSpec.AT_MOST
                ),
                MeasureSpec.makeMeasureSpec(
                    height,
                    MeasureSpec.AT_MOST
                )
            )

            currentToolbar.requestLayout()
        }

        // ========================================================
        // TOOLBAR BUTTON
        // ========================================================

        private fun createToolbarButton(
            text: String
        ): TextView {

            val button =
                TextView(
                    this@MainActivity
                )

            button.text =
                text

            button.gravity =
                Gravity.CENTER

            button.setTextColor(
                Color.WHITE
            )

            button.textSize =
                11f

            button.setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(5)
            )

            button.setBackground(
                createToolbarBackground()
            )

            val params =
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(36)
                )

            params.leftMargin =
                dp(2)

            params.rightMargin =
                dp(2)

            button.layoutParams =
                params

            return button
        }

        // ========================================================
        // EDIT MODE APPEARANCE
        // ========================================================

        private fun updateEditAppearance() {

            for (data in buttons) {

                val button =
                    buttonViews[data.id]
                        ?: continue

                if (editMode) {

                    button.setBackground(
                        createEditBackground()
                    )

                } else {

                    button.setBackground(
                        createButtonBackground()
                    )
                }
            }
        }

        // ========================================================
        // RESET CONTROLLER
        // ========================================================

        private fun resetController() {

            prefs.edit()
                .clear()
                .apply()

            controllerScale =
                1.0f

            editMode =
                false

            createButtonData()

            buildController()

            loadSavedPositions()

            applyPositions()
        }

        // ========================================================
        // SAVE BUTTON POSITION
        // ========================================================

        private fun saveButton(
            data: ButtonData
        ) {

            prefs.edit()
                .putFloat(
                    data.id + "_x",
                    data.x
                )
                .putFloat(
                    data.id + "_y",
                    data.y
                )
                .apply()
        }

        // ========================================================
        // SAVE SCALE
        // ========================================================

        private fun saveScale() {

            prefs.edit()
                .putFloat(
                    CONTROLLER_SCALE,
                    controllerScale
                )
                .apply()
        }

        // ========================================================
        // UPDATE SCALE LABEL
        // ========================================================

        private fun updateScaleLabel() {

            scaleLabel?.text =
                String.format(
                    Locale.US,
                    "%.1fx",
                    controllerScale
                )
        }

        // ========================================================
        // LOAD SAVED POSITIONS
        // ========================================================

        private fun loadSavedPositions() {

            for (data in buttons) {

                data.x =
                    prefs.getFloat(
                        data.id + "_x",
                        data.x
                    )

                data.y =
                    prefs.getFloat(
                        data.id + "_y",
                        data.y
                    )
            }

            controllerScale =
                prefs.getFloat(
                    CONTROLLER_SCALE,
                    controllerScale
                )

            editMode =
                prefs.getBoolean(
                    EDIT_MODE,
                    editMode
                )

            editButton?.text =
                if (editMode) {
                    "DONE"
                } else {
                    "EDIT"
                }

            updateScaleLabel()
        }

        // ========================================================
        // PRESS KEY
        // ========================================================

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

        // ========================================================
        // RELEASE KEY
        // ========================================================

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

        // ========================================================
        // RELEASE ALL KEYS
        // ========================================================

        fun releaseAllKeys() {

            val copy =
                activeKeys.toList()

            activeKeys.clear()

            attackDown =
                false

            for (keyCode in copy) {

                sendKeyToGame(
                    keyCode,
                    false
                )
            }
        }

        // ========================================================
        // NORMAL BUTTON BACKGROUND
        // ========================================================

        private fun createButtonBackground():
                GradientDrawable {

            return GradientDrawable().apply {

                setColor(
                    Color.argb(
                        165,
                        25,
                        25,
                        25
                    )
                )

                cornerRadius =
                    dp(12).toFloat()

                setStroke(
                    dp(1),
                    Color.argb(
                        180,
                        255,
                        255,
                        255
                    )
                )
            }
        }

        // ========================================================
        // EDIT BACKGROUND
        // ========================================================

        private fun createEditBackground():
                GradientDrawable {

            return GradientDrawable().apply {

                setColor(
                    Color.argb(
                        205,
                        55,
                        90,
                        170
                    )
                )

                cornerRadius =
                    dp(12).toFloat()

                setStroke(
                    dp(2),
                    Color.WHITE
                )
            }
        }

        // ========================================================
        // TOOLBAR BACKGROUND
        // ========================================================

        private fun createToolbarBackground():
                GradientDrawable {

            return GradientDrawable().apply {

                setColor(
                    Color.argb(
                        210,
                        20,
                        20,
                        20
                    )
                )

                cornerRadius =
                    dp(9).toFloat()

                setStroke(
                    dp(1),
                    Color.argb(
                        170,
                        255,
                        255,
                        255
                    )
                )
            }
        }

        // ========================================================
        // DP
        // ========================================================

        private fun dp(
            value: Int
        ): Int {

            return (
                value *
                        resources.displayMetrics.density
                ).toInt()
        }

        // ========================================================
        // CLAMP
        // ========================================================

        private fun clamp(
            value: Float,
            minValue: Float,
            maxValue: Float
        ): Float {

            return max(
                minValue,
                min(
                    maxValue,
                    value
                )
            )
        }

        // ========================================================
        // RESIZE / ROTATION
        // ========================================================

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

            if (
                w > 0 &&
                h > 0
            ) {

                post {

                    applyPositions()

                    applyToolbarPosition()
                }
            }
        }
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

        // ========================================================
        // GAME WEBVIEW
        // ========================================================

        gameWebView =
            WebView(this)

        gameWebView.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        gameWebView.isFocusable =
            true

        gameWebView.isFocusableInTouchMode =
            true

        with(gameWebView.settings) {

            javaScriptEnabled =
                true

            domStorageEnabled =
                true

            databaseEnabled =
                true

            mediaPlaybackRequiresUserGesture =
                false

            useWideViewPort =
                true

            loadWithOverviewMode =
                true

            builtInZoomControls =
                false

            displayZoomControls =
                false

            setSupportZoom(
                false
            )

            allowFileAccess =
                true

            allowContentAccess =
                true

            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) " +
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

                    controller.bringToFront()
                }
            }

        gameWebView.addJavascriptInterface(
            AndroidBridge(),
            "AndroidKeys"
        )

        rootLayout.addView(
            gameWebView
        )

        // ========================================================
        // CONTROLLER
        //
        // Controller is added AFTER WebView, so it is above it.
        // ========================================================

        controller =
            ControllerOverlay(
                this
            )

        rootLayout.addView(
            controller,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        controller.bringToFront()

        gameWebView.loadUrl(
            GAME_URL
        )

        enterImmersive()
    }

    // ============================================================
    // IMMERSIVE MODE
    // ============================================================

    @Suppress("DEPRECATION")
    private fun enterImmersive() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        controller.bringToFront()
    }

    @Suppress("DEPRECATION")
    private fun exitImmersive() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
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

            enterImmersive()

            controller.bringToFront()

        } else {

            controller.releaseAllKeys()
        }
    }

    // ============================================================
    // RESUME
    // ============================================================

    override fun onResume() {

        super.onResume()

        gameWebView.onResume()

        controller.visibility =
            View.VISIBLE

        controller.bringToFront()

        enterImmersive()
    }

    // ============================================================
    // PAUSE
    // ============================================================

    override fun onPause() {

        controller.releaseAllKeys()

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

        if (
            fullscreenView != null
        ) {

            exitFullscreen()

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

    // ============================================================
    // DESTROY
    // ============================================================

    override fun onDestroy() {

        controller.releaseAllKeys()

        try {

            fullscreenCallback?.onCustomViewHidden()

        } catch (_: Exception) {
        }

        gameWebView.stopLoading()

        gameWebView.destroy()

        super.onDestroy()
    }

    // ============================================================
    // ESCAPE JAVASCRIPT STRING
    // ============================================================

    private fun escapeJs(
        value: String
    ): String {

        return value
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
            )
    }
}
