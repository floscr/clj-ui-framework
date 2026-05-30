// Theme Adapter — live color scale editor for dev preview
// Uses the framework's own CSS classes (dogfooding).
// Self-contained IIFE. Persists state in localStorage.
(function() {
  'use strict';

  // ── OKLCH helpers ───────────────────────────────────────────────
  function oklchCSS(l, c, h) {
    return 'oklch(' + l.toFixed(3) + ' ' + c.toFixed(4) + ' ' + h.toFixed(1) + ')';
  }

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
  var GRAY_STEPS = [
    [50,0.975,0.003],[100,0.955,0.005],[200,0.915,0.010],[300,0.850,0.012],[400,0.690,0.025],
    [500,0.530,0.035],[600,0.425,0.035],[700,0.350,0.035],[800,0.245,0.025],[900,0.190,0.016],[950,0.145,0.011]
  ];
  var ACCENT_STEPS = [
    [50,0.965,0.020],[100,0.925,0.040],[200,0.860,0.075],[300,0.770,0.125],[400,0.690,0.170],
    [500,0.595,0.230],[600,0.505,0.255],[700,0.450,0.245],[800,0.395,0.210],[900,0.350,0.175],[950,0.260,0.130]
  ];

  // ── Presets ────────────────────────────────────────────────────
  var PRESETS = [
    { name: 'Purple',  grayHue: 285, graySat: 1.0, accentHue: 286, accentSat: 1.0 },
    { name: 'Blue',    grayHue: 255, graySat: 0.5, accentHue: 255, accentSat: 0.87 },
    { name: 'Neutral', grayHue: 0,   graySat: 0.0, accentHue: 255, accentSat: 0.87 },
    { name: 'Warm',    grayHue: 60,  graySat: 0.7, accentHue: 50,  accentSat: 0.85 },
    { name: 'Rose',    grayHue: 0,   graySat: 0.5, accentHue: 350, accentSat: 0.85 },
    { name: 'Emerald', grayHue: 165, graySat: 0.5, accentHue: 165, accentSat: 0.75 },
  ];

  // ── Spacing scale ───────────────────────────────────────────────
  var SIZE_STEPS = 16;  // --size-1 through --size-16
  var DEFAULT_SIZE_BASE = 0.25; // rem

  // ── Font scale ────────────────────────────────────────────────
  // Geometric: --font-{label} = base × ratio^power
  var FONT_STEPS = [[-2,'xs'],[-1,'sm'],[0,'base'],[1,'md'],[2,'lg'],[3,'xl'],[4,'2xl'],[5,'3xl']];
  var DEFAULT_FONT_BASE = 1.0;  // rem
  var DEFAULT_FONT_RATIO = 1.25;

  // ── Border radius ──────────────────────────────────────────────
  // Base values in px, scaled by a multiplier (0–2×)
  var RADIUS_DEFAULTS = [['sm',6],['md',10],['lg',16]];
  var DEFAULT_RADIUS_SCALE = 1.0;

  // ── State ──────────────────────────────────────────────────────
  var STORAGE_KEY = 'ui-fw-theme-adapter-v2';
  var DEFAULT = { grayHue: 285, graySat: 1.0, accentHue: 286, accentSat: 1.0, sizeBase: DEFAULT_SIZE_BASE, fontBase: DEFAULT_FONT_BASE, fontRatio: DEFAULT_FONT_RATIO, radiusScale: DEFAULT_RADIUS_SCALE, open: false };
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

  function applySpacing() {
    var root = document.documentElement.style;
    for (var n = 1; n <= SIZE_STEPS; n++) {
      var val = (state.sizeBase * n);
      // Format: strip trailing zeros, max 3 decimal places
      var s = val.toFixed(3).replace(/0+$/, '').replace(/\.$/, '');
      root.setProperty('--size-' + n, s + 'rem');
    }
  }

  function applyFont() {
    var root = document.documentElement.style;
    for (var i = 0; i < FONT_STEPS.length; i++) {
      var power = FONT_STEPS[i][0], label = FONT_STEPS[i][1];
      var val = state.fontBase * Math.pow(state.fontRatio, power);
      var s = val.toFixed(3).replace(/0+$/, '').replace(/\.$/, '');
      root.setProperty('--font-' + label, s + 'rem');
    }
  }

  function applyRadius() {
    var root = document.documentElement.style;
    for (var i = 0; i < RADIUS_DEFAULTS.length; i++) {
      var label = RADIUS_DEFAULTS[i][0], base = RADIUS_DEFAULTS[i][1];
      var val = Math.round(base * state.radiusScale);
      root.setProperty('--radius-' + label, val + 'px');
    }
  }

  function apply() {
    applyScale('gray', state.grayHue, GRAY_STEPS, state.graySat);
    applyScale('accent', state.accentHue, ACCENT_STEPS, state.accentSat);
    applySpacing();
    applyFont();
    applyRadius();
    save();
    updateUI();
  }

  function reset() {
    state = assign({}, DEFAULT, { open: true });
    var root = document.documentElement.style;
    GRAY_STEPS.forEach(function(s) { root.removeProperty('--gray-' + s[0]); });
    ACCENT_STEPS.forEach(function(s) { root.removeProperty('--accent-' + s[0]); });
    for (var n = 1; n <= SIZE_STEPS; n++) { root.removeProperty('--size-' + n); }
    for (var i = 0; i < FONT_STEPS.length; i++) { root.removeProperty('--font-' + FONT_STEPS[i][1]); }
    for (var i = 0; i < RADIUS_DEFAULTS.length; i++) { root.removeProperty('--radius-' + RADIUS_DEFAULTS[i][0]); }
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
    var sizeBase = state.sizeBase.toFixed(3).replace(/0+$/, '').replace(/\.$/, '');
    var fontBase = state.fontBase.toFixed(3).replace(/0+$/, '').replace(/\.$/, '');
    var fontRatio = state.fontRatio.toFixed(3).replace(/0+$/, '').replace(/\.$/, '');
    var fontStepsEDN = FONT_STEPS.map(function(s) {
      return '[' + String(s[0]).padStart(2) + ' "' + s[1] + '"]';
    }).join('\n                 ');
    return ':scales\n' +
      ' {:size {:base ' + sizeBase + ' :unit "rem" :steps ' + SIZE_STEPS + '}\n\n' +
      '  :font {:base ' + fontBase + ' :unit "rem" :ratio ' + fontRatio + '\n' +
      '         :steps [' + fontStepsEDN + ']}\n\n' +
      '  :color\n' +
      '  {:gray {:hue ' + state.grayHue + ' :chroma ' + grayChroma.toFixed(3) + '\n' +
      '          :steps [' + fmtSteps(GRAY_STEPS, state.graySat).trimStart() + ']}\n\n' +
      '   :accent {:hue ' + state.accentHue + ' :chroma ' + accentChroma.toFixed(2) + '\n' +
      '            :steps [' + fmtSteps(ACCENT_STEPS, state.accentSat).trimStart() + ']}}}' +
      '\n\n;; Radius (add to :tokens and :themes > :dark)\n' +
      RADIUS_DEFAULTS.map(function(r) {
        return ':radius-' + r[0] + ' "' + Math.round(r[1] * state.radiusScale) + 'px"';
      }).join('\n');
  }

  // ── Generate compact build-css opts ─────────────────────────────
  // Only includes parameters that differ from defaults.
  // Produces EDN suitable for passing to (build-css {...}).
  function generateScalesEDN() {
    var parts = [];
    var colorParts = [];

    // Gray
    var grayOverrides = [];
    if (state.grayHue !== DEFAULT.grayHue) grayOverrides.push(':hue ' + state.grayHue);
    if (state.graySat !== DEFAULT.graySat) {
      var cs = state.graySat.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
      grayOverrides.push(':chroma-scale ' + cs);
    }
    if (grayOverrides.length) colorParts.push(':gray {' + grayOverrides.join(' ') + '}');

    // Accent
    var accentOverrides = [];
    if (state.accentHue !== DEFAULT.accentHue) accentOverrides.push(':hue ' + state.accentHue);
    if (state.accentSat !== DEFAULT.accentSat) {
      var cs = state.accentSat.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
      accentOverrides.push(':chroma-scale ' + cs);
    }
    if (accentOverrides.length) colorParts.push(':accent {' + accentOverrides.join(' ') + '}');

    if (colorParts.length) parts.push(':color {' + colorParts.join('\n              ') + '}');

    // Size
    if (state.sizeBase !== DEFAULT.sizeBase) {
      var sb = state.sizeBase.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
      parts.push(':size {:base ' + sb + '}');
    }

    // Font
    var fontOverrides = [];
    if (state.fontBase !== DEFAULT.fontBase) {
      var fb = state.fontBase.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
      fontOverrides.push(':base ' + fb);
    }
    if (state.fontRatio !== DEFAULT.fontRatio) {
      var fr = state.fontRatio.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
      fontOverrides.push(':ratio ' + fr);
    }
    if (fontOverrides.length) parts.push(':font {' + fontOverrides.join(' ') + '}');

    // Radius tokens
    var radiusTokens = [];
    if (state.radiusScale !== DEFAULT.radiusScale) {
      RADIUS_DEFAULTS.forEach(function(r) {
        radiusTokens.push(':radius-' + r[0] + ' "' + Math.round(r[1] * state.radiusScale) + 'px"');
      });
    }

    // Build final EDN
    var result = '';
    if (parts.length) {
      result += '{:scales {' + parts.join('\n          ') + '}';
      if (radiusTokens.length) {
        result += '\n :tokens {' + radiusTokens.join(' ') + '}';
      }
      result += '}';
    } else if (radiusTokens.length) {
      result += '{:tokens {' + radiusTokens.join(' ') + '}}';
    } else {
      result += '{} ;; default theme, no overrides';
    }
    return result;
  }

  // ── DOM helpers ────────────────────────────────────────────────
  var panel, toggleBtn, swatchGray, swatchAccent, presetBtns = [], inputs = {};

  function el(tag, cls, children) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (typeof children === 'string') e.textContent = children;
    else if (Array.isArray(children)) children.forEach(function(c) { if (c) e.appendChild(c); });
    return e;
  }

  function hueGradient() {
    return 'linear-gradient(to right, oklch(0.65 0.15 0),oklch(0.65 0.15 60),oklch(0.65 0.15 120),oklch(0.65 0.15 180),oklch(0.65 0.15 240),oklch(0.65 0.15 300),oklch(0.65 0.15 360))';
  }

  function chromaGradient(hue) {
    return 'linear-gradient(to right, oklch(0.5 0 ' + hue + '), oklch(0.5 0.15 ' + hue + '))';
  }

  // ── Build slider row ──────────────────────────────────────────
  function makeSlider(id, label, min, max, step, gradient) {
    var row = el('div', 'flex items-center gap-2');

    var lbl = el('span', 'text-xs text-faint shrink-0', label);
    lbl.style.width = '26px';

    var input = el('input', 'form-range flex-1');
    input.type = 'range';
    input.min = String(min);
    input.max = String(max);
    input.step = String(step);
    if (gradient) input.style.background = gradient;

    var val = el('span', 'text-xs text-muted shrink-0 font-mono text-right', '0');
    val.style.width = '32px';

    row.appendChild(lbl);
    row.appendChild(input);
    row.appendChild(val);
    inputs[id] = { input: input, value: val };
    return row;
  }

  // ── Build swatch row ──────────────────────────────────────────
  function makeSwatches() {
    var row = el('div', 'swatch-row');
    var items = [];
    for (var i = 0; i < 11; i++) {
      var sw = el('div', 'swatch');
      row.appendChild(sw);
      items.push(sw);
    }
    return { row: row, items: items };
  }

  // ── Build panel ───────────────────────────────────────────────
  function buildPanel() {
    var root = el('div', 'popover popover-br');

    // Toggle button (visible when panel closed)
    toggleBtn = el('button', 'btn btn-secondary btn-icon btn-icon-round', '\u{1F3A8}');
    toggleBtn.addEventListener('click', function() {
      state.open = true; save(); updateUI();
    });

    // Panel card
    panel = el('div', 'card card-flush');
    panel.style.width = '280px';

    // ─ Header section ─
    var header = el('div', 'card-section flex items-center justify-between');
    var title = el('span', 'font-semibold text-sm', 'Theme');
    var headerActions = el('div', 'flex items-center gap-2');
    var resetBtn = el('button', 'btn btn-ghost btn-sm', 'Reset');
    resetBtn.addEventListener('click', reset);
    var closeBtn = el('button', 'btn btn-ghost btn-icon btn-sm', '\u00d7');
    closeBtn.addEventListener('click', function() {
      state.open = false; save(); updateUI();
    });
    headerActions.appendChild(resetBtn);
    headerActions.appendChild(closeBtn);
    header.appendChild(title);
    header.appendChild(headerActions);

    // ─ Body section ─
    var body = el('div', 'card-section vstack gap-3');

    // Presets
    var presetRow = el('div', 'hstack gap-1');
    PRESETS.forEach(function(p) {
      var dot = el('span', 'chip-dot');
      dot.style.background = oklchToHex(0.595, 0.23 * p.accentSat, p.accentHue);

      var btn = el('button', 'chip');
      btn.appendChild(dot);
      btn.appendChild(document.createTextNode(p.name));
      btn.addEventListener('click', function() {
        state.grayHue = p.grayHue;
        state.graySat = p.graySat;
        state.accentHue = p.accentHue;
        state.accentSat = p.accentSat;
        apply();
      });
      presetBtns.push({ el: btn, preset: p });
      presetRow.appendChild(btn);
    });

    // Gray scale section
    var graySection = el('div', 'vstack gap-2');
    graySection.appendChild(el('div', 'text-xs text-faint uppercase tracking-wide font-semibold', 'Gray'));
    graySection.appendChild(makeSlider('grayHue', 'Hue', 0, 360, 1, hueGradient()));
    graySection.appendChild(makeSlider('graySat', 'Chr', 0, 200, 1));
    var graySwatches = makeSwatches();
    swatchGray = graySwatches.items;
    graySection.appendChild(graySwatches.row);

    // Accent scale section
    var accentSection = el('div', 'vstack gap-2');
    accentSection.appendChild(el('div', 'text-xs text-faint uppercase tracking-wide font-semibold', 'Accent'));
    accentSection.appendChild(makeSlider('accentHue', 'Hue', 0, 360, 1, hueGradient()));
    accentSection.appendChild(makeSlider('accentSat', 'Chr', 0, 200, 1));
    var accentSwatches = makeSwatches();
    swatchAccent = accentSwatches.items;
    accentSection.appendChild(accentSwatches.row);

    // Spacing section
    var spacingSection = el('div', 'vstack gap-2');
    spacingSection.appendChild(el('div', 'text-xs text-faint uppercase tracking-wide font-semibold', 'Spacing'));
    spacingSection.appendChild(makeSlider('sizeBase', 'Base', 10, 50, 1));

    // Font section
    var fontSection = el('div', 'vstack gap-2');
    fontSection.appendChild(el('div', 'text-xs text-faint uppercase tracking-wide font-semibold', 'Font'));
    fontSection.appendChild(makeSlider('fontBase', 'Base', 75, 125, 1));
    fontSection.appendChild(makeSlider('fontRatio', 'Ratio', 105, 150, 1));

    // Radius section
    var radiusSection = el('div', 'vstack gap-2');
    radiusSection.appendChild(el('div', 'text-xs text-faint uppercase tracking-wide font-semibold', 'Radius'));
    radiusSection.appendChild(makeSlider('radiusScale', 'Scale', 0, 200, 5));

    body.appendChild(presetRow);
    body.appendChild(graySection);
    body.appendChild(accentSection);
    body.appendChild(spacingSection);
    body.appendChild(fontSection);
    body.appendChild(radiusSection);

    // ─ Footer section ─
    var footer = el('div', 'card-section vstack gap-2');
    var copyBtn = el('button', 'btn btn-secondary btn-sm w-full', 'Copy EDN');
    copyBtn.addEventListener('click', function() {
      var edn = generateEDN();
      navigator.clipboard.writeText(edn).then(function() {
        copyBtn.textContent = 'Copied!';
        setTimeout(function() { copyBtn.textContent = 'Copy EDN'; }, 1500);
      });
    });
    var copyScalesBtn = el('button', 'btn btn-ghost btn-sm w-full', 'Copy Scales');
    copyScalesBtn.addEventListener('click', function() {
      var edn = generateScalesEDN();
      navigator.clipboard.writeText(edn).then(function() {
        copyScalesBtn.textContent = 'Copied!';
        setTimeout(function() { copyScalesBtn.textContent = 'Copy Scales'; }, 1500);
      });
    });
    footer.appendChild(copyBtn);
    footer.appendChild(copyScalesBtn);

    // Assemble
    panel.appendChild(header);
    panel.appendChild(body);
    panel.appendChild(footer);
    root.appendChild(toggleBtn);
    root.appendChild(panel);
    document.body.appendChild(root);

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
    inputs.sizeBase.input.addEventListener('input', function(e) {
      state.sizeBase = parseInt(e.target.value) / 100; apply();
    });
    inputs.fontBase.input.addEventListener('input', function(e) {
      state.fontBase = parseInt(e.target.value) / 100; apply();
    });
    inputs.fontRatio.input.addEventListener('input', function(e) {
      state.fontRatio = parseInt(e.target.value) / 100; apply();
    });
    inputs.radiusScale.input.addEventListener('input', function(e) {
      state.radiusScale = parseInt(e.target.value) / 100; apply();
    });
  }

  // ── Update UI ─────────────────────────────────────────────────
  function updateUI() {
    if (!panel) return;

    // Show/hide
    panel.style.display = state.open ? 'flex' : 'none';
    toggleBtn.style.display = state.open ? 'none' : 'inline-flex';

    // Sync slider values
    inputs.grayHue.input.value = state.grayHue;
    inputs.grayHue.value.textContent = state.grayHue + '\u00b0';
    inputs.graySat.input.value = Math.round(state.graySat * 100);
    inputs.graySat.value.textContent = Math.round(state.graySat * 100) + '%';
    inputs.graySat.input.style.background = chromaGradient(state.grayHue);
    inputs.accentHue.input.value = state.accentHue;
    inputs.accentHue.value.textContent = state.accentHue + '\u00b0';
    inputs.accentSat.input.value = Math.round(state.accentSat * 100);
    inputs.accentSat.value.textContent = Math.round(state.accentSat * 100) + '%';
    inputs.accentSat.input.style.background = chromaGradient(state.accentHue);

    // Sync spacing slider
    inputs.sizeBase.input.value = Math.round(state.sizeBase * 100);
    inputs.sizeBase.value.textContent = state.sizeBase.toFixed(2);

    // Sync font sliders
    inputs.fontBase.input.value = Math.round(state.fontBase * 100);
    inputs.fontBase.value.textContent = state.fontBase.toFixed(2);
    inputs.fontRatio.input.value = Math.round(state.fontRatio * 100);
    inputs.fontRatio.value.textContent = state.fontRatio.toFixed(2);

    // Sync radius slider
    inputs.radiusScale.input.value = Math.round(state.radiusScale * 100);
    inputs.radiusScale.value.textContent = Math.round(state.radiusScale * 100) + '%';

    // Preset active states — uses .chip / .chip-active
    presetBtns.forEach(function(item) {
      var p = item.preset;
      var isActive = (state.grayHue === p.grayHue && state.graySat === p.graySat &&
                      state.accentHue === p.accentHue && state.accentSat === p.accentSat);
      item.el.className = isActive ? 'chip chip-active' : 'chip';
    });

    // Swatches
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
  function init() {
    buildPanel();
    if (state.grayHue !== DEFAULT.grayHue || state.graySat !== DEFAULT.graySat ||
        state.accentHue !== DEFAULT.accentHue || state.accentSat !== DEFAULT.accentSat ||
        state.sizeBase !== DEFAULT.sizeBase ||
        state.fontBase !== DEFAULT.fontBase || state.fontRatio !== DEFAULT.fontRatio ||
        state.radiusScale !== DEFAULT.radiusScale) {
      apply();
    } else {
      updateUI();
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
