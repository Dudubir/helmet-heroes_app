(function () {
  if (window.__vpad) return;
  window.__vpad = true;

  // ================= DEBUG MODE =================
  // Ilagay sa 'false' kapag tapos na tayo mag-debug. Habang 'true', may
  // lalabas na maliit na black log panel sa screen mismo na nagpapakita
  // ng bawat pindot at kung ano ang nangyari — walang kailangan na
  // kompyuter, USB debugging, o chrome://inspect.
  var DEBUG = true;
  var LOG_LINES = [];
  var logPanel = null;

  function dlog(msg) {
    var t = new Date().toISOString().substr(11, 8);
    LOG_LINES.push('[' + t + '] ' + msg);
    if (LOG_LINES.length > 60) LOG_LINES.shift();
    if (logPanel) logPanel.textContent = LOG_LINES.join('\n');
  }

  // Catch any error anywhere in the page and show it in the panel too —
  // kung may crash ang page o ibang script, makikita mo agad.
  window.addEventListener('error', function (e) {
    dlog('PAGE ERROR: ' + e.message + ' @ ' + e.filename + ':' + e.lineno);
  });

  // Global listener na nakikita ANG LAHAT ng keydown/keyup na dumarating sa
  // window, kahit saan pa galing — totoong keyboard, synthetic JS, o
  // Android native. Ito ang pinaka-importanteng linya: sinasabi nito kung
  // "totoo" (isTrusted:true) o "peke/JS-made" (isTrusted:false) ang event,
  // dahil maraming laro ang tumatanggi sa isTrusted:false na events.
  window.addEventListener('keydown', function (e) {
    dlog('WINDOW SAW keydown key=' + e.key + ' code=' + e.code + ' isTrusted=' + e.isTrusted);
  }, true);
  window.addEventListener('keyup', function (e) {
    dlog('WINDOW SAW keyup key=' + e.key + ' code=' + e.code + ' isTrusted=' + e.isTrusted);
  }, true);

  // Ito ang tatawagin ng Kotlin/Android side (via evaluateJavascript) para
  // ipakita rito mismo sa on-screen log kung na-deliver ba nang matagumpay
  // ang key event sa native/WebView layer — kahit walang adb o chrome
  // remote debugging.
  window.__vpadNativeLog = function (msg) { dlog('NATIVE: ' + msg); };

  var STORE = 'vpad_layout_v3', SET_STORE = 'vpad_settings_v3', HIDE_STORE = 'vpad_hidden_v3';

  // ---------- Key table (fallback kung walang Android bridge) ----------
  var SPECIAL = {
    'Space': { key: ' ', code: 'Space', kc: 32 },
    'Enter': { key: 'Enter', code: 'Enter', kc: 13 },
    'Escape': { key: 'Escape', code: 'Escape', kc: 27 },
    'Tab': { key: 'Tab', code: 'Tab', kc: 9 },
    'Backspace': { key: 'Backspace', code: 'Backspace', kc: 8 },
    'Shift': { key: 'Shift', code: 'ShiftLeft', kc: 16 },
    'Control': { key: 'Control', code: 'ControlLeft', kc: 17 },
    'Alt': { key: 'Alt', code: 'AltLeft', kc: 18 },
    'ArrowUp': { key: 'ArrowUp', code: 'ArrowUp', kc: 38 },
    'ArrowDown': { key: 'ArrowDown', code: 'ArrowDown', kc: 40 },
    'ArrowLeft': { key: 'ArrowLeft', code: 'ArrowLeft', kc: 37 },
    'ArrowRight': { key: 'ArrowRight', code: 'ArrowRight', kc: 39 }
  };
  for (var f = 1; f <= 12; f++) SPECIAL['F' + f] = { key: 'F' + f, code: 'F' + f, kc: 111 + f };

  function keyInfo(name) {
    if (!name) return null;
    if (SPECIAL[name]) return SPECIAL[name];
    if (name.length === 1) {
      var up = name.toUpperCase();
      if (up >= 'A' && up <= 'Z') return { key: name.toLowerCase(), code: 'Key' + up, kc: up.charCodeAt(0) };
      if (up >= '0' && up <= '9') return { key: up, code: 'Digit' + up, kc: up.charCodeAt(0) };
    }
    return null;
  }
  var KEY_NAMES = Object.keys(SPECIAL).concat('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'.split(''));

  // ---------- I-target ang #center na tag ng laro ----------
  var gameTarget = null;
  function findGameTarget() {
    var el = document.getElementById('center');
    if (el) return el;
    el = document.querySelector('.center');
    if (el) return el;
    var list = document.querySelectorAll('canvas,iframe,embed,object');
    var best = null, area = 0;
    for (var i = 0; i < list.length; i++) {
      var r = list[i].getBoundingClientRect();
      var a = r.width * r.height;
      if (a > area) { area = a; best = list[i]; }
    }
    return best || document.body;
  }

  function describeEl(el) {
    if (!el) return 'null';
    var r = el.getBoundingClientRect();
    return el.tagName + (el.id ? '#' + el.id : '') + (el.className ? '.' + String(el.className).replace(/\s+/g, '.') : '') +
      ' [' + Math.round(r.width) + 'x' + Math.round(r.height) + ']' +
      (el.tagName === 'IFRAME' ? ' src=' + (el.getAttribute('src') || '(none)') : '');
  }

  function dumpPageStructure() {
    dlog('--- PAGE STRUCTURE DUMP ---');
    dlog('#center exists: ' + !!document.getElementById('center'));
    dlog('.center exists: ' + !!document.querySelector('.center'));
    var list = document.querySelectorAll('canvas,iframe,embed,object');
    dlog('canvas/iframe/embed/object count: ' + list.length);
    for (var i = 0; i < list.length; i++) {
      dlog('  [' + i + '] ' + describeEl(list[i]));
    }
    dlog('--- END DUMP ---');
  }

  function focusGame() {
    gameTarget = findGameTarget();
    if (!gameTarget) { dlog('focusGame: NO TARGET FOUND'); return; }
    dlog('focusGame: target=' + describeEl(gameTarget));
    try { if (gameTarget.tabIndex == null || gameTarget.tabIndex < 0) gameTarget.tabIndex = 0; } catch (e) {}
    try { gameTarget.focus({ preventScroll: true }); } catch (e) { try { gameTarget.focus(); } catch (e2) {} }
    try {
      if (typeof window.AndroidKeys !== 'undefined' && window.AndroidKeys.ensureFocus) {
        window.AndroidKeys.ensureFocus();
      }
    } catch (e) {}
  }

  function fallbackFire(type, info) {
    var ev = new KeyboardEvent(type, {
      key: info.key, code: info.code, keyCode: info.kc, which: info.kc,
      bubbles: true, cancelable: true, view: window
    });
    try {
      Object.defineProperty(ev, 'keyCode', { get: function () { return info.kc; } });
      Object.defineProperty(ev, 'which', { get: function () { return info.kc; } });
    } catch (e) {}
    var t = gameTarget || document.activeElement || document;
    if (t && t.tagName === 'IFRAME') {
      try {
        if (t.contentDocument) t.contentDocument.dispatchEvent(ev);
        if (t.contentWindow) t.contentWindow.dispatchEvent(ev);
      } catch (e) {
        dlog('fallbackFire: iframe blocked (cross-origin): ' + e.message);
      }
    }
    t.dispatchEvent(ev);
    document.dispatchEvent(ev);
  }

  function sendKey(name, down) {
    var info = keyInfo(name);
    if (!info) { dlog('sendKey: unknown key name "' + name + '"'); return; }
    dlog('sendKey ' + name + ' down=' + down + ' hasAndroidKeys=' + (typeof window.AndroidKeys !== 'undefined'));
    if (down) focusGame();
    try {
      if (typeof window.AndroidKeys !== 'undefined') { window.AndroidKeys.key(name, down); }
    } catch (e) {
      dlog('AndroidKeys.key threw: ' + e.message);
    }
    fallbackFire(down ? 'keydown' : 'keyup', info);
  }

  // ---------- Fullscreen (CSS-based) ----------
  var fsEl = null, fsSavedStyle = null, fsSavedOverflow = '';
  function fsEvents() {
    ['fullscreenchange', 'webkitfullscreenchange'].forEach(function (n) { document.dispatchEvent(new Event(n)); });
    window.dispatchEvent(new Event('resize'));
  }
  function enterFs(el) {
    if (fsEl) exitFs();
    fsEl = el;
    fsSavedStyle = el.getAttribute('style');
    fsSavedOverflow = document.documentElement.style.overflow;
    el.style.cssText += ';position:fixed!important;left:0!important;top:0!important;' +
      'width:100vw!important;height:100vh!important;max-width:none!important;max-height:none!important;' +
      'margin:0!important;z-index:2147483000!important;background:#000!important;';
    document.documentElement.style.overflow = 'hidden';
    fsEvents();
  }
  function exitFs() {
    if (!fsEl) return;
    var el = fsEl; fsEl = null;
    if (fsSavedStyle === null) el.removeAttribute('style'); else el.setAttribute('style', fsSavedStyle);
    document.documentElement.style.overflow = fsSavedOverflow;
    fsEvents();
  }
  ['requestFullscreen', 'webkitRequestFullscreen', 'webkitRequestFullScreen', 'mozRequestFullScreen', 'msRequestFullscreen']
    .forEach(function (n) {
      try { Element.prototype[n] = function () { enterFs(this); return Promise.resolve(); }; } catch (e) {}
    });
  ['exitFullscreen', 'webkitExitFullscreen', 'webkitCancelFullScreen', 'mozCancelFullScreen', 'msExitFullscreen']
    .forEach(function (n) {
      try { document[n] = function () { exitFs(); return Promise.resolve(); }; } catch (e) {}
    });
  function defGetter(name, fn) {
    try { Object.defineProperty(document, name, { get: fn, configurable: true }); } catch (e) {}
  }
  ['fullscreenElement', 'webkitFullscreenElement', 'webkitCurrentFullScreenElement', 'mozFullScreenElement']
    .forEach(function (n) { defGetter(n, function () { return fsEl; }); });
  ['fullscreenEnabled', 'webkitFullscreenEnabled', 'mozFullScreenEnabled']
    .forEach(function (n) { defGetter(n, function () { return true; }); });
  ['webkitIsFullScreen', 'mozFullScreen']
    .forEach(function (n) { defGetter(n, function () { return !!fsEl; }); });

  function toggleFs() {
    if (fsEl) { exitFs(); return; }
    enterFs(findGameTarget());
  }

  // ---------- Data / storage ----------
  var DEFAULT = [
    { id: 1, label: 'W', key: 'W', x: 15, y: 50, size: 56 },
    { id: 2, label: 'A', key: 'A', x: 8, y: 68, size: 56 },
    { id: 3, label: 'S', key: 'S', x: 15, y: 86, size: 56 },
    { id: 4, label: 'D', key: 'D', x: 22, y: 68, size: 56 },
    { id: 5, label: 'ATK', key: 'Space', x: 90, y: 78, size: 76 },
    { id: 6, label: 'E', key: 'E', x: 76, y: 84, size: 50 },
    { id: 7, label: 'M', key: 'M', x: 68, y: 62, size: 54 },
    { id: 8, label: 'N', key: 'N', x: 78, y: 56, size: 54 },
    { id: 9, label: 'B', key: 'B', x: 88, y: 54, size: 54 },
    { id: 10, label: 'ESC', key: 'Escape', x: 94, y: 38, size: 44 }
  ];
  var layout, settings, hidden = false;
  try { layout = JSON.parse(localStorage.getItem(STORE)); } catch (e) {}
  if (!Array.isArray(layout)) layout = JSON.parse(JSON.stringify(DEFAULT));
  try { settings = JSON.parse(localStorage.getItem(SET_STORE)); } catch (e) {}
  if (!settings || typeof settings.opacity !== 'number') settings = { opacity: 0.6, scale: 1 };
  try { hidden = localStorage.getItem(HIDE_STORE) === '1'; } catch (e) {}
  var editing = false, selectedId = null;

  function save() { try { localStorage.setItem(STORE, JSON.stringify(layout)); } catch (e) {} }
  function saveSettings() { try { localStorage.setItem(SET_STORE, JSON.stringify(settings)); } catch (e) {} }
  function effOpacity(b) { return typeof b.opacity === 'number' ? b.opacity : settings.opacity; }

  // ---------- Styles ----------
  var st = document.createElement('style');
  st.textContent =
    '#vpad-root{position:fixed;left:0;top:0;right:0;bottom:0;z-index:2147483647;pointer-events:none;' +
    'font-family:sans-serif;-webkit-user-select:none;user-select:none;-webkit-touch-callout:none}' +
    '#vpad-root *{-webkit-tap-highlight-color:transparent;box-sizing:border-box}' +
    '.vpad-btn{position:absolute;transform:translate(-50%,-50%);border-radius:50%;pointer-events:auto;' +
    'touch-action:none;display:flex;align-items:center;justify-content:center;color:#fff;font-weight:bold;' +
    'background:rgba(30,30,30,.7);border:2px solid rgba(255,255,255,.8);text-align:center;overflow:hidden}' +
    '.vpad-btn.down{background:rgba(255,255,255,.85);color:#000}' +
    '.vpad-btn.edit{border:2px dashed #ffd400}' +
    '.vpad-btn.sel{border:3px solid #00e5ff}' +
    '.vpad-bar{position:absolute;top:6px;right:6px;display:flex;gap:6px;pointer-events:none}' +
    '.vpad-tool{pointer-events:auto;background:rgba(0,0,0,.65);color:#fff;border:1px solid #fff;' +
    'border-radius:8px;padding:8px 12px;font-size:14px;touch-action:manipulation}' +
    '#vpad-panel{pointer-events:auto;position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);' +
    'background:rgba(20,20,20,.96);color:#fff;border:1px solid #888;border-radius:10px;padding:12px;' +
    'width:280px;max-width:92%;max-height:88%;overflow-y:auto;font-size:14px}' +
    '#vpad-panel label{display:block;margin:8px 0 2px}' +
    '#vpad-panel input[type=text]{width:100%;padding:6px;font-size:16px;border-radius:6px;border:1px solid #666;background:#222;color:#fff}' +
    '#vpad-panel input[type=range]{width:100%}' +
    '#vpad-panel button{margin:10px 6px 0 0;padding:8px 12px;border-radius:6px;border:1px solid #888;background:#333;color:#fff;font-size:14px}' +
    '#vpad-log{pointer-events:auto;position:absolute;left:4px;bottom:4px;width:min(94vw,520px);height:32vh;' +
    'background:rgba(0,0,0,.85);color:#0f0;font:10px/1.3 monospace;padding:6px;overflow-y:auto;' +
    'white-space:pre-wrap;border:1px solid #0f0;border-radius:6px}' +
    '#vpad-log-bar{pointer-events:auto;position:absolute;left:4px;bottom:4px;display:flex;gap:6px}';
  document.documentElement.appendChild(st);

  var root = document.createElement('div');
  root.id = 'vpad-root';
  document.documentElement.appendChild(root);
  var btnLayer = document.createElement('div'); root.appendChild(btnLayer);
  var toolLayer = document.createElement('div'); root.appendChild(toolLayer);
  var debugLayer = document.createElement('div'); root.appendChild(debugLayer);

  setInterval(function () {
    if (!document.documentElement.contains(root) || root.nextSibling || root !== document.documentElement.lastChild) {
      document.documentElement.appendChild(root);
    }
  }, 1000);

  // ---------- Debug panel UI ----------
  var logVisible = DEBUG;
  function renderDebug() {
    debugLayer.innerHTML = '';
    if (!DEBUG) return;

    var barWrap = document.createElement('div');
    barWrap.id = 'vpad-log-bar';
    debugLayer.appendChild(barWrap);

    var toggleBtn = document.createElement('div');
    toggleBtn.className = 'vpad-tool';
    toggleBtn.textContent = logVisible ? 'Hide Log' : 'Show Log';
    toggleBtn.addEventListener('pointerdown', function (e) { e.stopPropagation(); });
    toggleBtn.addEventListener('click', function (e) {
      e.stopPropagation(); logVisible = !logVisible; renderDebug();
    });

    var testBtn = document.createElement('div');
    testBtn.className = 'vpad-tool';
    testBtn.textContent = 'TEST W KEY';
    testBtn.addEventListener('pointerdown', function (e) { e.stopPropagation(); });
    testBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      dlog('--- TEST: manually firing W down/up ---');
      sendKey('W', true);
      setTimeout(function () { sendKey('W', false); }, 150);
    });

    var clearBtn = document.createElement('div');
    clearBtn.className = 'vpad-tool';
    clearBtn.textContent = 'Clear';
    clearBtn.addEventListener('pointerdown', function (e) { e.stopPropagation(); });
    clearBtn.addEventListener('click', function (e) {
      e.stopPropagation(); LOG_LINES = []; if (logPanel) logPanel.textContent = '';
    });

    if (!logVisible) {
      barWrap.appendChild(toggleBtn);
      barWrap.appendChild(testBtn);
      return;
    }

    var panel = document.createElement('div');
    panel.id = 'vpad-log';
    panel.textContent = LOG_LINES.join('\n');
    logPanel = panel;
    debugLayer.appendChild(panel);

    barWrap.style.bottom = 'calc(32vh + 8px)';
    barWrap.appendChild(toggleBtn);
    barWrap.appendChild(testBtn);
    barWrap.appendChild(clearBtn);
  }

  // ---------- Buttons ----------
  function render() {
    btnLayer.innerHTML = '';
    if (hidden && !editing) return;
    layout.forEach(function (b) {
      var el = document.createElement('div');
      el.className = 'vpad-btn' + (editing ? ' edit' : '') + (editing && b.id === selectedId ? ' sel' : '');
      el.textContent = b.label;
      var px = Math.round(b.size * settings.scale);
      el.style.left = b.x + '%';
      el.style.top = b.y + '%';
      el.style.width = px + 'px';
      el.style.height = px + 'px';
      el.style.fontSize = Math.max(10, Math.round(px * 0.3)) + 'px';
      el.style.opacity = editing ? Math.max(0.5, effOpacity(b)) : effOpacity(b);
      attach(el, b);
      btnLayer.appendChild(el);
    });
  }

  function attach(el, b) {
    var dragging = false, moved = false, sx = 0, sy = 0, pressed = false;

    el.addEventListener('pointerdown', function (e) {
      e.preventDefault(); e.stopPropagation();
      try { el.setPointerCapture(e.pointerId); } catch (x) {}
      if (editing) {
        dragging = true; moved = false; sx = e.clientX; sy = e.clientY;
      } else if (!pressed) {
        pressed = true; el.classList.add('down'); sendKey(b.key, true);
      }
    });
    el.addEventListener('pointermove', function (e) {
      if (!editing || !dragging) return;
      if (Math.abs(e.clientX - sx) + Math.abs(e.clientY - sy) > 6) moved = true;
      if (moved) {
        b.x = Math.max(2, Math.min(98, e.clientX / window.innerWidth * 100));
        b.y = Math.max(2, Math.min(98, e.clientY / window.innerHeight * 100));
        el.style.left = b.x + '%';
        el.style.top = b.y + '%';
      }
    });
    function up(e) {
      e.preventDefault();
      if (editing) {
        if (dragging) {
          dragging = false;
          if (moved) { save(); } else { selectedId = b.id; render(); openPanel(b); }
        }
      } else if (pressed) {
        pressed = false; el.classList.remove('down'); sendKey(b.key, false);
      }
    }
    el.addEventListener('pointerup', up);
    el.addEventListener('pointercancel', up);
  }

  // ---------- Panels ----------
  function closePanel() { var p = document.getElementById('vpad-panel'); if (p) p.remove(); }

  function newPanel() {
    closePanel();
    var p = document.createElement('div');
    p.id = 'vpad-panel';
    p.addEventListener('pointerdown', function (e) { e.stopPropagation(); });
    root.appendChild(p);
    return p;
  }

  function addSlider(p, text, min, max, val, fmt, onInput) {
    var lab = document.createElement('label');
    var span = document.createElement('span');
    span.textContent = fmt(val);
    lab.textContent = text + ': ';
    lab.appendChild(span);
    var s = document.createElement('input');
    s.type = 'range'; s.min = min; s.max = max; s.value = val;
    s.oninput = function () { var v = parseInt(s.value, 10); span.textContent = fmt(v); onInput(v); };
    p.appendChild(lab); p.appendChild(s);
  }

  function addButton(p, text, fn) {
    var bt = document.createElement('button');
    bt.textContent = text; bt.onclick = fn; p.appendChild(bt);
  }

  function openPanel(b) {
    var p = newPanel();
    var title = document.createElement('b'); title.textContent = 'I-edit ang button'; p.appendChild(title);

    var l1 = document.createElement('label'); l1.textContent = 'Label (text sa button)'; p.appendChild(l1);
    var inLabel = document.createElement('input'); inLabel.type = 'text'; inLabel.value = b.label; p.appendChild(inLabel);

    var l2 = document.createElement('label'); l2.textContent = 'Key (A-Z, 0-9, Space, Enter, Shift, ArrowUp, F1...)'; p.appendChild(l2);
    var inKey = document.createElement('input'); inKey.type = 'text'; inKey.value = b.key;
    inKey.setAttribute('list', 'vp-keys'); inKey.setAttribute('autocapitalize', 'off'); p.appendChild(inKey);
    var dl = document.createElement('datalist'); dl.id = 'vp-keys';
    KEY_NAMES.forEach(function (k) { var o = document.createElement('option'); o.value = k; dl.appendChild(o); });
    p.appendChild(dl);

    inLabel.oninput = function () { b.label = inLabel.value; save(); render(); };
    inKey.oninput = function () {
      var v = inKey.value.length === 1 ? inKey.value.toUpperCase() : inKey.value;
      if (keyInfo(v)) { b.key = v; inKey.style.borderColor = '#666'; save(); }
      else inKey.style.borderColor = '#f44';
    };

    addSlider(p, 'Laki ng button', 30, 160, b.size, function (v) { return v + 'px'; },
      function (v) { b.size = v; save(); render(); });
    addSlider(p, 'Opacity ng button', 10, 100, Math.round(effOpacity(b) * 100), function (v) { return v + '%'; },
      function (v) { b.opacity = v / 100; save(); render(); });

    addButton(p, 'Delete', function () {
      layout = layout.filter(function (x) { return x.id !== b.id; });
      selectedId = null; save(); closePanel(); render();
    });
    addButton(p, 'OK', function () { closePanel(); render(); });
  }

  function openSettings() {
    var p = newPanel();
    var title = document.createElement('b'); title.textContent = 'Settings (lahat ng buttons)'; p.appendChild(title);
    addSlider(p, 'Opacity', 10, 100, Math.round(settings.opacity * 100), function (v) { return v + '%'; },
      function (v) {
        settings.opacity = v / 100;
        layout.forEach(function (b) { delete b.opacity; });
        saveSettings(); save(); render();
      });
    addSlider(p, 'Laki', 50, 200, Math.round(settings.scale * 100), function (v) { return v + '%'; },
      function (v) { settings.scale = v / 100; saveSettings(); render(); });
    addButton(p, 'OK', function () { closePanel(); });
  }

  // ---------- Toolbar ----------
  function renderTools() {
    toolLayer.innerHTML = '';
    var bar = document.createElement('div');
    bar.className = 'vpad-bar';
    toolLayer.appendChild(bar);

    function tool(text, fn, opts) {
      var t = document.createElement('div');
      t.className = 'vpad-tool';
      t.textContent = text;
      if (opts && opts.opacity) t.style.opacity = opts.opacity;
      if (opts && opts.bg) t.style.background = opts.bg;
      t.addEventListener('pointerdown', function (e) { e.stopPropagation(); });
      t.addEventListener('click', function (e) { e.stopPropagation(); fn(); });
      bar.appendChild(t);
    }

    if (!editing) {
      tool('Full', function () { toggleFs(); }, { opacity: '.6' });
      tool(hidden ? '\uD83D\uDC41' : '\uD83D\uDEAB', function () {
        hidden = !hidden;
        try { localStorage.setItem(HIDE_STORE, hidden ? '1' : '0'); } catch (e) {}
        renderTools(); render();
      }, { opacity: '.6' });
      tool('\u2699', function () { editing = true; renderTools(); render(); }, { opacity: '.6' });
    } else {
      tool('Done', function () {
        editing = false; selectedId = null; closePanel(); renderTools(); render();
      }, { bg: 'rgba(0,140,60,.9)' });
      tool('Reset', function () {
        if (confirm('I-reset sa default ang layout at settings?')) {
          layout = JSON.parse(JSON.stringify(DEFAULT));
          settings = { opacity: 0.6, scale: 1 };
          save(); saveSettings(); closePanel(); render();
        }
      });
      tool('Settings', function () { openSettings(); });
      tool('+ Add', function () {
        var b = { id: Date.now(), label: 'X', key: 'X', x: 50, y: 50, size: 60 };
        layout.push(b); selectedId = b.id; save(); render(); openPanel(b);
      });
    }
  }

  dlog('controller.js loaded. AndroidKeys=' + (typeof window.AndroidKeys));
  dumpPageStructure();
  focusGame();
  renderTools();
  render();
  renderDebug();
})();
