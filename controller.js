(function () {
  if (window.__vpad) return;
  window.__vpad = true;

  var STORE = 'vpad_layout_v1';
  var HIDE_STORE = 'vpad_hidden_v1';

  // ---------- Key table ----------
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

  function fire(type, info) {
    var ev = new KeyboardEvent(type, {
      key: info.key, code: info.code, keyCode: info.kc, which: info.kc,
      bubbles: true, cancelable: true, view: window
    });
    try {
      Object.defineProperty(ev, 'keyCode', { get: function () { return info.kc; } });
      Object.defineProperty(ev, 'which', { get: function () { return info.kc; } });
    } catch (e) {}
    (document.activeElement || document).dispatchEvent(ev);
  }

  // ---------- Layout ----------
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
  var layout;
  try { layout = JSON.parse(localStorage.getItem(STORE)); } catch (e) {}
  if (!Array.isArray(layout)) layout = JSON.parse(JSON.stringify(DEFAULT));
  var hidden = false;
  try { hidden = localStorage.getItem(HIDE_STORE) === '1'; } catch (e) {}
  var editing = false, selectedId = null;

  function save() {
    try { localStorage.setItem(STORE, JSON.stringify(layout)); } catch (e) {}
  }

  // ---------- Styles ----------
  var st = document.createElement('style');
  st.textContent =
    '#vpad-root{position:fixed;left:0;top:0;right:0;bottom:0;z-index:2147483647;pointer-events:none;' +
    'font-family:sans-serif;-webkit-user-select:none;user-select:none;-webkit-touch-callout:none}' +
    '#vpad-root *{-webkit-tap-highlight-color:transparent;box-sizing:border-box}' +
    '.vpad-btn{position:absolute;transform:translate(-50%,-50%);border-radius:50%;pointer-events:auto;' +
    'touch-action:none;display:flex;align-items:center;justify-content:center;color:#fff;font-weight:bold;' +
    'background:rgba(30,30,30,.45);border:2px solid rgba(255,255,255,.6);font-size:16px;text-align:center;overflow:hidden}' +
    '.vpad-btn.down{background:rgba(255,255,255,.55);color:#000}' +
    '.vpad-btn.edit{border:2px dashed #ffd400}' +
    '.vpad-btn.sel{border:3px solid #00e5ff}' +
    '.vpad-tool{pointer-events:auto;position:absolute;background:rgba(0,0,0,.6);color:#fff;border:1px solid #fff;' +
    'border-radius:8px;padding:8px 12px;font-size:14px;touch-action:manipulation}' +
    '#vpad-panel{pointer-events:auto;position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);' +
    'background:rgba(20,20,20,.95);color:#fff;border:1px solid #888;border-radius:10px;padding:12px;width:260px;font-size:14px}' +
    '#vpad-panel label{display:block;margin:8px 0 2px}' +
    '#vpad-panel input{width:100%;padding:6px;font-size:16px;border-radius:6px;border:1px solid #666;background:#222;color:#fff}' +
    '#vpad-panel button{margin:10px 6px 0 0;padding:8px 12px;border-radius:6px;border:1px solid #888;background:#333;color:#fff;font-size:14px}';
  document.documentElement.appendChild(st);

  var root = document.createElement('div');
  root.id = 'vpad-root';
  document.documentElement.appendChild(root);

  var btnLayer = document.createElement('div');
  root.appendChild(btnLayer);
  var toolLayer = document.createElement('div');
  root.appendChild(toolLayer);

  // ---------- Buttons ----------
  function render() {
    btnLayer.innerHTML = '';
    if (hidden && !editing) return;
    layout.forEach(function (b) {
      var el = document.createElement('div');
      el.className = 'vpad-btn' + (editing ? ' edit' : '') + (editing && b.id === selectedId ? ' sel' : '');
      el.textContent = b.label;
      el.style.left = b.x + '%';
      el.style.top = b.y + '%';
      el.style.width = b.size + 'px';
      el.style.height = b.size + 'px';
      if (b.size < 50) el.style.fontSize = '12px';
      attach(el, b);
      btnLayer.appendChild(el);
    });
  }

  function attach(el, b) {
    var dragging = false, moved = false, sx = 0, sy = 0, info = keyInfo(b.key), pressed = false;

    el.addEventListener('pointerdown', function (e) {
      e.preventDefault(); e.stopPropagation();
      try { el.setPointerCapture(e.pointerId); } catch (x) {}
      if (editing) {
        dragging = true; moved = false; sx = e.clientX; sy = e.clientY;
      } else if (info && !pressed) {
        pressed = true; el.classList.add('down'); fire('keydown', info);
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
        pressed = false; el.classList.remove('down'); fire('keyup', info);
      }
    }
    el.addEventListener('pointerup', up);
    el.addEventListener('pointercancel', up);
  }

  // ---------- Edit panel ----------
  function closePanel() {
    var p = document.getElementById('vpad-panel');
    if (p) p.remove();
  }

  function openPanel(b) {
    closePanel();
    var p = document.createElement('div');
    p.id = 'vpad-panel';
    p.innerHTML =
      '<b>I-edit ang button</b>' +
      '<label>Label (text sa button)</label><input id="vp-label">' +
      '<label>Key (A-Z, 0-9, Space, Enter, Shift, ArrowUp, F1...)</label>' +
      '<input id="vp-key" list="vp-keys" autocapitalize="off">' +
      '<datalist id="vp-keys">' + KEY_NAMES.map(function (k) { return '<option value="' + k + '">'; }).join('') + '</datalist>' +
      '<label>Laki: <span id="vp-sz"></span>px</label>' +
      '<input id="vp-size" type="range" min="36" max="160">' +
      '<div><button id="vp-del">Delete</button><button id="vp-ok">OK</button></div>';
    root.appendChild(p);
    var l = p.querySelector('#vp-label'), k = p.querySelector('#vp-key'),
        s = p.querySelector('#vp-size'), sz = p.querySelector('#vp-sz');
    l.value = b.label; k.value = b.key; s.value = b.size; sz.textContent = b.size;
    l.oninput = function () { b.label = l.value; save(); render(); };
    k.oninput = function () {
      // Tanggapin lang kapag valid na key
      var v = k.value.length === 1 ? k.value.toUpperCase() : k.value;
      if (keyInfo(v)) { b.key = v; k.style.borderColor = '#666'; save(); }
      else k.style.borderColor = '#f44';
    };
    s.oninput = function () { b.size = parseInt(s.value, 10); sz.textContent = b.size; save(); render(); };
    p.querySelector('#vp-del').onclick = function () {
      layout = layout.filter(function (x) { return x.id !== b.id; });
      selectedId = null; save(); closePanel(); render();
    };
    p.querySelector('#vp-ok').onclick = function () { closePanel(); render(); };
  }

  // ---------- Toolbar ----------
  function mkTool(text, pos, fn) {
    var t = document.createElement('div');
    t.className = 'vpad-tool';
    t.textContent = text;
    for (var k in pos) t.style[k] = pos[k];
    t.addEventListener('pointerdown', function (e) { e.stopPropagation(); });
    t.addEventListener('click', function (e) { e.stopPropagation(); fn(); });
    toolLayer.appendChild(t);
  }

  function renderTools() {
    toolLayer.innerHTML = '';
    if (!editing) {
      mkTool('\u2699', { top: '6px', right: '6px', opacity: '.6' }, function () {
        editing = true; renderTools(); render();
      });
      mkTool(hidden ? '\uD83D\uDC41' : '\uD83D\uDEAB', { top: '6px', right: '52px', opacity: '.6' }, function () {
        hidden = !hidden;
        try { localStorage.setItem(HIDE_STORE, hidden ? '1' : '0'); } catch (e) {}
        renderTools(); render();
      });
    } else {
      mkTool('+ Add', { top: '6px', right: '6px' }, function () {
        var b = { id: Date.now(), label: 'X', key: 'X', x: 50, y: 50, size: 60 };
        layout.push(b); selectedId = b.id; save(); render(); openPanel(b);
      });
      mkTool('Reset', { top: '6px', right: '80px' }, function () {
        if (confirm('I-reset sa default ang layout?')) {
          layout = JSON.parse(JSON.stringify(DEFAULT)); save(); closePanel(); render();
        }
      });
      mkTool('Done', { top: '6px', right: '150px', background: 'rgba(0,140,60,.85)' }, function () {
        editing = false; selectedId = null; closePanel(); renderTools(); render();
      });
    }
  }

  renderTools();
  render();
})();
