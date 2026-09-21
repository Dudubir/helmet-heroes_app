(function () {

    if (window.__vpad) return;
    window.__vpad = true;

    // ============================================================
    // STORAGE
    // ============================================================

    var STORE = 'vpad_layout_v3';
    var SET_STORE = 'vpad_settings_v3';
    var HIDE_STORE = 'vpad_hidden_v3';

    // ============================================================
    // KEY TABLE
    // ============================================================

    var SPECIAL = {

        'Space': {
            key: ' ',
            code: 'Space',
            kc: 32
        },

        'Enter': {
            key: 'Enter',
            code: 'Enter',
            kc: 13
        },

        'Escape': {
            key: 'Escape',
            code: 'Escape',
            kc: 27
        },

        'Tab': {
            key: 'Tab',
            code: 'Tab',
            kc: 9
        },

        'Backspace': {
            key: 'Backspace',
            code: 'Backspace',
            kc: 8
        },

        'Shift': {
            key: 'Shift',
            code: 'ShiftLeft',
            kc: 16
        },

        'Control': {
            key: 'Control',
            code: 'ControlLeft',
            kc: 17
        },

        'Alt': {
            key: 'Alt',
            code: 'AltLeft',
            kc: 18
        },

        'ArrowUp': {
            key: 'ArrowUp',
            code: 'ArrowUp',
            kc: 38
        },

        'ArrowDown': {
            key: 'ArrowDown',
            code: 'ArrowDown',
            kc: 40
        },

        'ArrowLeft': {
            key: 'ArrowLeft',
            code: 'ArrowLeft',
            kc: 37
        },

        'ArrowRight': {
            key: 'ArrowRight',
            code: 'ArrowRight',
            kc: 39
        }
    };

    for (var f = 1; f <= 12; f++) {

        SPECIAL['F' + f] = {
            key: 'F' + f,
            code: 'F' + f,
            kc: 111 + f
        };
    }

    // ============================================================
    // KEY INFORMATION
    // ============================================================

    function keyInfo(name) {

        if (!name) return null;

        if (SPECIAL[name]) {
            return SPECIAL[name];
        }

        if (name.length === 1) {

            var up = name.toUpperCase();

            if (
                up >= 'A' &&
                up <= 'Z'
            ) {

                return {
                    key: name.toLowerCase(),
                    code: 'Key' + up,
                    kc: up.charCodeAt(0)
                };
            }

            if (
                up >= '0' &&
                up <= '9'
            ) {

                return {
                    key: up,
                    code: 'Digit' + up,
                    kc: up.charCodeAt(0)
                };
            }
        }

        return null;
    }

    var KEY_NAMES =
        Object.keys(SPECIAL)
            .concat(
                'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
                    .split('')
            );

    // ============================================================
    // FALLBACK JAVASCRIPT KEY EVENT
    // ============================================================

    function fallbackFire(
        type,
        info
    ) {

        var ev =
            new KeyboardEvent(
                type,
                {
                    key: info.key,
                    code: info.code,
                    keyCode: info.kc,
                    which: info.kc,
                    bubbles: true,
                    cancelable: true,
                    view: window
                }
            );

        try {

            Object.defineProperty(
                ev,
                'keyCode',
                {
                    get: function () {
                        return info.kc;
                    }
                }
            );

            Object.defineProperty(
                ev,
                'which',
                {
                    get: function () {
                        return info.kc;
                    }
                }
            );

        } catch (e) {}

        window.dispatchEvent(ev);
        document.dispatchEvent(ev);

        var t =
            document.activeElement;

        if (
            !t ||
            t === document.body ||
            t === document.documentElement
        ) {

            t =
                document.querySelector('canvas') ||
                document;
        }

        try {
            t.dispatchEvent(ev);
        } catch (e) {}
    }

    // ============================================================
    // SEND KEY TO ANDROID
    // ============================================================

    function sendKey(
        name,
        down
    ) {

        var info =
            keyInfo(name);

        if (!info) return;

        // --------------------------------------------------------
        // Stop this transparent controller WebView from stealing
        // keyboard focus.
        // --------------------------------------------------------

        try {
            if (
                window.AndroidKeys &&
                typeof window.AndroidKeys.key === 'function'
            ) {

                window.AndroidKeys.key(
                    name,
                    down
                );
            }

        } catch (e) {}

        // --------------------------------------------------------
        // JS fallback as additional compatibility.
        // --------------------------------------------------------

        fallbackFire(
            down ? 'keydown' : 'keyup',
            info
        );
    }

    // ============================================================
    // DEFAULT CONTROLLER
    // ============================================================

    var DEFAULT = [

        {
            id: 1,
            label: 'W',
            key: 'W',
            x: 15,
            y: 58,
            size: 56
        },

        {
            id: 2,
            label: 'A',
            key: 'A',
            x: 8,
            y: 76,
            size: 56
        },

        {
            id: 3,
            label: 'S',
            key: 'S',
            x: 15,
            y: 94,
            size: 56
        },

        {
            id: 4,
            label: 'D',
            key: 'D',
            x: 22,
            y: 76,
            size: 56
        },

        {
            id: 5,
            label: 'ATK',
            key: 'Space',
            x: 90,
            y: 80,
            size: 76
        },

        {
            id: 6,
            label: 'E',
            key: 'E',
            x: 76,
            y: 86,
            size: 50
        },

        {
            id: 7,
            label: 'M',
            key: 'M',
            x: 68,
            y: 64,
            size: 54
        },

        {
            id: 8,
            label: 'N',
            key: 'N',
            x: 78,
            y: 58,
            size: 54
        },

        {
            id: 9,
            label: 'B',
            key: 'B',
            x: 88,
            y: 56,
            size: 54
        },

        {
            id: 10,
            label: 'ESC',
            key: 'Escape',
            x: 94,
            y: 30,
            size: 44
        }
    ];

    // ============================================================
    // LOAD SETTINGS
    // ============================================================

    var layout;
    var settings;
    var hidden = false;

    try {
        layout =
            JSON.parse(
                localStorage.getItem(STORE)
            );
    } catch (e) {}

    if (!Array.isArray(layout)) {

        layout =
            JSON.parse(
                JSON.stringify(DEFAULT)
            );
    }

    try {

        settings =
            JSON.parse(
                localStorage.getItem(SET_STORE)
            );

    } catch (e) {}

    if (
        !settings ||
        typeof settings.opacity !== 'number'
    ) {

        settings = {
            opacity: 0.6,
            scale: 1
        };
    }

    try {

        hidden =
            localStorage.getItem(
                HIDE_STORE
            ) === '1';

    } catch (e) {}

    var editing = false;
    var selectedId = null;

    // ============================================================
    // SAVE
    // ============================================================

    function save() {

        try {

            localStorage.setItem(
                STORE,
                JSON.stringify(layout)
            );

        } catch (e) {}
    }

    function saveSettings() {

        try {

            localStorage.setItem(
                SET_STORE,
                JSON.stringify(settings)
            );

        } catch (e) {}
    }

    function effOpacity(b) {

        return typeof b.opacity === 'number'
            ? b.opacity
            : settings.opacity;
    }

    // ============================================================
    // STYLE
    // ============================================================

    var st =
        document.createElement('style');

    st.textContent =

        '#vpad-root{' +
            'position:fixed;' +
            'left:0;' +
            'top:0;' +
            'right:0;' +
            'bottom:0;' +
            'width:100vw;' +
            'height:100vh;' +
            'z-index:2147483647;' +
            'pointer-events:none;' +
            'font-family:sans-serif;' +
            '-webkit-user-select:none;' +
            'user-select:none;' +
            '-webkit-touch-callout:none;' +
            'overflow:hidden;' +
        '}' +

        '#vpad-root *{' +
            '-webkit-tap-highlight-color:transparent;' +
            'box-sizing:border-box;' +
        '}' +

        '.vpad-btn{' +
            'position:absolute;' +
            'transform:translate(-50%,-50%);' +
            'border-radius:50%;' +
            'pointer-events:auto;' +
            'touch-action:none;' +
            'display:flex;' +
            'align-items:center;' +
            'justify-content:center;' +
            'color:#fff;' +
            'font-weight:bold;' +
            'background:rgba(30,30,30,.7);' +
            'border:2px solid rgba(255,255,255,.8);' +
            'text-align:center;' +
            'overflow:hidden;' +
        '}' +

        '.vpad-btn.down{' +
            'background:rgba(255,255,255,.85);' +
            'color:#000;' +
        '}' +

        '.vpad-btn.edit{' +
            'border:2px dashed #ffd400;' +
        '}' +

        '.vpad-btn.sel{' +
            'border:3px solid #00e5ff;' +
        '}' +

        '.vpad-bar{' +
            'position:absolute;' +
            'top:6px;' +
            'right:6px;' +
            'display:flex;' +
            'gap:6px;' +
            'pointer-events:none;' +
        '}' +

        '.vpad-tool{' +
            'pointer-events:auto;' +
            'background:rgba(0,0,0,.65);' +
            'color:#fff;' +
            'border:1px solid #fff;' +
            'border-radius:8px;' +
            'padding:8px 12px;' +
            'font-size:14px;' +
            'touch-action:manipulation;' +
        '}' +

        '#vpad-panel{' +
            'pointer-events:auto;' +
            'position:absolute;' +
            'left:50%;' +
            'top:50%;' +
            'transform:translate(-50%,-50%);' +
            'background:rgba(20,20,20,.96);' +
            'color:#fff;' +
            'border:1px solid #888;' +
            'border-radius:10px;' +
            'padding:12px;' +
            'width:280px;' +
            'max-width:92%;' +
            'max-height:88%;' +
            'overflow-y:auto;' +
            'font-size:14px;' +
        '}' +

        '#vpad-panel label{' +
            'display:block;' +
            'margin:8px 0 2px;' +
        '}' +

        '#vpad-panel input[type=text]{' +
            'width:100%;' +
            'padding:6px;' +
            'font-size:16px;' +
            'border-radius:6px;' +
            'border:1px solid #666;' +
            'background:#222;' +
            'color:#fff;' +
        '}' +

        '#vpad-panel input[type=range]{' +
            'width:100%;' +
        '}' +

        '#vpad-panel button{' +
            'margin:10px 6px 0 0;' +
            'padding:8px 12px;' +
            'border-radius:6px;' +
            'border:1px solid #888;' +
            'background:#333;' +
            'color:#fff;' +
            'font-size:14px;' +
        '}';

    document.documentElement.appendChild(st);

    // ============================================================
    // ROOT
    // ============================================================

    var root =
        document.createElement('div');

    root.id = 'vpad-root';

    document.documentElement.appendChild(root);

    var btnLayer =
        document.createElement('div');

    root.appendChild(btnLayer);

    var toolLayer =
        document.createElement('div');

    root.appendChild(toolLayer);

    // ============================================================
    // BUTTON RENDER
    // ============================================================

    function render() {

        btnLayer.innerHTML = '';

        if (
            hidden &&
            !editing
        ) {
            return;
        }

        layout.forEach(function (b) {

            var el =
                document.createElement('div');

            el.className =
                'vpad-btn' +
                (
                    editing
                        ? ' edit'
                        : ''
                ) +
                (
                    editing &&
                    b.id === selectedId
                        ? ' sel'
                        : ''
                );

            el.textContent =
                b.label;

            var px =
                Math.round(
                    b.size *
                    settings.scale
                );

            el.style.left =
                b.x + '%';

            el.style.top =
                b.y + '%';

            el.style.width =
                px + 'px';

            el.style.height =
                px + 'px';

            el.style.fontSize =
                Math.max(
                    10,
                    Math.round(
                        px * 0.3
                    )
                ) + 'px';

            el.style.opacity =
                editing
                    ? Math.max(
                        0.5,
                        effOpacity(b)
                    )
                    : effOpacity(b);

            attach(
                el,
                b
            );

            btnLayer.appendChild(el);
        });
    }

    // ============================================================
    // BUTTON TOUCH
    // ============================================================

    function attach(
        el,
        b
    ) {

        var dragging = false;
        var moved = false;

        var sx = 0;
        var sy = 0;

        var pressed = false;

        el.addEventListener(
            'pointerdown',
            function (e) {

                e.preventDefault();
                e.stopPropagation();

                try {
                    el.setPointerCapture(
                        e.pointerId
                    );
                } catch (x) {}

                // ------------------------------------------------
                // EDIT MODE
                // ------------------------------------------------

                if (editing) {

                    dragging = true;
                    moved = false;

                    sx = e.clientX;
                    sy = e.clientY;

                    return;
                }

                // ------------------------------------------------
                // GAME MODE
                // ------------------------------------------------

                if (!pressed) {

                    pressed = true;

                    el.classList.add(
                        'down'
                    );

                    // Tell Android to send a real keyboard key.
                    sendKey(
                        b.key,
                        true
                    );
                }
            },
            {
                passive: false
            }
        );

        el.addEventListener(
            'pointermove',
            function (e) {

                if (
                    !editing ||
                    !dragging
                ) {
                    return;
                }

                if (
                    Math.abs(
                        e.clientX - sx
                    ) +
                    Math.abs(
                        e.clientY - sy
                    ) > 6
                ) {

                    moved = true;
                }

                if (moved) {

                    b.x =
                        Math.max(
                            2,
                            Math.min(
                                98,
                                e.clientX /
                                window.innerWidth *
                                100
                            )
                        );

                    b.y =
                        Math.max(
                            2,
                            Math.min(
                                98,
                                e.clientY /
                                window.innerHeight *
                                100
                            )
                        );

                    el.style.left =
                        b.x + '%';

                    el.style.top =
                        b.y + '%';
                }
            }
        );

        function up(e) {

            e.preventDefault();
            e.stopPropagation();

            if (editing) {

                if (dragging) {

                    dragging = false;

                    if (moved) {

                        save();

                    } else {

                        selectedId =
                            b.id;

                        render();

                        openPanel(b);
                    }
                }

                return;
            }

            if (pressed) {

                pressed = false;

                el.classList.remove(
                    'down'
                );

                sendKey(
                    b.key,
                    false
                );
            }
        }

        el.addEventListener(
            'pointerup',
            up,
            {
                passive: false
            }
        );

        el.addEventListener(
            'pointercancel',
            up,
            {
                passive: false
            }
        );

        el.addEventListener(
            'pointerleave',
            function () {

                // Do NOT release the key just because
                // the finger temporarily leaves the button.
                //
                // This makes W/A/S/D work much better
                // for continuous movement.
            }
        );
    }

    // ============================================================
    // PANEL
    // ============================================================

    function closePanel() {

        var p =
            document.getElementById(
                'vpad-panel'
            );

        if (p) {
            p.remove();
        }
    }

    function newPanel() {

        closePanel();

        var p =
            document.createElement('div');

        p.id =
            'vpad-panel';

        p.addEventListener(
            'pointerdown',
            function (e) {
                e.stopPropagation();
            }
        );

        root.appendChild(p);

        return p;
    }

    // ============================================================
    // SLIDER
    // ============================================================

    function addSlider(
        p,
        text,
        min,
        max,
        val,
        fmt,
        onInput
    ) {

        var lab =
            document.createElement(
                'label'
            );

        var span =
            document.createElement(
                'span'
            );

        span.textContent =
            fmt(val);

        lab.textContent =
            text + ': ';

        lab.appendChild(span);

        var s =
            document.createElement(
                'input'
            );

        s.type = 'range';

        s.min = min;
        s.max = max;
        s.value = val;

        s.oninput =
            function () {

                var v =
                    parseInt(
                        s.value,
                        10
                    );

                span.textContent =
                    fmt(v);

                onInput(v);
            };

        p.appendChild(lab);
        p.appendChild(s);
    }

    // ============================================================
    // PANEL BUTTON
    // ============================================================

    function addButton(
        p,
        text,
        fn
    ) {

        var bt =
            document.createElement(
                'button'
            );

        bt.textContent =
            text;

        bt.onclick =
            fn;

        p.appendChild(bt);
    }

    // ============================================================
    // EDIT BUTTON
    // ============================================================

    function openPanel(b) {

        var p =
            newPanel();

        var title =
            document.createElement(
                'b'
            );

        title.textContent =
            'I-edit ang button';

        p.appendChild(title);

        var l1 =
            document.createElement(
                'label'
            );

        l1.textContent =
            'Label (text sa button)';

        p.appendChild(l1);

        var inLabel =
            document.createElement(
                'input'
            );

        inLabel.type = 'text';
        inLabel.value = b.label;

        p.appendChild(inLabel);

        var l2 =
            document.createElement(
                'label'
            );

        l2.textContent =
            'Key (A-Z, 0-9, Space, Enter, Shift, ArrowUp, F1...)';

        p.appendChild(l2);

        var inKey =
            document.createElement(
                'input'
            );

        inKey.type = 'text';
        inKey.value = b.key;

        inKey.setAttribute(
            'list',
            'vp-keys'
        );

        inKey.setAttribute(
            'autocapitalize',
            'off'
        );

        p.appendChild(inKey);

        var dl =
            document.createElement(
                'datalist'
            );

        dl.id =
            'vp-keys';

        KEY_NAMES.forEach(
            function (k) {

                var o =
                    document.createElement(
                        'option'
                    );

                o.value = k;

                dl.appendChild(o);
            }
        );

        p.appendChild(dl);

        inLabel.oninput =
            function () {

                b.label =
                    inLabel.value;

                save();

                render();
            };

        inKey.oninput =
            function () {

                var v =
                    inKey.value.length === 1
                        ? inKey.value.toUpperCase()
                        : inKey.value;

                if (keyInfo(v)) {

                    b.key = v;

                    inKey.style.borderColor =
                        '#666';

                    save();

                } else {

                    inKey.style.borderColor =
                        '#f44';
                }
            };

        addSlider(
            p,
            'Laki ng button',
            30,
            160,
            b.size,
            function (v) {
                return v + 'px';
            },
            function (v) {

                b.size = v;

                save();

                render();
            }
        );

        addSlider(
            p,
            'Opacity ng button',
            10,
            100,
            Math.round(
                effOpacity(b) *
                100
            ),
            function (v) {
                return v + '%';
            },
            function (v) {

                b.opacity =
                    v / 100;

                save();

                render();
            }
        );

        addButton(
            p,
            'Delete',
            function () {

                layout =
                    layout.filter(
                        function (x) {
                            return x.id !== b.id;
                        }
                    );

                selectedId = null;

                save();

                closePanel();

                render();
            }
        );

        addButton(
            p,
            'OK',
            function () {

                closePanel();

                render();
            }
        );
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    function openSettings() {

        var p =
            newPanel();

        var title =
            document.createElement(
                'b'
            );

        title.textContent =
            'Settings (lahat ng buttons)';

        p.appendChild(title);

        addSlider(
            p,
            'Opacity',
            10,
            100,
            Math.round(
                settings.opacity *
                100
            ),
            function (v) {
                return v + '%';
            },
            function (v) {

                settings.opacity =
                    v / 100;

                layout.forEach(
                    function (b) {
                        delete b.opacity;
                    }
                );

                saveSettings();
                save();

                render();
            }
        );

        addSlider(
            p,
            'Laki',
            50,
            200,
            Math.round(
                settings.scale *
                100
            ),
            function (v) {
                return v + '%';
            },
            function (v) {

                settings.scale =
                    v / 100;

                saveSettings();

                render();
            }
        );

        addButton(
            p,
            'OK',
            function () {

                closePanel();
            }
        );
    }

    // ============================================================
    // TOOLBAR
    // ============================================================

    function renderTools() {

        toolLayer.innerHTML = '';

        var bar =
            document.createElement(
                'div'
            );

        bar.className =
            'vpad-bar';

        toolLayer.appendChild(bar);

        function tool(
            text,
            fn,
            opts
        ) {

            var t =
                document.createElement(
                    'div'
                );

            t.className =
                'vpad-tool';

            t.textContent =
                text;

            if (
                opts &&
                opts.opacity
            ) {

                t.style.opacity =
                    opts.opacity;
            }

            if (
                opts &&
                opts.bg
            ) {

                t.style.background =
                    opts.bg;
            }

            t.addEventListener(
                'pointerdown',
                function (e) {
                    e.preventDefault();
                    e.stopPropagation();
                },
                {
                    passive: false
                }
            );

            t.addEventListener(
                'click',
                function (e) {

                    e.preventDefault();
                    e.stopPropagation();

                    fn();
                }
            );

            bar.appendChild(t);
        }

        // --------------------------------------------------------
        // NORMAL MODE
        // --------------------------------------------------------

        if (!editing) {

            // This Full button now asks Android/WebView
            // to use the REAL fullscreen API.
            tool(
                'Full',
                function () {

                    try {

                        // Ask the GAME WebView to request fullscreen.
                        //
                        // The controller itself is separate from
                        // the game, so its own document is not used.

                        window.AndroidKeys.key(
                            '__REQUEST_GAME_FULLSCREEN__',
                            true
                        );

                    } catch (e) {}
                },
                {
                    opacity: '.6'
                }
            );

            tool(
                hidden
                    ? '\uD83D\uDC41'
                    : '\uD83D\uDEAB',

                function () {

                    hidden =
                        !hidden;

                    try {

                        localStorage.setItem(
                            HIDE_STORE,
                            hidden
                                ? '1'
                                : '0'
                        );

                    } catch (e) {}

                    renderTools();
                    render();
                },
                {
                    opacity: '.6'
                }
            );

            tool(
                '\u2699',

                function () {

                    editing = true;

                    renderTools();
                    render();
                },
                {
                    opacity: '.6'
                }
            );

        } else {

            tool(
                'Done',

                function () {

                    editing = false;

                    selectedId = null;

                    closePanel();

                    renderTools();
                    render();
                },
                {
                    bg:
                        'rgba(0,140,60,.9)'
                }
            );

            tool(
                'Reset',

                function () {

                    if (
                        confirm(
                            'I-reset sa default ang layout at settings?'
                        )
                    ) {

                        layout =
                            JSON.parse(
                                JSON.stringify(
                                    DEFAULT
                                )
                            );

                        settings = {
                            opacity: 0.6,
                            scale: 1
                        };

                        save();
                        saveSettings();

                        closePanel();

                        render();
                    }
                }
            );

            tool(
                'Settings',

                function () {

                    openSettings();
                }
            );

            tool(
                '+ Add',

                function () {

                    var b = {

                        id:
                            Date.now(),

                        label: 'X',

                        key: 'X',

                        x: 50,

                        y: 50,

                        size: 60
                    };

                    layout.push(b);

                    selectedId =
                        b.id;

                    save();

                    render();

                    openPanel(b);
                }
            );
        }
    }

    // ============================================================
    // INITIALIZE
    // ============================================================

    renderTools();

    render();

})();
