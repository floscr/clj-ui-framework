(() => {
  // ../../../dev/squint/node_modules/squint-cljs/src/squint/core.js
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
  var IApply__apply = Symbol("IApply__apply");
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
  function truth_(x) {
    return x != null && x !== false;
  }
  var _metaSym = Symbol("meta");

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
          dismiss_BANG_();
          const on_click10 = item["on-click"];
          if (truth_(on_click10)) {
            return on_click10();
          } else {
            if (truth_(url3)) {
              return window.location = url3;
            }
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
})();
