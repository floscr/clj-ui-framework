// Theme Adapter — live color scale editor for dev preview
// Self-contained IIFE. Persists state in localStorage.
(function() {
  'use strict';

  // ── OKLCH helpers ───────────────────────────────────────────────
  // CSS oklch() is used directly — no hex conversion needed.
  // oklch(L C H) where L: 0-1, C: 0-~0.4, H: 0-360 degrees.
  function oklchCSS(l, c, h) {
    return 'oklch(' + l.toFixed(3) + ' ' + c.toFixed(4) + ' ' + h.toFixed(1) + ')';
  }

  // OKLCH → sRGB hex for swatch preview (canvas-free fallback)
  function oklchToHex(L, C, H) {
    var hRad = H * Math.PI / 180;
    var a = C * Math.cos(hRad), b = C * Math.sin(hRad);
    var l_ = L + 0.3963377774 * a + 0.2158037573 * b;
    var m_ = L - 0.1055613458 * a - 0.0638541728 * b;
    var s_ = L - 0.0894841775 * a - 1.2914855480 * b;
    var l3 = l_*l_*l_, m3 = m_*m_*m_, s3 = s_*s_*s_;
    var rLin =  4.0767416621*l3 - 3.3077115913*m3 + 0.2309699292*s3;
    var gLin = -1.2684380046*l3 + 2.6097574011*m3 - 0.3413193965*s3;
    var bLin = -0.0041960863*l3 - 0.7034186147*m3 + 1.7076147010*s3;
    function gamma(x) { return x <= 0.0031308 ? 12.92*x : 1.055*Math.pow(Math.max(0,x),1/2.4)-0.055; }
    function clamp(x) { return Math.max(0, Math.min(255, Math.round(gamma(Math.max(0,x))*255))); }
    var r = clamp(rLin), g = clamp(gLin), bl = clamp(bLin);
    return '#' + [r,g,bl].map(function(v){ var h = v.toString(16); return h.length<2?'0'+h:h; }).join('');
  }

  // ── Scale step definitions [label, lightness, baseChroma] ──────
  // OKLCH: L is 0-1 (perceptual lightness), C is chroma (colorfulness)
  var GRAY_STEPS = [
    [50,0.975,0.003],[100,0.955,0.005],[200,0.915,0.010],[300,0.850,0.012],[400,0.690,0.025],
    [500,0.530,0.035],[600,0.425,0.035],[700,0.350,0.035],[800,0.245,0.025],[900,0.190,0.016],[950,0.145,0.011]
  ];
  var ACCENT_STEPS = [
    [50,0.965,0.020],[100,0.925,0.040],[200,0.860,0.075],[300,0.770,0.125],[400,0.690,0.170],
    [500,0.595,0.230],[600,0.505,0.255],[700,0.450,0.245],[800,0.395,0.210],[900,0.350,0.175],[950,0.260,0.130]
  ];

  // ── Presets ────────────────────────────────────────────────────
  // Presets use OKLCH hue (0-360) and chroma multiplier (0-2)
  var PRESETS = [
    { name: 'Purple',  grayHue: 285, graySat: 1.0, accentHue: 286, accentSat: 1.0 },
    { name: 'Blue',    grayHue: 255, graySat: 0.5, accentHue: 255, accentSat: 0.87 },
    { name: 'Neutral', grayHue: 0,   graySat: 0.0, accentHue: 255, accentSat: 0.87 },
    { name: 'Warm',    grayHue: 60,  graySat: 0.7, accentHue: 50,  accentSat: 0.85 },
    { name: 'Rose',    grayHue: 0,   graySat: 0.5, accentHue: 350, accentSat: 0.85 },
    { name: 'Emerald', grayHue: 165, graySat: 0.5, accentHue: 165, accentSat: 0.75 },
  ];

  // ── State ──────────────────────────────────────────────────────
  var STORAGE_KEY = 'ui-fw-theme-adapter-v2';
  var DEFAULT = { grayHue: 285, graySat: 1.0, accentHue: 286, accentSat: 1.0, open: false };
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
  function applyScale(name, hue, steps, chromaMul) {
    var root = document.documentElement.style;
    for (var i = 0; i < steps.length; i++) {
      var label = steps[i][0], l = steps[i][1], baseChroma = steps[i][2];
      var c = Math.min(0.4, baseChroma * chromaMul);
      root.setProperty('--' + name + '-' + label, oklchCSS(l, c, hue));
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
    function fmtSteps(steps, chromaMul) {
      return steps.map(function(s) {
        var c = Math.min(0.4, s[2] * chromaMul);
        return '                    [' + String(s[0]).padStart(3) + '  ' + s[1].toFixed(3) + ' ' + c.toFixed(3) + ']';
      }).join('\n');
    }
    var grayChroma = Math.min(0.4, 0.025 * state.graySat);
    var accentChroma = Math.min(0.4, 0.23 * state.accentSat);
    return ':color\n' +
      '  {:gray {:hue ' + state.grayHue + ' :chroma ' + grayChroma.toFixed(3) + '\n' +
      '          :steps [' + fmtSteps(GRAY_STEPS, state.graySat).trimStart() + ']}\n\n' +
      '   :accent {:hue ' + state.accentHue + ' :chroma ' + accentChroma.toFixed(2) + '\n' +
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
    return 'linear-gradient(to right, oklch(0.5 0 ' + hue + '), oklch(0.5 0.15 ' + hue + '))';
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
      dot.style.background = oklchToHex(0.595, 0.23 * p.accentSat, p.accentHue);
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
    graySection.appendChild(makeSlider('graySat', 'Chr', 0, 200, 1));
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
    accentSection.appendChild(makeSlider('accentSat', 'Chr', 0, 200, 1));
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

    // Swatches — use oklch() CSS for accurate preview
    if (swatchGray) {
      GRAY_STEPS.forEach(function(s, i) {
        var c = Math.min(0.4, s[2] * state.graySat);
        swatchGray[i].style.background = oklchCSS(s[1], c, state.grayHue);
      });
    }
    if (swatchAccent) {
      ACCENT_STEPS.forEach(function(s, i) {
        var c = Math.min(0.4, s[2] * state.accentSat);
        swatchAccent[i].style.background = oklchCSS(s[1], c, state.accentHue);
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
