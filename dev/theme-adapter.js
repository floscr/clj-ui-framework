// Theme Adapter — live color scale editor for dev preview
// Self-contained IIFE. Persists state in localStorage.
(function() {
  'use strict';

  // ── HSL → Hex ──────────────────────────────────────────────────
  function hslToHex(h, s, l) {
    h = ((h % 360) + 360) % 360;
    s = Math.max(0, Math.min(100, s)) / 100;
    l = Math.max(0, Math.min(100, l)) / 100;
    var c = (1 - Math.abs(2 * l - 1)) * s;
    var x = c * (1 - Math.abs(((h / 60) % 2) - 1));
    var m = l - c / 2;
    var r, g, b;
    if (h < 60)       { r = c; g = x; b = 0; }
    else if (h < 120) { r = x; g = c; b = 0; }
    else if (h < 180) { r = 0; g = c; b = x; }
    else if (h < 240) { r = 0; g = x; b = c; }
    else if (h < 300) { r = x; g = 0; b = c; }
    else              { r = c; g = 0; b = x; }
    function toHex(v) {
      var n = Math.round((v + m) * 255);
      var hex = n.toString(16);
      return hex.length === 1 ? '0' + hex : hex;
    }
    return '#' + toHex(r) + toHex(g) + toHex(b);
  }

  // ── Scale step definitions [label, lightness, baseSaturation] ──
  var GRAY_STEPS = [
    [50,97,14],[100,95,14],[200,90,12],[300,82,10],[400,64,10],
    [500,46,10],[600,34,12],[700,26,16],[800,15,18],[900,9,18],[950,5,18]
  ];
  var ACCENT_STEPS = [
    [50,97,100],[100,94,95],[200,89,95],[300,82,95],[400,75,93],
    [500,67,96],[600,57,84],[700,49,72],[800,41,69],[900,34,67],[950,22,73]
  ];

  // ── Presets ────────────────────────────────────────────────────
  var PRESETS = [
    { name: 'Purple',  grayHue: 240, graySat: 1.0, accentHue: 252, accentSat: 1.0 },
    { name: 'Blue',    grayHue: 215, graySat: 0.4, accentHue: 221, accentSat: 0.87 },
    { name: 'Neutral', grayHue: 0,   graySat: 0.0, accentHue: 221, accentSat: 0.87 },
    { name: 'Warm',    grayHue: 30,  graySat: 0.7, accentHue: 24,  accentSat: 1.0 },
    { name: 'Rose',    grayHue: 340, graySat: 0.5, accentHue: 340, accentSat: 0.85 },
    { name: 'Emerald', grayHue: 160, graySat: 0.5, accentHue: 160, accentSat: 0.75 },
  ];

  // ── State ──────────────────────────────────────────────────────
  var STORAGE_KEY = 'ui-fw-theme-adapter';
  var DEFAULT = { grayHue: 240, graySat: 1.0, accentHue: 252, accentSat: 1.0, open: false };
  var state = assign({}, DEFAULT);
  try {
    var saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (saved) state = assign({}, DEFAULT, saved);
  } catch(e) {}

  function assign(target) {
    for (var i = 1; i < arguments.length; i++) {
      var src = arguments[i];
      if (src) for (var k in src) if (src.hasOwnProperty(k)) target[k] = src[k];
    }
    return target;
  }

  function save() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch(e) {}
  }

  // ── Apply colors to :root ─────────────────────────────────────
  function applyScale(name, hue, steps, satMul) {
    var root = document.documentElement.style;
    for (var i = 0; i < steps.length; i++) {
      var label = steps[i][0], l = steps[i][1], baseSat = steps[i][2];
      var sat = Math.round(Math.min(100, baseSat * satMul));
      root.setProperty('--' + name + '-' + label, hslToHex(hue, sat, l));
    }
  }

  function apply() {
    applyScale('gray', state.grayHue, GRAY_STEPS, state.graySat);
    applyScale('accent', state.accentHue, ACCENT_STEPS, state.accentSat);
    save();
    updateUI();
  }

  function reset() {
    state = assign({}, DEFAULT, { open: true });
    // Remove inline overrides so CSS file values take effect
    var root = document.documentElement.style;
    GRAY_STEPS.forEach(function(s) { root.removeProperty('--gray-' + s[0]); });
    ACCENT_STEPS.forEach(function(s) { root.removeProperty('--accent-' + s[0]); });
    save();
    updateUI();
  }

  // ── Generate EDN for tokens.edn ───────────────────────────────
  function generateEDN() {
    function fmtSteps(steps, satMul) {
      return steps.map(function(s) {
        var sat = Math.round(Math.min(100, s[2] * satMul));
        return '                    [' + String(s[0]).padStart(3) + ' ' + String(s[1]).padStart(2) + ' ' + sat + ']';
      }).join('\n');
    }
    return ':color\n' +
      '  {:gray {:hue ' + state.grayHue + ' :saturation ' + Math.round(18 * state.graySat) + '\n' +
      '          :steps [' + fmtSteps(GRAY_STEPS, state.graySat).trimStart() + ']}\n\n' +
      '   :accent {:hue ' + state.accentHue + ' :saturation ' + Math.round(96 * state.accentSat) + '\n' +
      '            :steps [' + fmtSteps(ACCENT_STEPS, state.accentSat).trimStart() + ']}}';
  }

  // ── UI ─────────────────────────────────────────────────────────
  var panel, swatchGray, swatchAccent, inputs = {};

  function el(tag, attrs, children) {
    var e = document.createElement(tag);
    if (attrs) for (var k in attrs) {
      if (k === 'style' && typeof attrs[k] === 'object') {
        for (var s in attrs[k]) e.style[s] = attrs[k][s];
      } else if (k.slice(0,2) === 'on') {
        e.addEventListener(k.slice(2), attrs[k]);
      } else {
        e.setAttribute(k, attrs[k]);
      }
    }
    if (children) {
      if (typeof children === 'string') e.textContent = children;
      else if (Array.isArray(children)) children.forEach(function(c) { if (c) e.appendChild(c); });
    }
    return e;
  }

  var S = {
    panel: {
      position: 'fixed', bottom: '16px', right: '16px', zIndex: '99999',
      fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
      fontSize: '12px', lineHeight: '1.4',
    },
    card: {
      background: '#1a1a26', color: '#e0e0e8', border: '1px solid #2a2a3a',
      borderRadius: '12px', width: '280px', overflow: 'hidden',
      boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
    },
    header: {
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '10px 12px', borderBottom: '1px solid #2a2a3a',
    },
    title: { fontWeight: '600', fontSize: '12px', letterSpacing: '0.02em' },
    body: { padding: '10px 12px', display: 'flex', flexDirection: 'column', gap: '10px' },
    presetRow: { display: 'flex', flexWrap: 'wrap', gap: '4px' },
    preset: {
      padding: '3px 8px', border: '1px solid #3a3a4a', borderRadius: '6px',
      background: 'transparent', color: '#b0b0c0', cursor: 'pointer',
      fontSize: '11px', fontFamily: 'inherit', transition: 'all 0.15s',
    },
    presetActive: {
      background: '#7a5afc', borderColor: '#7a5afc', color: '#fff',
    },
    section: { display: 'flex', flexDirection: 'column', gap: '6px' },
    label: { fontWeight: '500', fontSize: '11px', color: '#8a8aa0', textTransform: 'uppercase', letterSpacing: '0.05em' },
    sliderRow: { display: 'flex', alignItems: 'center', gap: '8px' },
    sliderLabel: { width: '26px', fontSize: '10px', color: '#6a6a80', flexShrink: '0' },
    sliderValue: { width: '32px', fontSize: '10px', color: '#9a9aac', textAlign: 'right', flexShrink: '0', fontFamily: "'JetBrains Mono', monospace" },
    slider: { flex: '1', height: '4px', WebkitAppearance: 'none', appearance: 'none', borderRadius: '2px', outline: 'none', cursor: 'pointer' },
    swatches: { display: 'flex', gap: '2px' },
    swatch: { flex: '1', height: '14px', borderRadius: '3px' },
    footer: { padding: '8px 12px', borderTop: '1px solid #2a2a3a', display: 'flex', gap: '6px' },
    btn: {
      flex: '1', padding: '5px 8px', border: '1px solid #3a3a4a', borderRadius: '6px',
      background: 'transparent', color: '#b0b0c0', cursor: 'pointer',
      fontSize: '11px', fontFamily: 'inherit', textAlign: 'center',
    },
    toggle: {
      width: '36px', height: '36px', borderRadius: '10px',
      background: '#1a1a26', border: '1px solid #2a2a3a', cursor: 'pointer',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: '16px', boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
      position: 'fixed', bottom: '16px', right: '16px', zIndex: '99999',
    },
  };

  function applyStyle(elem, style) {
    for (var k in style) elem.style[k] = style[k];
  }

  function hueGradient() {
    return 'linear-gradient(to right, hsl(0,80%,50%),hsl(60,80%,50%),hsl(120,80%,50%),hsl(180,80%,50%),hsl(240,80%,50%),hsl(300,80%,50%),hsl(360,80%,50%))';
  }

  function satGradient(hue) {
    return 'linear-gradient(to right, hsl(' + hue + ',0%,50%), hsl(' + hue + ',100%,50%))';
  }

  function makeSlider(id, label, min, max, step, gradient) {
    var row = el('div');
    applyStyle(row, S.sliderRow);
    var lbl = el('span', null, label);
    applyStyle(lbl, S.sliderLabel);
    var input = el('input', { type: 'range', min: String(min), max: String(max), step: String(step) });
    applyStyle(input, S.slider);
    input.style.background = gradient || '#3a3a4a';
    var val = el('span', null, '0');
    applyStyle(val, S.sliderValue);
    row.appendChild(lbl);
    row.appendChild(input);
    row.appendChild(val);
    inputs[id] = { input: input, value: val };
    return row;
  }

  function makeSwatches() {
    var row = el('div');
    applyStyle(row, S.swatches);
    var items = [];
    for (var i = 0; i < 11; i++) {
      var sw = el('div');
      applyStyle(sw, S.swatch);
      row.appendChild(sw);
      items.push(sw);
    }
    return { row: row, items: items };
  }

  function buildPanel() {
    // Toggle button (shown when panel is closed)
    var toggle = el('button', { onclick: function() { state.open = true; save(); updateUI(); } }, '\u{1F3A8}');
    applyStyle(toggle, S.toggle);

    // Panel
    panel = el('div');
    applyStyle(panel, S.panel);

    var card = el('div');
    applyStyle(card, S.card);

    // Header
    var header = el('div');
    applyStyle(header, S.header);
    var title = el('span', null, 'Theme');
    applyStyle(title, S.title);
    var closeBtn = el('button', { onclick: function() { state.open = false; save(); updateUI(); } }, '\u00d7');
    applyStyle(closeBtn, { background: 'none', border: 'none', color: '#6a6a80', cursor: 'pointer', fontSize: '16px', padding: '0 2px', fontFamily: 'inherit' });
    var resetBtn = el('button', { onclick: reset }, 'Reset');
    applyStyle(resetBtn, { background: 'none', border: '1px solid #3a3a4a', color: '#6a6a80', cursor: 'pointer', fontSize: '10px', padding: '2px 6px', borderRadius: '4px', fontFamily: 'inherit', marginLeft: 'auto', marginRight: '8px' });
    header.appendChild(title);
    header.appendChild(resetBtn);
    header.appendChild(closeBtn);

    // Body
    var body = el('div');
    applyStyle(body, S.body);

    // Presets
    var presetRow = el('div');
    applyStyle(presetRow, S.presetRow);
    PRESETS.forEach(function(p) {
      var btn = el('button', { onclick: function() {
        state.grayHue = p.grayHue;
        state.graySat = p.graySat;
        state.accentHue = p.accentHue;
        state.accentSat = p.accentSat;
        apply();
      }}, p.name);
      applyStyle(btn, S.preset);
      // Small color dot
      var dot = el('span');
      dot.style.cssText = 'display:inline-block;width:6px;height:6px;border-radius:50%;margin-right:4px;vertical-align:middle;';
      dot.style.background = hslToHex(p.accentHue, Math.round(96 * p.accentSat), 67);
      btn.insertBefore(dot, btn.firstChild);
      presetRow.appendChild(btn);
    });

    // Gray section
    var graySection = el('div');
    applyStyle(graySection, S.section);
    var grayLabel = el('div', null, 'Gray');
    applyStyle(grayLabel, S.label);
    graySection.appendChild(grayLabel);
    graySection.appendChild(makeSlider('grayHue', 'Hue', 0, 360, 1, hueGradient()));
    graySection.appendChild(makeSlider('graySat', 'Sat', 0, 200, 1));
    var graySwatches = makeSwatches();
    swatchGray = graySwatches.items;
    graySection.appendChild(graySwatches.row);

    // Accent section
    var accentSection = el('div');
    applyStyle(accentSection, S.section);
    var accentLabel = el('div', null, 'Accent');
    applyStyle(accentLabel, S.label);
    accentSection.appendChild(accentLabel);
    accentSection.appendChild(makeSlider('accentHue', 'Hue', 0, 360, 1, hueGradient()));
    accentSection.appendChild(makeSlider('accentSat', 'Sat', 0, 200, 1));
    var accentSwatches = makeSwatches();
    swatchAccent = accentSwatches.items;
    accentSection.appendChild(accentSwatches.row);

    body.appendChild(presetRow);
    body.appendChild(graySection);
    body.appendChild(accentSection);

    // Footer
    var footer = el('div');
    applyStyle(footer, S.footer);
    var copyBtn = el('button', { onclick: function() {
      var edn = generateEDN();
      navigator.clipboard.writeText(edn).then(function() {
        copyBtn.textContent = 'Copied!';
        setTimeout(function() { copyBtn.textContent = 'Copy EDN'; }, 1500);
      });
    }}, 'Copy EDN');
    applyStyle(copyBtn, S.btn);
    footer.appendChild(copyBtn);

    card.appendChild(header);
    card.appendChild(body);
    card.appendChild(footer);
    panel.appendChild(card);

    document.body.appendChild(toggle);
    document.body.appendChild(panel);

    // Slider events
    inputs.grayHue.input.addEventListener('input', function(e) {
      state.grayHue = parseInt(e.target.value); apply();
    });
    inputs.graySat.input.addEventListener('input', function(e) {
      state.graySat = parseInt(e.target.value) / 100; apply();
    });
    inputs.accentHue.input.addEventListener('input', function(e) {
      state.accentHue = parseInt(e.target.value); apply();
    });
    inputs.accentSat.input.addEventListener('input', function(e) {
      state.accentSat = parseInt(e.target.value) / 100; apply();
    });

    // Style range inputs (webkit + moz)
    var css = document.createElement('style');
    css.textContent = [
      '#ta-root input[type=range]::-webkit-slider-thumb {',
      '  -webkit-appearance: none; width: 14px; height: 14px;',
      '  border-radius: 50%; background: #fff; border: 2px solid #7a5afc;',
      '  cursor: pointer; margin-top: -5px; box-shadow: 0 1px 4px rgba(0,0,0,0.3);',
      '}',
      '#ta-root input[type=range]::-moz-range-thumb {',
      '  width: 14px; height: 14px; border-radius: 50%;',
      '  background: #fff; border: 2px solid #7a5afc; cursor: pointer;',
      '  box-shadow: 0 1px 4px rgba(0,0,0,0.3);',
      '}',
      '#ta-root input[type=range]::-webkit-slider-runnable-track {',
      '  height: 4px; border-radius: 2px;',
      '}',
      '#ta-root input[type=range]::-moz-range-track {',
      '  height: 4px; border-radius: 2px;',
      '}',
    ].join('\n');
    document.head.appendChild(css);
    panel.id = 'ta-root';
    toggle.id = 'ta-toggle';

    // Reference for toggle visibility
    panel._toggle = toggle;
  }

  function updateUI() {
    if (!panel) return;
    // Show/hide
    panel.style.display = state.open ? 'block' : 'none';
    panel._toggle.style.display = state.open ? 'none' : 'flex';

    // Sync slider values
    inputs.grayHue.input.value = state.grayHue;
    inputs.grayHue.value.textContent = state.grayHue + '\u00b0';
    inputs.graySat.input.value = Math.round(state.graySat * 100);
    inputs.graySat.value.textContent = Math.round(state.graySat * 100) + '%';
    inputs.graySat.input.style.background = satGradient(state.grayHue);
    inputs.accentHue.input.value = state.accentHue;
    inputs.accentHue.value.textContent = state.accentHue + '\u00b0';
    inputs.accentSat.input.value = Math.round(state.accentSat * 100);
    inputs.accentSat.value.textContent = Math.round(state.accentSat * 100) + '%';
    inputs.accentSat.input.style.background = satGradient(state.accentHue);

    // Swatches
    if (swatchGray) {
      GRAY_STEPS.forEach(function(s, i) {
        var sat = Math.round(Math.min(100, s[2] * state.graySat));
        swatchGray[i].style.background = hslToHex(state.grayHue, sat, s[1]);
      });
    }
    if (swatchAccent) {
      ACCENT_STEPS.forEach(function(s, i) {
        var sat = Math.round(Math.min(100, s[2] * state.accentSat));
        swatchAccent[i].style.background = hslToHex(state.accentHue, sat, s[1]);
      });
    }
  }

  // ── Init ───────────────────────────────────────────────────────
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  function init() {
    buildPanel();
    // Apply saved non-default state
    if (state.grayHue !== DEFAULT.grayHue || state.graySat !== DEFAULT.graySat ||
        state.accentHue !== DEFAULT.accentHue || state.accentSat !== DEFAULT.accentSat) {
      apply();
    } else {
      updateUI();
    }
  }
})();
