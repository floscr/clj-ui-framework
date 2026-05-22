(() => {
  // ../../../dev/squint/node_modules/squint-cljs/src/squint/core.js
  function toFn(x) {
    if (x == null) return x;
    if (x instanceof Function) {
      return x;
    }
    const t = typeof x;
    if (t === "string") {
      return (coll, d) => {
        return get(coll, x, d);
      };
    }
    if (t === "object") {
      return (k, d) => {
        return get(x, k, d);
      };
    }
    return x;
  }
  var has = Object.prototype.hasOwnProperty;
  function findKey(iter, tar, key) {
    for (key of iter.keys()) {
      if (dequal(key, tar)) return key;
    }
  }
  function dequal(foo, bar) {
    if (foo === bar) return true;
    var ctor, len, tmp;
    if (foo && bar && (ctor = foo.constructor) === bar.constructor) {
      if (ctor === Array) {
        if ((len = foo.length) === bar.length) {
          while (len-- && dequal(foo[len], bar[len])) ;
        }
        return len === -1;
      }
      if (ctor === Set) {
        if (foo.size !== bar.size) {
          return false;
        }
        for (const elt of foo) {
          tmp = elt;
          if (tmp && typeof tmp === "object") {
            tmp = findKey(bar, tmp);
            if (!tmp) return false;
          }
          if (!bar.has(tmp)) return false;
        }
        return true;
      }
      if (ctor === Map) {
        if (foo.size !== bar.size) {
          return false;
        }
        for (const kv of foo) {
          tmp = kv[0];
          if (tmp && typeof tmp === "object") {
            tmp = findKey(bar, tmp);
            if (!tmp) return false;
          }
          if (!dequal(kv[1], bar.get(tmp))) {
            return false;
          }
        }
        return true;
      }
      if (!ctor || typeof foo === "object") {
        len = 0;
        for (const k in foo) {
          if (has.call(foo, k) && ++len && !has.call(bar, k)) return false;
          if (!(k in bar) || !dequal(foo[k], bar[k])) return false;
        }
        return Object.keys(bar).length === len;
      }
    }
    return false;
  }
  function walkArray(arr, comp) {
    return arr.every(function(x, i) {
      return i === 0 || comp(arr[i - 1], x);
    });
  }
  function _EQ_(...xs) {
    return walkArray(xs, (x, y) => dequal(x, y));
  }
  var MAP_TYPE = 1;
  var ARRAY_TYPE = 2;
  var OBJECT_TYPE = 3;
  var LIST_TYPE = 4;
  var SET_TYPE = 5;
  var LAZY_ITERABLE_TYPE = 6;
  function isObj(coll) {
    return coll.constructor === Object;
  }
  function typeConst(obj) {
    if (obj == null) {
      return void 0;
    }
    if (isObj(obj)) {
      return OBJECT_TYPE;
    }
    if (obj instanceof Map) return MAP_TYPE;
    if (obj instanceof Set) return SET_TYPE;
    if (obj instanceof List) return LIST_TYPE;
    if (Array.isArray(obj)) return ARRAY_TYPE;
    if (obj instanceof LazyIterable) return LAZY_ITERABLE_TYPE;
    if (obj instanceof SortedSet) return SET_TYPE;
    if (obj instanceof Object) return OBJECT_TYPE;
    return void 0;
  }
  function get(coll, key, otherwise = void 0) {
    if (coll == null) {
      return otherwise;
    }
    let v;
    if (isObj(coll)) {
      v = coll[key];
      if (v === void 0) {
        return otherwise;
      } else {
        return v;
      }
    }
    let g;
    switch (typeConst(coll)) {
      case SET_TYPE:
        if (coll.has(key)) v = key;
        break;
      case MAP_TYPE:
        v = coll.get(key);
        break;
      case ARRAY_TYPE:
        v = coll[key];
        break;
      default:
        g = coll["get"];
        if (g instanceof Function) {
          try {
            v = coll.get(key);
            break;
          } catch (e) {
          }
        }
        v = coll[key];
        break;
    }
    return v !== void 0 ? v : otherwise;
  }
  function seqable_QMARK_(x) {
    return x === null || x === void 0 || // we used to check instanceof Object but this returns false for TC39 Records
    // also we used to write `Symbol.iterator in` but this does not work for strings and some other types
    !!x[Symbol.iterator];
  }
  function iterable(x) {
    if (x === null || x === void 0) {
      return [];
    }
    if (seqable_QMARK_(x)) {
      return x;
    }
    if (x instanceof Object) return Object.entries(x);
    throw new TypeError(`${x} is not iterable`);
  }
  var IIterable = Symbol("Iterable");
  function first(coll) {
    const [first2] = iterable(coll);
    return first2;
  }
  var tolr = false;
  var LazyIterable = class {
    constructor(gen) {
      this.gen = gen;
      this.usages = 0;
    }
    [Symbol.iterator]() {
      this.usages++;
      if (this.usages >= 2 && tolr) {
        try {
          throw new Error();
        } catch (e) {
          console.warn("Re-use of lazy value", e.stack);
        }
      }
      return this.gen();
    }
  };
  LazyIterable.prototype[IIterable] = true;
  function lazy(f) {
    return new LazyIterable(f);
  }
  function not(expr) {
    return !truth_(expr);
  }
  var Atom = class {
    constructor(init) {
      this.val = init;
      this._watches = {};
      this._deref = () => this.val;
      this._hasWatches = false;
      this._reset_BANG_ = (x) => {
        const old_val = this.val;
        this.val = x;
        if (this._hasWatches) {
          for (const entry of Object.entries(this._watches)) {
            const k = entry[0];
            const f = entry[1];
            f(k, this, old_val, x);
          }
        }
        return x;
      };
      this._add_watch = (k, fn) => {
        this._watches[k] = fn;
        this._hasWatches = true;
      };
      this._remove_watch = (k) => {
        delete this._watches[k];
      };
    }
  };
  function atom(init) {
    return new Atom(init);
  }
  function deref(ref) {
    return ref._deref();
  }
  function reset_BANG_(atm, v) {
    atm._reset_BANG_(v);
  }
  function swap_BANG_(atm, f, ...args) {
    f = toFn(f);
    const v = f(deref(atm), ...args);
    reset_BANG_(atm, v);
    return v;
  }
  var IApply__apply = Symbol("IApply__apply");
  var List = class extends Array {
    constructor(...args) {
      super();
      this.push(...args);
    }
  };
  function concat1(colls) {
    return lazy(function* () {
      for (const coll of colls) {
        yield* iterable(coll);
      }
    });
  }
  function concat(...colls) {
    return concat1(colls);
  }
  concat[IApply__apply] = (colls) => {
    return concat1(colls);
  };
  function sort(f, coll) {
    if (arguments.length === 1) {
      coll = f;
      f = void 0;
    }
    f = toFn(f);
    coll = iterable(coll);
    const clone = [...coll];
    return clone.sort(f || compare);
  }
  function compare(x, y) {
    if (x === y) {
      return 0;
    } else {
      if (x == null) {
        return -1;
      }
      if (y == null) {
        return 1;
      }
      const tx = typeof x;
      const ty = typeof y;
      if (tx === "number" && ty === "number" || tx === "string" && ty === "string") {
        if (x === y) {
          return 0;
        }
        if (x < y) {
          return -1;
        }
        return 1;
      } else if (Array.isArray(x) && Array.isArray(y)) {
        if (x.length < y.length) {
          return -1;
        } else if (x.length > y.length) {
          return 1;
        } else {
          for (let i = 0; i < x.length; i++) {
            const c = compare(x[i], y[i]);
            if (c != 0) {
              return c;
            }
          }
          return 0;
        }
      } else {
        throw new Error(`comparing ${tx} to ${ty}`);
      }
    }
  }
  function truth_(x) {
    return x != null && x !== false;
  }
  var _metaSym = Symbol("meta");
  var SortedSet = class _SortedSet {
    constructor(xs) {
      const isSorted = xs instanceof _SortedSet;
      if (!isSorted) {
        xs = sort(xs);
      }
      const s = new Set(xs);
      this._elts = [...s];
      this._set = s;
    }
    add(x) {
      if (this._set.has(x)) return this;
      const xs = this._elts;
      let added = false;
      for (let i = 0; i < xs.length; i++) {
        if (compare(x, xs[i]) <= 0) {
          xs.splice(i, 0, x);
          added = true;
          break;
        }
      }
      if (!added) {
        xs.push(x);
        this._set.add(x);
      } else {
        this._set = new Set(xs);
      }
      this.size = xs.length;
      return this;
    }
    delete(x) {
      if (!this._set.has(x)) return this;
      const xs = this._elts;
      const idx = xs.indexOf(x);
      xs.splice(idx, 1);
      this._set = new Set(xs);
      this.size = xs.length;
      return this;
    }
    has(x) {
      return this._set.has(x);
    }
    keys() {
      return this.values();
    }
    values() {
      return this._elts[Symbol.iterator]();
    }
    entries() {
      return this._set.entries();
    }
    forEach(...xs) {
      return this.set.forEach(...xs);
    }
    clear() {
      this._elts = [];
      this._set = new Set(this._elts);
    }
    [Symbol.iterator]() {
      return this.keys();
    }
  };

  // .compiled/context_menu.mjs
  var get_state = function() {
    const or__23426__auto__1 = window["__uiCtxState"];
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const s2 = { "menu": null, "cleanup": null };
      window["__uiCtxState"] = s2;
      return s2;
    }
    ;
  };
  var create_icon = function(paths) {
    const ns_uri1 = "http://www.w3.org/2000/svg";
    const svg2 = document.createElementNS(ns_uri1, "svg");
    svg2.setAttribute("viewBox", "0 0 24 24");
    svg2.setAttribute("width", "16");
    svg2.setAttribute("height", "16");
    svg2.setAttribute("fill", "none");
    svg2.setAttribute("stroke", "currentColor");
    svg2.setAttribute("stroke-width", "2");
    svg2.setAttribute("stroke-linecap", "round");
    svg2.setAttribute("stroke-linejoin", "round");
    svg2.setAttribute("style", "flex-shrink:0");
    paths.forEach((function(d) {
      const p3 = document.createElementNS(ns_uri1, "path");
      p3.setAttribute("d", d);
      return svg2.appendChild(p3);
    }));
    return svg2;
  };
  var dismiss_BANG_ = function() {
    const state1 = get_state();
    if (truth_(state1.menu)) {
      state1.menu.remove();
      state1.menu = null;
    }
    ;
    if (truth_(state1.cleanup)) {
      state1.cleanup();
      return state1.cleanup = null;
    }
    ;
  };
  var execute_item_BANG_ = function(item) {
    const on_click1 = item["on-click"];
    const url2 = item["url"];
    if (truth_(on_click1)) {
      return on_click1();
    } else {
      if (truth_(url2)) {
        return window.location = url2;
      }
    }
    ;
  };
  var show_confirm_BANG_ = function(menu, item) {
    const confirm_val1 = item["confirm"];
    const message2 = confirm_val1 === true ? "Are you sure?" : confirm_val1;
    const _3 = menu.innerHTML = "";
    const msg_el4 = document.createElement("div");
    msg_el4.className = "context-menu-confirm-message";
    msg_el4.textContent = message2;
    menu.appendChild(msg_el4);
    const actions5 = document.createElement("div");
    actions5.className = "context-menu-confirm-actions";
    const cancel_btn6 = document.createElement("button");
    cancel_btn6.className = "context-menu-item";
    cancel_btn6.textContent = "Cancel";
    cancel_btn6.setAttribute("tabindex", "-1");
    cancel_btn6.addEventListener("click", (function(e) {
      e.preventDefault();
      e.stopPropagation();
      return dismiss_BANG_();
    }));
    actions5.appendChild(cancel_btn6);
    const danger7 = item["variant"] === "danger";
    const confirm_btn8 = document.createElement("button");
    confirm_btn8.className = danger7 ? "context-menu-item context-menu-item--danger" : "context-menu-item";
    confirm_btn8.textContent = "Confirm";
    confirm_btn8.setAttribute("tabindex", "-1");
    confirm_btn8.addEventListener("click", (function(e) {
      e.preventDefault();
      e.stopPropagation();
      dismiss_BANG_();
      return execute_item_BANG_(item);
    }));
    actions5.appendChild(confirm_btn8);
    menu.appendChild(actions5);
    const cancel9 = menu.querySelector(".context-menu-item");
    if (truth_(cancel9)) {
      return cancel9.focus();
    }
    ;
  };
  var create_menu = function(items) {
    const menu1 = document.createElement("div");
    menu1.className = "context-menu";
    menu1.setAttribute("role", "menu");
    items.forEach((function(item) {
      if (item["type"] === "separator") {
        const sep2 = document.createElement("div");
        sep2.className = "context-menu-separator";
        sep2.setAttribute("role", "separator");
        return menu1.appendChild(sep2);
      } else {
        const url3 = item["url"];
        const tag4 = truth_(url3) ? "a" : "button";
        const el5 = document.createElement(tag4);
        const danger6 = item["variant"] === "danger";
        el5.className = danger6 ? "context-menu-item context-menu-item--danger" : "context-menu-item";
        el5.setAttribute("role", "menuitem");
        el5.setAttribute("tabindex", "-1");
        if (truth_(url3)) {
          el5.setAttribute("href", url3);
        }
        ;
        const paths7 = item["icon-paths"];
        if (truth_((() => {
          const and__23442__auto__8 = paths7;
          if (truth_(and__23442__auto__8)) {
            return paths7.length > 0;
          } else {
            return and__23442__auto__8;
          }
          ;
        })())) {
          el5.appendChild(create_icon(paths7));
        }
        ;
        const span9 = document.createElement("span");
        span9.textContent = item["label"];
        el5.appendChild(span9);
        el5.addEventListener("click", (function(e) {
          e.preventDefault();
          e.stopPropagation();
          if (truth_(item["confirm"])) {
            return show_confirm_BANG_(el5.closest(".context-menu"), item);
          } else {
            dismiss_BANG_();
            return execute_item_BANG_(item);
          }
          ;
        }));
        return menu1.appendChild(el5);
      }
      ;
    }));
    return menu1;
  };
  var position_menu_BANG_ = function(menu, x, y) {
    menu.style.left = `${x ?? ""}px`;
    menu.style.top = `${y ?? ""}px`;
    document.body.appendChild(menu);
    const rect1 = menu.getBoundingClientRect();
    const vw2 = window.innerWidth;
    const vh3 = window.innerHeight;
    const new_x4 = x + rect1.width > vw2 ? vw2 - rect1.width - 8 : x;
    const new_y5 = y + rect1.height > vh3 ? vh3 - rect1.height - 8 : y;
    menu.style.left = `${new_x4 ?? ""}px`;
    return menu.style.top = `${new_y5 ?? ""}px`;
  };
  var focus_first_item_BANG_ = function(menu) {
    const first_item1 = menu.querySelector(".context-menu-item");
    if (truth_(first_item1)) {
      return first_item1.focus();
    }
    ;
  };
  var focus_next_BANG_ = function(menu, direction) {
    const items1 = menu.querySelectorAll(".context-menu-item");
    const active2 = document.activeElement;
    const len3 = items1.length;
    if (len3 > 0) {
      const current_idx4 = (() => {
        const result5 = atom(-1);
        items1.forEach((function(item, i) {
          if (_EQ_(item, active2)) {
            return reset_BANG_(result5, i);
          }
          ;
        }));
        return deref(result5);
      })();
      const next_idx6 = direction === "down" ? current_idx4 < len3 - 1 ? current_idx4 + 1 : 0 : direction === "up" ? current_idx4 > 0 ? current_idx4 - 1 : len3 - 1 : "else" ? current_idx4 : null;
      return items1[next_idx6].focus();
    }
    ;
  };
  var open_context_menu = (() => {
    const f1 = (function(var_args) {
      const args21 = [];
      const len__23321__auto__2 = arguments.length;
      let i33 = 0;
      while (true) {
        if (i33 < len__23321__auto__2) {
          args21.push(arguments[i33]);
          let G__4 = i33 + 1;
          i33 = G__4;
          continue;
        }
        ;
        break;
      }
      ;
      const argseq__23513__auto__5 = 1 < args21.length ? args21.slice(1) : null;
      return f1.cljs$core$IFn$_invoke$arity$variadic(arguments[0], argseq__23513__auto__5);
    });
    f1.cljs$core$IFn$_invoke$arity$variadic = (function(event, args) {
      dismiss_BANG_();
      const items6 = (() => {
        const passed7 = first(args);
        if (truth_(passed7)) {
          return passed7;
        } else {
          const json8 = event.currentTarget.dataset.contextMenu;
          if (truth_(json8)) {
            return JSON.parse(json8);
          }
          ;
        }
        ;
      })();
      const _9 = not(items6) ? console.warn("Context menu: no items provided") : null;
      const menu10 = create_menu(items6);
      const state11 = get_state();
      state11.menu = menu10;
      position_menu_BANG_(menu10, event.clientX, event.clientY);
      focus_first_item_BANG_(menu10);
      const on_click12 = (function(e) {
        if (not(menu10.contains(e.target))) {
          return dismiss_BANG_();
        }
        ;
      });
      const on_key13 = (function(e) {
        const key14 = e.key;
        if (key14 === "Escape") {
          e.preventDefault();
          return dismiss_BANG_();
        } else {
          if (key14 === "ArrowDown") {
            e.preventDefault();
            return focus_next_BANG_(menu10, "down");
          } else {
            if (key14 === "ArrowUp") {
              e.preventDefault();
              return focus_next_BANG_(menu10, "up");
            } else {
              return null;
            }
          }
        }
        ;
      });
      const on_scroll15 = (function(_) {
        return dismiss_BANG_();
      });
      const on_resize16 = (function(_) {
        return dismiss_BANG_();
      });
      const cleanup17 = (function() {
        document.removeEventListener("click", on_click12, true);
        document.removeEventListener("keydown", on_key13, true);
        window.removeEventListener("scroll", on_scroll15, true);
        return window.removeEventListener("resize", on_resize16);
      });
      document.addEventListener("click", on_click12, true);
      document.addEventListener("keydown", on_key13, true);
      window.addEventListener("scroll", on_scroll15, true);
      window.addEventListener("resize", on_resize16);
      return state11.cleanup = cleanup17;
    });
    f1.cljs$lang$maxFixedArity = 1;
    return f1;
  })();
  window["__uiContextMenu"] = open_context_menu;

  // .compiled/theme.mjs
  var storage_key = "ui-theme";
  var get_stored = function() {
    return (() => {
      try {
        return localStorage.getItem(storage_key);
      } catch (_e1) {
        return null;
      }
    })();
  };
  var store_BANG_ = function(mode) {
    return (() => {
      try {
        if (mode === "auto") {
          return localStorage.removeItem(storage_key);
        } else {
          return localStorage.setItem(storage_key, mode);
        }
        ;
      } catch (_e1) {
        return null;
      }
    })();
  };
  var system_prefers_dark_QMARK_ = function() {
    return window.matchMedia("(prefers-color-scheme: dark)").matches;
  };
  var resolve_effective = function(mode) {
    const G__51 = mode;
    switch (G__51) {
      case "light":
        return "light";
        break;
      case "dark":
        return "dark";
        break;
      default:
        if (truth_(system_prefers_dark_QMARK_())) {
          return "dark";
        } else {
          return "light";
        }
    }
    ;
  };
  var suppress_transitions_BANG_ = function() {
    const el1 = document.documentElement;
    el1.setAttribute("data-no-transitions", "");
    el1.offsetHeight;
    return requestAnimationFrame((function() {
      return requestAnimationFrame((function() {
        return el1.removeAttribute("data-no-transitions");
      }));
    }));
  };
  var apply_theme_BANG_ = function(mode) {
    const el1 = document.documentElement;
    suppress_transitions_BANG_();
    const G__62 = mode;
    switch (G__62) {
      case "light":
        return el1.setAttribute("data-theme", "light");
        break;
      case "dark":
        return el1.setAttribute("data-theme", "dark");
        break;
      default:
        return el1.removeAttribute("data-theme");
    }
    ;
  };
  var subscribers = atom([]);
  var notify_BANG_ = function(mode, effective) {
    const subs1 = deref(subscribers);
    return subs1.forEach((function(f) {
      return f({ "mode": mode, "effective": effective });
    }));
  };
  var get_mode = function() {
    const or__23426__auto__1 = get_stored();
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      return "auto";
    }
    ;
  };
  var get_effective = function() {
    return resolve_effective(get_mode());
  };
  var set_mode_BANG_ = function(mode) {
    const m1 = truth_(get(/* @__PURE__ */ new Set(["auto", "light", "dark"]), mode)) ? mode : "auto";
    store_BANG_(m1);
    apply_theme_BANG_(m1);
    return notify_BANG_(m1, resolve_effective(m1));
  };
  var toggle_BANG_ = function() {
    const current1 = get_mode();
    const next_mode2 = (() => {
      const G__73 = current1;
      switch (G__73) {
        case "auto":
          return "light";
          break;
        case "light":
          return "dark";
          break;
        case "dark":
          return "auto";
          break;
        default:
          return "auto";
      }
      ;
    })();
    set_mode_BANG_(next_mode2);
    return next_mode2;
  };
  var subscribe_BANG_ = function(f) {
    swap_BANG_(subscribers, (function(subs) {
      return subs.concat([f]);
    }));
    return function() {
      return swap_BANG_(subscribers, (function(subs) {
        return subs.filter((function(s) {
          return !_EQ_(s, f);
        }));
      }));
    };
  };
  var init_BANG_ = function() {
    const mode1 = get_mode();
    apply_theme_BANG_(mode1);
    const mql2 = window.matchMedia("(prefers-color-scheme: dark)");
    return mql2.addEventListener("change", (function(_e) {
      if (get_mode() === "auto") {
        apply_theme_BANG_("auto");
        return notify_BANG_("auto", resolve_effective("auto"));
      }
      ;
    }));
  };
  window["__uiTheme"] = { "init": init_BANG_, "set": set_mode_BANG_, "get": get_mode, "effective": get_effective, "toggle": toggle_BANG_, "subscribe": subscribe_BANG_ };
})();
