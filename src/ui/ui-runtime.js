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
  function walkArray(arr, comp2) {
    return arr.every(function(x, i) {
      return i === 0 || comp2(arr[i - 1], x);
    });
  }
  function _EQ_(...xs) {
    return walkArray(xs, (x, y) => dequal(x, y));
  }
  function _PLUS_(...xs) {
    return xs.reduce((x, y) => x + y, 0);
  }
  function mapAssocMut(m, k, v) {
    m.set(k, v);
    return m;
  }
  function objAssocMut(m, k, v) {
    m[k] = v;
    return m;
  }
  function getAssocMut(m) {
    switch (typeConst(m)) {
      case MAP_TYPE:
        return mapAssocMut;
      case ARRAY_TYPE:
      case OBJECT_TYPE:
        return objAssocMut;
    }
  }
  function assoc_BANG_(m, k, v, ...kvs) {
    if (kvs.length % 2 !== 0) {
      throw new Error("Illegal argument: assoc expects an odd number of arguments.");
    }
    switch (typeConst(m)) {
      case MAP_TYPE:
        m.set(k, v);
        for (let i = 0; i < kvs.length; i += 2) {
          m.set(kvs[i], kvs[i + 1]);
        }
        break;
      case ARRAY_TYPE:
      case OBJECT_TYPE:
        m[k] = v;
        for (let i = 0; i < kvs.length; i += 2) {
          m[kvs[i]] = kvs[i + 1];
        }
        break;
      default:
        throw new Error(
          `Illegal argument: assoc! expects a Map, Array, or Object as the first argument, but got ${typeof m}.`
        );
    }
    return m;
  }
  function copy(o) {
    switch (typeConst(o)) {
      case MAP_TYPE:
        return new Map(o);
      case SET_TYPE:
        return new o.constructor(o);
      case ARRAY_TYPE:
        return [...o];
      case OBJECT_TYPE:
        return { ...o };
      default:
        throw new Error(`Don't know how to copy object of type ${typeof o}.`);
    }
  }
  function assoc(o, k, v, ...kvs) {
    if (!o) {
      o = {};
    }
    const ret = copy(o);
    assoc_BANG_(ret, k, v, ...kvs);
    return ret;
  }
  var MAP_TYPE = 1;
  var ARRAY_TYPE = 2;
  var OBJECT_TYPE = 3;
  var LIST_TYPE = 4;
  var SET_TYPE = 5;
  var LAZY_ITERABLE_TYPE = 6;
  function emptyOfType(type) {
    switch (type) {
      case MAP_TYPE:
        return /* @__PURE__ */ new Map();
      case ARRAY_TYPE:
        return [];
      case OBJECT_TYPE:
        return {};
      // Object.create?
      case LIST_TYPE:
        return new List();
      case SET_TYPE:
        return /* @__PURE__ */ new Set();
      case LAZY_ITERABLE_TYPE:
        return lazy(function* () {
          return;
        });
    }
    return void 0;
  }
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
  function assoc_in_with(f, fname, o, keys2, value) {
    keys2 = vec(keys2);
    o = o || {};
    const baseType = typeConst(o);
    if (baseType !== MAP_TYPE && baseType !== ARRAY_TYPE && baseType !== OBJECT_TYPE)
      throw new Error(
        `Illegal argument: ${fname} expects the first argument to be a Map, Array, or Object.`
      );
    const chain = [o];
    let lastInChain = o;
    for (let i = 0; i < keys2.length - 1; i += 1) {
      const k = keys2[i];
      let chainValue;
      if (lastInChain instanceof Map) chainValue = lastInChain.get(k);
      else chainValue = lastInChain[k];
      if (!chainValue) {
        chainValue = emptyOfType(baseType);
      }
      chain.push(chainValue);
      lastInChain = chainValue;
    }
    chain.push(value);
    for (let i = chain.length - 2; i >= 0; i -= 1) {
      chain[i] = f(chain[i], keys2[i], chain[i + 1]);
    }
    return chain[0];
  }
  function assoc_in(o, keys2, value) {
    return assoc_in_with(assoc, "assoc-in", o, keys2, value);
  }
  function comp(...fs) {
    fs = fs.map(toFn);
    if (fs.length === 0) {
      return identity;
    } else if (fs.length === 1) {
      return fs[0];
    }
    const [f, ...more] = fs.slice().reverse();
    return function(...args) {
      let x = f(...args);
      for (const g of more) {
        x = g(x);
      }
      return x;
    };
  }
  function conj_BANG_set(o, rest2) {
    for (const x of rest2) {
      o.add(x);
    }
    return o;
  }
  function conj_BANG_(...xs) {
    if (xs.length === 0) {
      return vector();
    }
    const [_o, ...rest2] = xs;
    let o = _o;
    if (o === null || o === void 0) {
      o = [];
    }
    switch (typeConst(o)) {
      case SET_TYPE:
        conj_BANG_set(o, rest2);
        break;
      case LIST_TYPE:
        o.unshift(...rest2.reverse());
        break;
      case ARRAY_TYPE:
        o.push(...rest2);
        break;
      case MAP_TYPE:
        for (const x of rest2) {
          if (!Array.isArray(x))
            iterable(x).forEach((kv) => {
              o.set(kv[0], kv[1]);
            });
          else o.set(x[0], x[1]);
        }
        break;
      case OBJECT_TYPE:
        for (const x of rest2) {
          if (!Array.isArray(x)) Object.assign(o, x);
          else o[x[0]] = x[1];
        }
        break;
      default:
        throw new Error(
          "Illegal argument: conj! expects a Set, Array, List, Map, or Object as the first argument."
        );
    }
    return o;
  }
  function conj(...xs) {
    if (xs.length === 0) {
      return vector();
    }
    const [_o, ...rest2] = xs;
    let o = _o;
    if (o === null || o === void 0) {
      o = list();
    }
    let m, o2;
    switch (typeConst(o)) {
      case SET_TYPE:
        if (o instanceof SortedSet) {
          return conj_BANG_set(new o.constructor(o), rest2);
        } else {
          return new o.constructor([...o, ...rest2]);
        }
      case LIST_TYPE:
        return new List(...rest2.reverse(), ...o);
      case ARRAY_TYPE:
        return [...o, ...rest2];
      case MAP_TYPE:
        m = new Map(o);
        for (const x of rest2) {
          if (!Array.isArray(x))
            iterable(x).forEach((kv) => {
              m.set(kv[0], kv[1]);
            });
          else m.set(x[0], x[1]);
        }
        return m;
      case LAZY_ITERABLE_TYPE:
        return lazy(function* () {
          yield* rest2;
          yield* o;
        });
      case OBJECT_TYPE:
        o2 = { ...o };
        for (const x of rest2) {
          if (!Array.isArray(x)) Object.assign(o2, x);
          else o2[x[0]] = x[1];
        }
        return o2;
      default:
        throw new Error(
          "Illegal argument: conj expects a Set, Array, List, Map, or Object as the first argument."
        );
    }
  }
  function contains_QMARK_(coll, v) {
    switch (typeConst(coll)) {
      case SET_TYPE:
      case MAP_TYPE:
        return coll.has(v);
      case void 0:
        return false;
      default:
        return v in coll;
    }
  }
  function dissoc(m, ...ks) {
    if (!m) return;
    if (ks.length === 0) return m;
    const m2 = copy(m);
    switch (typeConst(m)) {
      case MAP_TYPE:
        for (const k of ks) {
          m2.delete(k);
        }
        break;
      default:
        for (const k of ks) {
          delete m2[k];
        }
        break;
    }
    return m2;
  }
  function nth(coll, idx, orElse) {
    if (coll) {
      var elt = void 0;
      if (Array.isArray(coll)) {
        elt = coll[idx];
      } else {
        const iter = iterable(coll);
        let i = 0;
        for (const value of iter) {
          if (i++ == idx) {
            elt = value;
            break;
          }
        }
      }
      if (elt !== void 0) {
        return elt;
      }
    }
    return orElse;
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
  var IIterable__iterator = Symbol.iterator;
  function _iterator(coll) {
    return coll[Symbol.iterator]();
  }
  var es6_iterator = _iterator;
  function seq(x) {
    if (x == null) return x;
    const iter = iterable(x);
    if (iter.length === 0 || iter.size === 0) {
      return null;
    }
    const _i = iter[Symbol.iterator]();
    if (_i.next().done) return null;
    return iter;
  }
  function first(coll) {
    const [first2] = iterable(coll);
    return first2;
  }
  function second(coll) {
    const [_, v] = iterable(coll);
    return v;
  }
  function rest(coll) {
    return lazy(function* () {
      let first2 = true;
      for (const x of iterable(coll)) {
        if (first2) first2 = false;
        else yield x;
      }
    });
  }
  var Reduced = class {
    value;
    constructor(x) {
      this.value = x;
    }
    _deref() {
      return this.value;
    }
  };
  function reduced(x) {
    return new Reduced(x);
  }
  function reduced_QMARK_(x) {
    return x instanceof Reduced;
  }
  function reduce(f, arg1, arg2) {
    f = toFn(f);
    let coll, val;
    if (arguments.length === 2) {
      const iter = iterable(arg1)[Symbol.iterator]();
      const vd = iter.next();
      if (vd.done) {
        val = f();
      } else {
        val = vd.value;
      }
      coll = iter;
    } else {
      val = arg1;
      coll = iterable(arg2);
    }
    if (val instanceof Reduced) {
      return val.value;
    }
    for (const x of coll) {
      val = f(val, x);
      if (val instanceof Reduced) {
        val = val.value;
        break;
      }
    }
    return val;
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
  var Cons = class {
    constructor(x, coll) {
      this.x = x;
      this.coll = coll;
    }
    *[Symbol.iterator]() {
      yield this.x;
      yield* iterable(this.coll);
    }
  };
  function cons(x, coll) {
    return new Cons(x, coll);
  }
  function map(f, ...colls) {
    f = toFn(f);
    switch (colls.length) {
      case 0:
        return (rf) => {
          return (...args) => {
            switch (args.length) {
              case 0: {
                return rf();
              }
              case 1: {
                return rf(args[0]);
              }
              case 2: {
                return rf(args[0], f(args[1]));
              }
              default: {
                return rf(args[0], f(...args.slice(1)));
              }
            }
          };
        };
      case 1:
        return lazy(function* () {
          for (const x of iterable(colls[0])) {
            yield f(x);
          }
        });
      default:
        return lazy(function* () {
          const iters = colls.map((coll) => es6_iterator(iterable(coll)));
          while (true) {
            const args = [];
            for (const i of iters) {
              const nextVal = i.next();
              if (nextVal.done) {
                return;
              }
              args.push(nextVal.value);
            }
            yield f(...args);
          }
        });
    }
  }
  function filter1(pred) {
    return (rf) => {
      return (...args) => {
        switch (args.length) {
          case 0:
            return rf();
          case 1:
            return rf(args[0]);
          case 2: {
            const result = args[0];
            const input = args[1];
            if (truth_(pred(input))) {
              return rf(result, input);
            } else return result;
          }
        }
      };
    };
  }
  function filter(pred, coll) {
    if (arguments.length === 1) {
      return filter1(pred);
    }
    pred = toFn(pred);
    return lazy(function* () {
      for (const x of iterable(coll)) {
        if (truth_(pred(x))) {
          yield x;
        }
      }
    });
  }
  function filterv(pred, coll) {
    return [...filter(pred, coll)];
  }
  function remove(pred, coll) {
    if (arguments.length === 1) {
      return filter1(complement(pred));
    }
    return filter(complement(pred), coll);
  }
  function map_indexed1(f) {
    return (rf) => {
      let i = -1;
      return (...args) => {
        switch (args.length) {
          case 0:
            return rf();
          case 1:
            return rf(args[0]);
          case 2:
            return rf(args[0], f((i = i + 1, i), args[1]));
        }
      };
    };
  }
  function map_indexed(f, coll) {
    f = toFn(f);
    if (arguments.length === 1) {
      return map_indexed1(f);
    }
    return lazy(function* () {
      let idx = 0;
      for (const i of iterable(coll)) {
        yield f(idx, i);
        idx++;
      }
    });
  }
  function not(expr) {
    return !truth_(expr);
  }
  function nil_QMARK_(v) {
    return v == null;
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
  function vector(...args) {
    return args;
  }
  function vector_QMARK_(x) {
    return typeConst(x) === ARRAY_TYPE;
  }
  function mapv(...args) {
    if (args.length === 2) {
      const [_f, coll] = args;
      const f = toFn(_f);
      const iter = iterable(coll);
      if (Array.isArray(iter)) {
        const ret2 = new Array(iter.length);
        for (var i = 0; i < iter.length; i++) {
          ret2[i] = f(iter[i]);
        }
        return ret2;
      } else {
        var ret = [];
        for (const x of iter) {
          ret.push(f(x));
        }
        return ret;
      }
    }
    return [...map(...args)];
  }
  function vec(x) {
    if (array_QMARK_(x)) {
      return x;
    }
    return [...iterable(x)];
  }
  function set(coll) {
    return new Set(iterable(coll));
  }
  var IApply__apply = Symbol("IApply__apply");
  function apply(f, ...args) {
    f = toFn(f);
    const xs = args.slice(0, args.length - 1);
    const coll = iterable(args[args.length - 1]);
    const af = f[IApply__apply];
    if (af) {
      return af(...xs, coll);
    }
    return f(...xs, ...coll);
  }
  function complement(f) {
    f = toFn(f);
    return (...args) => not(f(...args));
  }
  var List = class extends Array {
    constructor(...args) {
      super();
      this.push(...args);
    }
  };
  function list(...args) {
    return new List(...args);
  }
  function array_QMARK_(x) {
    return Array.isArray(x);
  }
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
  function mapcat(f, ...colls) {
    if (colls.length === 0) {
      return comp(map(f), cat);
    } else {
      const mapped = map(f, ...colls);
      return concat1(mapped);
    }
  }
  function identity(x) {
    return x;
  }
  function empty(coll) {
    const type = typeConst(coll);
    if (type != null) {
      return emptyOfType(type);
    } else {
      throw new Error(`Can't create empty of ${typeof coll}`);
    }
  }
  function merge(...args) {
    const firstArg = args[0];
    let obj;
    if (firstArg === null || firstArg === void 0) {
      obj = {};
    } else {
      obj = into(empty(firstArg), firstArg);
    }
    return conj_BANG_(obj, ...args.slice(1));
  }
  function into(...args) {
    let to, xform, from, c, rf;
    switch (args.length) {
      case 0:
        return [];
      case 1:
        return args[0];
      case 2:
        return conj(args[0] ?? [], ...iterable(args[1]));
      case 3:
        to = args[0];
        xform = args[1];
        from = args[2];
        c = copy(to);
        rf = (coll, v) => {
          if (v === void 0) {
            return coll;
          }
          return conj_BANG_(coll, v);
        };
        return transduce(xform, rf, c, from);
      default:
        throw TypeError(`Invalid arity call of into: ${args.length}`);
    }
  }
  function repeat(...args) {
    if (args.length == 0 || args.length > 2) {
      throw new Error(`Invalid arity: ${args.length}`);
    }
    return {
      [IIterable]: true,
      [IIterable__iterator]: args.length == 1 ? function* () {
        const x = args[0];
        while (true) yield x;
      } : function* () {
        const [n, x] = args;
        for (var i = 0; i < n; i++) yield x;
      }
    };
  }
  function ensure_reduced(x) {
    if (reduced_QMARK_(x)) {
      return x;
    } else {
      return reduced(x);
    }
  }
  function take1(n) {
    return (rf) => {
      let na = n;
      return (...args) => {
        const al = args.length;
        if (al === 0) {
          return rf();
        }
        if (al === 1) {
          const result = args[0];
          return rf(result);
        }
        if (al === 2) {
          let result = args[0];
          const input = args[1];
          const n2 = na;
          const nn = (na = na - 1, na);
          if (n2 > 0) {
            result = rf(result, input);
          }
          if (!(nn > 0)) {
            return ensure_reduced(result);
          } else {
            return result;
          }
        }
      };
    };
  }
  function take(n, coll) {
    if (arguments.length === 1) {
      return take1(n);
    }
    return lazy(function* () {
      let i = n - 1;
      for (const x of iterable(coll)) {
        if (i-- >= 0) {
          yield x;
        }
        if (i < 0) {
          return;
        }
      }
    });
  }
  function drop1(n) {
    return (rf) => {
      let na = n;
      return (...args) => {
        const al = args.length;
        if (al === 0) {
          return rf();
        }
        if (al === 1) {
          return rf(args[0]);
        }
        if (al === 2) {
          const result = args[0];
          const input = args[1];
          const n2 = na;
          na--;
          if (n2 > 0) {
            return result;
          } else return rf(result, input);
        }
      };
    };
  }
  function drop(n, xs) {
    if (arguments.length === 1) return drop1(n);
    return lazy(function* () {
      const iter = _iterator(iterable(xs));
      for (let x = 0; x < n; x++) {
        iter.next();
      }
      yield* iter;
    });
  }
  function distinct1() {
    return (rf) => {
      const seen = /* @__PURE__ */ new Set();
      return (...args) => {
        const al = args.length;
        if (al === 0) return rf();
        if (al === 1) return rf(args[0]);
        if (al === 2) {
          const result = args[0];
          const input = args[1];
          if (seen.has(input)) return result;
          seen.add(input);
          return rf(result, input);
        }
      };
    };
  }
  function distinct(coll) {
    if (arguments.length === 0) return distinct1();
    return lazy(function* () {
      const seen = /* @__PURE__ */ new Set();
      for (const x of iterable(coll)) {
        if (!seen.has(x)) yield x;
        seen.add(x);
      }
      return;
    });
  }
  function get_in(coll, path, orElse) {
    let entry = coll;
    for (const item of path) {
      entry = get(entry, item);
    }
    if (entry === void 0) return orElse;
    return entry;
  }
  function every_QMARK_(pred, coll) {
    pred = toFn(pred);
    for (const x of iterable(coll)) {
      if (!pred(x)) return false;
    }
    return true;
  }
  function keep1(pred) {
    return (rf) => {
      return (...args) => {
        const al = args.length;
        if (al === 0) return rf();
        if (al === 1) return rf(args[0]);
        if (al === 2) {
          const result = args[0];
          const input = args[1];
          const v = pred(input);
          if (v == null) return result;
          return rf(result, v);
        }
      };
    };
  }
  function keep(pred, coll) {
    pred = toFn(pred);
    if (arguments.length === 1) return keep1(pred);
    return lazy(function* () {
      for (const o of iterable(coll)) {
        const res = pred(o);
        if (truth_(res)) yield res;
      }
    });
  }
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
  function some(pred, coll) {
    pred = toFn(pred);
    for (const o of iterable(coll)) {
      const res = pred(o);
      if (truth_(res)) return res;
    }
    return void 0;
  }
  function empty_QMARK_(coll) {
    return seq(coll) ? false : true;
  }
  function count(coll) {
    if (!coll) return 0;
    const len = coll.length || coll.size;
    if (typeof len === "number") {
      return len;
    }
    let ret = 0;
    for (const _ of iterable(coll)) {
      ret++;
    }
    return ret;
  }
  function true_QMARK_(x) {
    return x === true;
  }
  function false_QMARK_(x) {
    return x === false;
  }
  function boolean$(x) {
    return !!x;
  }
  function reduce_kv(f, init, m) {
    if (!m) {
      return init;
    }
    var ret = init;
    for (const o of iterable(m)) {
      ret = f(ret, o[0], o[1]);
    }
    return ret;
  }
  function max(x, y, ...more) {
    if (y == void 0) {
      return x;
    }
    return Math.max(x, y, ...more);
  }
  function min(x, y, ...more) {
    if (y == void 0) {
      return x;
    }
    return Math.min(x, y, ...more);
  }
  function map_QMARK_(coll) {
    if (coll == null) return false;
    if (isObj(coll)) return true;
    if (coll instanceof Map) return true;
    return false;
  }
  function into_array(type, aseq) {
    const theSeq = aseq || type;
    return vec(theSeq);
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
  function subs(s, start, end) {
    return s.substring(start, end);
  }
  function fn_QMARK_(x) {
    return "function" === typeof x;
  }
  function number_QMARK_(x) {
    return typeof x == "number";
  }
  function keys(obj) {
    if (obj == null) return;
    const t = typeConst(obj);
    switch (t) {
      case OBJECT_TYPE:
        return Object.keys(obj);
      case MAP_TYPE:
        return Array.from(obj.keys());
    }
  }
  function string_QMARK_(s) {
    return typeof s === "string";
  }
  var _metaSym = Symbol("meta");
  function mod(x, y) {
    return (x % y + y) % y;
  }
  function transduce(xform, ...args) {
    switch (args.length) {
      case 2: {
        const f = args[0];
        const coll = args[1];
        return transduce(xform, f, f(), coll);
      }
      default: {
        let f = args[0];
        const init = args[1];
        const coll = args[2];
        f = xform(f);
        const ret = reduce(f, init, coll);
        return f(ret);
      }
    }
  }
  function not_empty(x) {
    const isSeq = seq(x);
    if (isSeq) {
      return x;
    } else return null;
  }
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
  function preserving_reduced(rf) {
    return (a1, a2) => {
      const ret = rf(a1, a2);
      if (reduced_QMARK_(ret)) {
        return reduced(ret);
      } else return ret;
    };
  }
  function cat(rf) {
    rf = preserving_reduced(rf);
    return (...args) => {
      switch (args.length) {
        case 0:
          return rf();
        case 1:
          return rf(args[0]);
        case 2:
          return reduce(rf, args[0], args[1]);
      }
    };
  }
  function update_vals(m, f) {
    const m2 = empty(m);
    const assocFn = getAssocMut(m) || assoc_BANG_;
    reduce_kv(
      (acc, k, v) => {
        return assocFn(acc, k, f(v));
      },
      m2,
      m
    );
    return m2;
  }
  function clj__GT_js_(x, seen) {
    if (seen.has(x)) return x;
    seen.add(x);
    if (map_QMARK_(x)) {
      return update_vals(x, (x2) => clj__GT_js_(x2, seen));
    }
    const tc = typeConst(x);
    if (tc && tc != OBJECT_TYPE) {
      return mapv((x2) => clj__GT_js_(x2, seen), x);
    }
    return x;
  }
  function clj__GT_js(x) {
    return clj__GT_js_(x, /* @__PURE__ */ new Set());
  }

  // .compiled/color_picker.mjs
  var clamp = function(v, lo, hi) {
    return Math.max(lo, Math.min(v, hi));
  };
  var fmt_num = function(n) {
    if (truth_(isFinite(n))) {
      return `${Math.round(n * 1e3) / 1e3}`;
    } else {
      return "0";
    }
    ;
  };
  var hsv__GT_rgb = function(h, s, v) {
    const c1 = v * s;
    const h_SINGLEQUOTE_2 = mod(h, 360) / 60;
    const x3 = c1 * (1 - Math.abs(mod(h_SINGLEQUOTE_2, 2) - 1));
    const m4 = v - c1;
    const rgb5 = h_SINGLEQUOTE_2 < 1 ? [c1, x3, 0] : h_SINGLEQUOTE_2 < 2 ? [x3, c1, 0] : h_SINGLEQUOTE_2 < 3 ? [0, c1, x3] : h_SINGLEQUOTE_2 < 4 ? [0, x3, c1] : h_SINGLEQUOTE_2 < 5 ? [x3, 0, c1] : "else" ? [c1, 0, x3] : null;
    return [Math.round(255 * (rgb5[0] + m4)), Math.round(255 * (rgb5[1] + m4)), Math.round(255 * (rgb5[2] + m4))];
  };
  var hue_of = function(r, g, b, mx, d) {
    const h1 = d === 0 ? 0 : _EQ_(mx, r) ? 60 * mod((g - b) / d, 6) : _EQ_(mx, g) ? 60 * ((b - r) / d + 2) : "else" ? 60 * ((r - g) / d + 4) : null;
    if (h1 < 0) {
      return h1 + 360;
    } else {
      return h1;
    }
    ;
  };
  var rgb__GT_hsv = function(r, g, b) {
    const r1 = r / 255;
    const g2 = g / 255;
    const b3 = b / 255;
    const mx4 = Math.max(r1, g2, b3);
    const mn5 = Math.min(r1, g2, b3);
    const d6 = mx4 - mn5;
    return [hue_of(r1, g2, b3, mx4, d6), mx4 === 0 ? 0 : d6 / mx4, mx4];
  };
  var linear = function(c) {
    const c1 = c / 255;
    if (c1 <= 0.04045) {
      return c1 / 12.92;
    } else {
      return Math.pow((c1 + 0.055) / 1.055, 2.4);
    }
    ;
  };
  var rgb__GT_oklch = function(r, g, b) {
    const r1 = linear(r);
    const g2 = linear(g);
    const b3 = linear(b);
    const l4 = Math.cbrt(0.4122214708 * r1 + 0.5363325363 * g2 + 0.0514459929 * b3);
    const m5 = Math.cbrt(0.2119034982 * r1 + 0.6806995451 * g2 + 0.1073969566 * b3);
    const s6 = Math.cbrt(0.0883024619 * r1 + 0.2817188376 * g2 + 0.6299787005 * b3);
    const L7 = 0.2104542553 * l4 + 0.793617785 * m5 + -0.0040720468 * s6;
    const oa8 = 1.9779984951 * l4 + -2.428592205 * m5 + 0.4505937099 * s6;
    const ob9 = 0.0259040371 * l4 + 0.7827717662 * m5 + -0.808675766 * s6;
    const h10 = 180 / Math.PI * Math.atan2(ob9, oa8);
    return [L7, Math.sqrt(oa8 * oa8 + ob9 * ob9), h10 < 0 ? h10 + 360 : h10];
  };
  var rgb__GT_hsl = function(r, g, b) {
    const r1 = r / 255;
    const g2 = g / 255;
    const b3 = b / 255;
    const mx4 = Math.max(r1, g2, b3);
    const mn5 = Math.min(r1, g2, b3);
    const d6 = mx4 - mn5;
    const l7 = (mx4 + mn5) / 2;
    return [hue_of(r1, g2, b3, mx4, d6), d6 === 0 ? 0 : d6 / (1 - Math.abs(2 * l7 - 1)), l7];
  };
  var probe = atom(null);
  var probe_ctx = function() {
    const or__23426__auto__1 = deref(probe);
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const c2 = document.createElement("canvas");
      c2.width = 1;
      c2.height = 1;
      const ctx3 = c2.getContext("2d", { "willReadFrequently": true });
      reset_BANG_(probe, ctx3);
      return ctx3;
    }
    ;
  };
  var parse_rgba = function(s) {
    const s1 = `${(() => {
      const or__23426__auto__2 = s;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return "";
      }
      ;
    })() ?? ""}`.trim();
    if (truth_(!_EQ_("", s1) && CSS.supports("color", s1))) {
      const ctx3 = probe_ctx();
      ctx3.clearRect(0, 0, 1, 1);
      ctx3.fillStyle = s1;
      ctx3.fillRect(0, 0, 1, 1);
      const d4 = ctx3.getImageData(0, 0, 1, 1).data;
      return [d4[0], d4[1], d4[2], d4[3] / 255];
    }
    ;
  };
  var hex2 = function(n) {
    return clamp(Math.round(n), 0, 255).toString(16).padStart(2, "0");
  };
  var compose = function(h, s, v, a, fmt2) {
    const rgb1 = hsv__GT_rgb(h, s, v);
    const r2 = rgb1[0];
    const g3 = rgb1[1];
    const b4 = rgb1[2];
    const translucent_QMARK_5 = a < 0.999;
    const G__5116 = fmt2;
    switch (G__5116) {
      case "oklch":
        const lch8 = rgb__GT_oklch(r2, g3, b4);
        const c9 = lch8[1];
        const h10 = c9 < 5e-4 ? 0 : Math.round(10 * lch8[2]) / 10;
        return `${"oklch("}${fmt_num(lch8[0]) ?? ""}${" "}${fmt_num(c9) ?? ""}${" "}${h10 ?? ""}${(translucent_QMARK_5 ? `${" / "}${fmt_num(a) ?? ""}` : "") ?? ""}${")"}`;
        break;
      case "rgb":
        if (translucent_QMARK_5) {
          return `${"rgba("}${r2 ?? ""}${", "}${g3 ?? ""}${", "}${b4 ?? ""}${", "}${fmt_num(a) ?? ""}${")"}`;
        } else {
          return `${"rgb("}${r2 ?? ""}${", "}${g3 ?? ""}${", "}${b4 ?? ""}${")"}`;
        }
        ;
        break;
      case "hsl":
        const hsl11 = rgb__GT_hsl(r2, g3, b4);
        const hh12 = Math.round(hsl11[0]);
        const ss13 = Math.round(100 * hsl11[1]);
        const ll14 = Math.round(100 * hsl11[2]);
        if (translucent_QMARK_5) {
          return `${"hsla("}${hh12 ?? ""}${", "}${ss13 ?? ""}${"%, "}${ll14 ?? ""}${"%, "}${fmt_num(a) ?? ""}${")"}`;
        } else {
          return `${"hsl("}${hh12 ?? ""}${", "}${ss13 ?? ""}${"%, "}${ll14 ?? ""}${"%)"}`;
        }
        ;
        break;
      default:
        return `${"#"}${hex2(r2) ?? ""}${hex2(g3) ?? ""}${hex2(b4) ?? ""}${(translucent_QMARK_5 ? hex2(a * 255) : "") ?? ""}`;
    }
    ;
  };
  var detect_fmt = function(s) {
    const s1 = `${(() => {
      const or__23426__auto__2 = s;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return "";
      }
      ;
    })() ?? ""}`.trim().toLowerCase();
    if (truth_(s1.startsWith("#"))) {
      return "hex";
    } else {
      if (truth_(s1.startsWith("rgb"))) {
        return "rgb";
      } else {
        if (truth_(s1.startsWith("hsl"))) {
          return "hsl";
        } else {
          if (truth_(s1.startsWith("oklch"))) {
            return "oklch";
          } else {
            return null;
          }
        }
      }
    }
    ;
  };
  var q = function(el2, sel) {
    return el2.querySelector(sel);
  };
  var state = function(el2) {
    return el2["__uiColor"];
  };
  var formats = function(el2) {
    return (() => {
      const or__23426__auto__1 = el2.getAttribute("data-formats");
      if (truth_(or__23426__auto__1)) {
        return or__23426__auto__1;
      } else {
        return "oklch hex rgb hsl";
      }
      ;
    })().split(" ");
  };
  var adopt_BANG_ = function(el2, value) {
    const st1 = (() => {
      const or__23426__auto__2 = state(el2);
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        const o3 = { "h": 0, "s": 0, "v": 0, "a": 1, "fmt": null, "value": null };
        el2["__uiColor"] = o3;
        return o3;
      }
      ;
    })();
    const fs4 = formats(el2);
    const f5 = detect_fmt(value);
    const cur6 = st1["fmt"];
    const temp__23062__auto__7 = parse_rgba(value);
    if (truth_(temp__23062__auto__7)) {
      const rgba8 = temp__23062__auto__7;
      const hsv9 = rgb__GT_hsv(rgba8[0], rgba8[1], rgba8[2]);
      if (hsv9[1] > 1e-4) {
        st1["h"] = hsv9[0];
      }
      ;
      if (hsv9[2] > 1e-4) {
        st1["s"] = hsv9[1];
      }
      ;
      st1["v"] = hsv9[2];
      st1["a"] = rgba8[3];
    }
    ;
    st1["fmt"] = truth_((() => {
      const and__23442__auto__10 = f5;
      if (truth_(and__23442__auto__10)) {
        return fs4.includes(f5);
      } else {
        return and__23442__auto__10;
      }
      ;
    })()) ? f5 : truth_((() => {
      const and__23442__auto__11 = cur6;
      if (truth_(and__23442__auto__11)) {
        return fs4.includes(cur6);
      } else {
        return and__23442__auto__11;
      }
      ;
    })()) ? cur6 : "else" ? fs4[0] : null;
    return st1;
  };
  var paint_BANG_ = function(el2) {
    const st1 = state(el2);
    const h2 = st1["h"];
    const s3 = st1["s"];
    const v4 = st1["v"];
    const rgb5 = hsv__GT_rgb(h2, s3, v4);
    const solid6 = `${"rgb("}${rgb5[0] ?? ""}${", "}${rgb5[1] ?? ""}${", "}${rgb5[2] ?? ""}${")"}`;
    const temp__23062__auto__7 = q(el2, ".color-picker-plane");
    if (truth_(temp__23062__auto__7)) {
      const plane8 = temp__23062__auto__7;
      plane8.style.background = `${"linear-gradient(to top, #000, rgba(0,0,0,0)),"}${"linear-gradient(to right, #fff, hsl("}${Math.round(h2) ?? ""}${", 100%, 50%))"}`;
      plane8.setAttribute("aria-valuetext", (() => {
        const or__23426__auto__9 = st1["value"];
        if (truth_(or__23426__auto__9)) {
          return or__23426__auto__9;
        } else {
          return "";
        }
        ;
      })());
    }
    ;
    const temp__23062__auto__10 = q(el2, ".color-picker-marker");
    if (truth_(temp__23062__auto__10)) {
      const marker11 = temp__23062__auto__10;
      marker11.style.left = `${100 * s3}${"%"}`;
      marker11.style.top = `${100 * (1 - v4)}${"%"}`;
      marker11.style.background = solid6;
    }
    ;
    const temp__23062__auto__12 = q(el2, "[data-ui-color-hue]");
    if (truth_(temp__23062__auto__12)) {
      const hue13 = temp__23062__auto__12;
      hue13.value = `${h2 ?? ""}`;
    }
    ;
    const temp__23062__auto__14 = q(el2, "[data-ui-color-alpha]");
    if (truth_(temp__23062__auto__14)) {
      const op15 = temp__23062__auto__14;
      op15.value = `${st1["a"] ?? ""}`;
      op15.style.setProperty("--color-picker-solid", solid6);
    }
    ;
    return el2.querySelectorAll("[data-ui-color-format]").forEach((function(b) {
      return b.setAttribute("data-active", _EQ_(b.getAttribute("data-ui-color-format"), st1["fmt"]) ? "true" : "false");
    }));
  };
  var show_value_BANG_ = function(el2, value) {
    state(el2)["value"] = value;
    const temp__23062__auto__1 = q(el2, "[data-ui-color-text]");
    if (truth_(temp__23062__auto__1)) {
      const t2 = temp__23062__auto__1;
      return t2.value = value;
    }
    ;
  };
  var sync_BANG_ = function(el2) {
    const v1 = (() => {
      const or__23426__auto__2 = el2.getAttribute("data-value");
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return "";
      }
      ;
    })();
    const st3 = state(el2);
    if (truth_((() => {
      const or__23426__auto__4 = st3 == null;
      if (or__23426__auto__4) {
        return or__23426__auto__4;
      } else {
        return !_EQ_(v1, st3["value"]);
      }
      ;
    })())) {
      adopt_BANG_(el2, v1);
      show_value_BANG_(el2, v1);
      return paint_BANG_(el2);
    }
    ;
  };
  var fire_BANG_ = function(input, type) {
    return input.dispatchEvent(new Event(type, { "bubbles": true }));
  };
  var emit_BANG_ = function(el2, commit_QMARK_) {
    const st1 = state(el2);
    const v2 = compose(st1["h"], st1["s"], st1["v"], st1["a"], st1["fmt"]);
    show_value_BANG_(el2, v2);
    el2.setAttribute("data-value", v2);
    paint_BANG_(el2);
    const temp__23062__auto__3 = q(el2, "[data-ui-color-value]");
    if (truth_(temp__23062__auto__3)) {
      const input4 = temp__23062__auto__3;
      input4.value = v2;
      fire_BANG_(input4, "input");
      if (truth_(commit_QMARK_)) {
        return fire_BANG_(input4, "change");
      }
      ;
    }
    ;
  };
  var commit_BANG_ = function(el2) {
    const temp__23062__auto__1 = q(el2, "[data-ui-color-value]");
    if (truth_(temp__23062__auto__1)) {
      const input2 = temp__23062__auto__1;
      return fire_BANG_(input2, "change");
    }
    ;
  };
  var closest = function(target, sel) {
    if (truth_((() => {
      const and__23442__auto__1 = target;
      if (truth_(and__23442__auto__1)) {
        return target.closest;
      } else {
        return and__23442__auto__1;
      }
      ;
    })())) {
      return target.closest(sel);
    }
    ;
  };
  var live_picker = function(node) {
    const temp__23062__auto__1 = closest(node, "[data-ui-color-picker]");
    if (truth_(temp__23062__auto__1)) {
      const el2 = temp__23062__auto__1;
      if (truth_(el2.hasAttribute("data-ui-color-disabled"))) {
        return null;
      } else {
        sync_BANG_(el2);
        return el2;
      }
      ;
    }
    ;
  };
  var drag = atom(null);
  var plane_at_BANG_ = function(el2, plane, e) {
    const rect1 = plane.getBoundingClientRect();
    const st2 = state(el2);
    st2["s"] = clamp((e.clientX - rect1.left) / rect1.width, 0, 1);
    st2["v"] = 1 - clamp((e.clientY - rect1.top) / rect1.height, 0, 1);
    return emit_BANG_(el2, false);
  };
  var on_pointerdown = function(e) {
    const temp__23062__auto__1 = closest(e.target, ".color-picker-plane");
    if (truth_(temp__23062__auto__1)) {
      const plane2 = temp__23062__auto__1;
      const temp__23062__auto__3 = live_picker(plane2);
      if (truth_(temp__23062__auto__3)) {
        const el4 = temp__23062__auto__3;
        e.preventDefault();
        plane2.focus();
        plane2.setPointerCapture(e.pointerId);
        reset_BANG_(drag, [el4, plane2]);
        return plane_at_BANG_(el4, plane2, e);
      }
      ;
    }
    ;
  };
  var on_pointermove = function(e) {
    const temp__23062__auto__1 = deref(drag);
    if (truth_(temp__23062__auto__1)) {
      const d2 = temp__23062__auto__1;
      return plane_at_BANG_(d2[0], d2[1], e);
    }
    ;
  };
  var on_pointerup = function(_) {
    const temp__23062__auto__1 = deref(drag);
    if (truth_(temp__23062__auto__1)) {
      const d2 = temp__23062__auto__1;
      reset_BANG_(drag, null);
      return commit_BANG_(d2[0]);
    }
    ;
  };
  var on_input = function(e) {
    const t1 = e.target;
    const hue_QMARK_2 = (() => {
      const and__23442__auto__3 = t1.hasAttribute;
      if (truth_(and__23442__auto__3)) {
        return t1.hasAttribute("data-ui-color-hue");
      } else {
        return and__23442__auto__3;
      }
      ;
    })();
    const alpha_QMARK_4 = (() => {
      const and__23442__auto__5 = t1.hasAttribute;
      if (truth_(and__23442__auto__5)) {
        return t1.hasAttribute("data-ui-color-alpha");
      } else {
        return and__23442__auto__5;
      }
      ;
    })();
    if (truth_((() => {
      const or__23426__auto__6 = hue_QMARK_2;
      if (truth_(or__23426__auto__6)) {
        return or__23426__auto__6;
      } else {
        return alpha_QMARK_4;
      }
      ;
    })())) {
      const temp__23062__auto__7 = live_picker(t1);
      if (truth_(temp__23062__auto__7)) {
        const el8 = temp__23062__auto__7;
        state(el8)[truth_(hue_QMARK_2) ? "h" : "a"] = parseFloat(t1.value);
        return emit_BANG_(el8, false);
      }
      ;
    }
    ;
  };
  var commit_text_BANG_ = function(el2, t) {
    if (truth_(parse_rgba(t.value))) {
      adopt_BANG_(el2, t.value.trim());
      return emit_BANG_(el2, true);
    } else {
      return t.value = state(el2)["value"];
    }
    ;
  };
  var on_change = function(e) {
    const t1 = e.target;
    if (truth_(t1.hasAttribute)) {
      if (truth_((() => {
        const or__23426__auto__2 = t1.hasAttribute("data-ui-color-hue");
        if (truth_(or__23426__auto__2)) {
          return or__23426__auto__2;
        } else {
          return t1.hasAttribute("data-ui-color-alpha");
        }
        ;
      })())) {
        const temp__23062__auto__3 = live_picker(t1);
        if (truth_(temp__23062__auto__3)) {
          const el4 = temp__23062__auto__3;
          return commit_BANG_(el4);
        }
        ;
      } else {
        if (truth_(t1.hasAttribute("data-ui-color-text"))) {
          const temp__23062__auto__5 = live_picker(t1);
          if (truth_(temp__23062__auto__5)) {
            const el6 = temp__23062__auto__5;
            return commit_text_BANG_(el6, t1);
          }
          ;
        } else {
          return null;
        }
      }
      ;
    }
    ;
  };
  var on_click = function(e) {
    const temp__23062__auto__1 = closest(e.target, "[data-ui-color-format]");
    if (truth_(temp__23062__auto__1)) {
      const b2 = temp__23062__auto__1;
      const temp__23062__auto__3 = live_picker(b2);
      if (truth_(temp__23062__auto__3)) {
        const el4 = temp__23062__auto__3;
        state(el4)["fmt"] = b2.getAttribute("data-ui-color-format");
        return emit_BANG_(el4, true);
      }
      ;
    }
    ;
  };
  var arrow_deltas = { "ArrowLeft": ["s", -1], "ArrowRight": ["s", 1], "ArrowDown": ["v", -1], "ArrowUp": ["v", 1] };
  var on_keydown = function(e) {
    const temp__23062__auto__1 = arrow_deltas[e.key];
    if (truth_(temp__23062__auto__1)) {
      const delta2 = temp__23062__auto__1;
      const temp__23062__auto__3 = closest(e.target, ".color-picker-plane");
      if (truth_(temp__23062__auto__3)) {
        const plane4 = temp__23062__auto__3;
        const temp__23062__auto__5 = live_picker(plane4);
        if (truth_(temp__23062__auto__5)) {
          const el6 = temp__23062__auto__5;
          e.preventDefault();
          const st7 = state(el6);
          const k8 = delta2[0];
          const step9 = truth_(e.shiftKey) ? 0.1 : 0.01;
          st7[k8] = clamp(st7[k8] + step9 * delta2[1], 0, 1);
          return emit_BANG_(el6, true);
        }
        ;
      }
      ;
    }
    ;
  };
  var mk = function(tag, cls) {
    const e1 = document.createElement(tag);
    if (truth_(cls)) {
      e1.className = cls;
    }
    ;
    return e1;
  };
  var track_row = function(label, cls, data_attr, mx, step) {
    const row1 = mk("label", "color-picker-track-row");
    const span2 = mk("span", null);
    const input3 = mk("input", `${"color-picker-track "}${cls ?? ""}`);
    span2.textContent = label;
    input3.type = "range";
    input3.min = "0";
    input3.max = `${mx ?? ""}`;
    input3.step = `${step ?? ""}`;
    input3.setAttribute(data_attr, "true");
    row1.append(span2, input3);
    return row1;
  };
  var all_formats = ["oklch", "hex", "rgb", "hsl"];
  var format_labels = { "oklch": "OKLCH", "hex": "Hex", "rgb": "RGB", "hsl": "HSL" };
  var create = function(opts) {
    const opts1 = (() => {
      const or__23426__auto__2 = opts;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return {};
      }
      ;
    })();
    const fs3 = (() => {
      const or__23426__auto__4 = opts1["formats"];
      if (truth_(or__23426__auto__4)) {
        return or__23426__auto__4;
      } else {
        return all_formats;
      }
      ;
    })().filter((function(f) {
      return format_labels[f];
    }));
    const fs5 = fs3.length > 0 ? fs3 : all_formats;
    const el6 = mk("div", "color-picker");
    const plane7 = mk("div", "color-picker-plane");
    const tracks8 = mk("div", "color-picker-tracks");
    const text9 = mk("input", "color-picker-text");
    const value10 = mk("input", null);
    el6.setAttribute("data-ui-color-picker", "true");
    el6.setAttribute("data-value", (() => {
      const or__23426__auto__11 = opts1["value"];
      if (truth_(or__23426__auto__11)) {
        return or__23426__auto__11;
      } else {
        return "";
      }
      ;
    })());
    el6.setAttribute("data-formats", fs5.join(" "));
    if (truth_(opts1["alpha"])) {
      el6.setAttribute("data-alpha", "true");
    }
    ;
    if (fs5.length > 1) {
      const group12 = mk("div", "color-picker-formats");
      group12.setAttribute("role", "group");
      group12.setAttribute("aria-label", "Color format");
      fs5.forEach((function(f) {
        const b13 = mk("button", "color-picker-format");
        b13.type = "button";
        b13.textContent = format_labels[f];
        b13.setAttribute("data-ui-color-format", f);
        return group12.append(b13);
      }));
      el6.append(group12);
    }
    ;
    plane7.setAttribute("tabindex", "0");
    plane7.setAttribute("role", "slider");
    plane7.setAttribute("aria-label", "Saturation and brightness");
    plane7.append(mk("span", "color-picker-marker"));
    tracks8.append(track_row("Hue", "color-picker-hue", "data-ui-color-hue", 360, 1));
    if (truth_(opts1["alpha"])) {
      tracks8.append(track_row("Opacity", "color-picker-opacity", "data-ui-color-alpha", 1, 0.01));
    }
    ;
    text9.type = "text";
    text9.setAttribute("spellcheck", "false");
    text9.setAttribute("autocomplete", "off");
    text9.setAttribute("aria-label", "Color value");
    text9.setAttribute("data-ui-color-text", "true");
    value10.type = "hidden";
    value10.setAttribute("data-ui-color-value", "true");
    value10.value = (() => {
      const or__23426__auto__14 = opts1["value"];
      if (truth_(or__23426__auto__14)) {
        return or__23426__auto__14;
      } else {
        return "";
      }
      ;
    })();
    el6.append(plane7, tracks8, text9, value10);
    sync_BANG_(el6);
    return el6;
  };
  var set_value_BANG_ = function(el2, value) {
    el2.setAttribute("data-value", (() => {
      const or__23426__auto__1 = value;
      if (truth_(or__23426__auto__1)) {
        return or__23426__auto__1;
      } else {
        return "";
      }
      ;
    })());
    const temp__23062__auto__2 = q(el2, "[data-ui-color-value]");
    if (truth_(temp__23062__auto__2)) {
      const input3 = temp__23062__auto__2;
      input3.value = (() => {
        const or__23426__auto__4 = value;
        if (truth_(or__23426__auto__4)) {
          return or__23426__auto__4;
        } else {
          return "";
        }
        ;
      })();
    }
    ;
    return sync_BANG_(el2);
  };
  var scan_BANG_ = function() {
    return document.querySelectorAll("[data-ui-color-picker]").forEach((function(el2) {
      return sync_BANG_(el2);
    }));
  };
  var on_mutations = function(records) {
    const added_QMARK_1 = atom(false);
    records.forEach((function(r) {
      if ("attributes" === r.type) {
        if (truth_(r.target.hasAttribute("data-ui-color-picker"))) {
          return sync_BANG_(r.target);
        }
      } else {
        if (r.addedNodes.length > 0) {
          return reset_BANG_(added_QMARK_1, true);
        }
      }
      ;
    }));
    if (truth_(deref(added_QMARK_1))) {
      return scan_BANG_();
    }
    ;
  };
  var init_BANG_ = function() {
    for (let G__1 of iterable([["pointerdown", on_pointerdown], ["pointermove", on_pointermove], ["pointerup", on_pointerup], ["pointercancel", on_pointerup], ["input", on_input], ["change", on_change], ["click", on_click], ["keydown", on_keydown]])) {
      const vec__25 = G__1;
      const type6 = nth(vec__25, 0, null);
      const f7 = nth(vec__25, 1, null);
      document.addEventListener(type6, f7, true);
    }
    ;
    scan_BANG_();
    return new MutationObserver(on_mutations).observe(document.documentElement, { "childList": true, "subtree": true, "attributes": true, "attributeFilter": ["data-value"] });
  };
  window["__uiColorPicker"] = { "create": create, "setValue": set_value_BANG_ };
  if ("loading" === document.readyState) {
    document.addEventListener("DOMContentLoaded", init_BANG_);
  } else {
    init_BANG_();
  }

  // .compiled/command.mjs
  var items = function(dialog) {
    return Array.from(dialog.querySelectorAll(".command-item"));
  };
  var item_order = function(el2) {
    return Number((() => {
      const or__23426__auto__1 = el2.style.order;
      if (truth_(or__23426__auto__1)) {
        return or__23426__auto__1;
      } else {
        return 0;
      }
      ;
    })());
  };
  var visible_items = function(dialog) {
    const vis1 = items(dialog).filter((function(el2) {
      return not(el2.hidden) && not(el2.disabled);
    }));
    const container2 = (function(el2) {
      const or__23426__auto__3 = el2.closest(".command-group");
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return dialog;
      }
      ;
    });
    const containers4 = [];
    vis1.forEach((function(el2) {
      const c5 = container2(el2);
      if (truth_(containers4.includes(c5))) {
        return null;
      } else {
        return containers4.push(c5);
      }
      ;
    }));
    containers4.sort((function(a, b) {
      return item_order(a) - item_order(b);
    }));
    return containers4.flatMap((function(c) {
      return vis1.filter((function(el2) {
        return c === container2(el2);
      })).sort((function(a, b) {
        return item_order(a) - item_order(b);
      }));
    }));
  };
  var item_text = function(el2) {
    const v1 = el2.dataset.commandValue;
    return (!(v1 == null) ? v1 : (() => {
      const or__23426__auto__2 = el2.textContent;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return "";
      }
      ;
    })()).toLowerCase();
  };
  var item_label = function(el2) {
    const lbl1 = el2.querySelector(".command-item-label");
    return (() => {
      const or__23426__auto__2 = (() => {
        const and__23442__auto__3 = lbl1;
        if (truth_(and__23442__auto__3)) {
          return lbl1.textContent;
        } else {
          return and__23442__auto__3;
        }
        ;
      })();
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        const or__23426__auto__4 = el2.textContent;
        if (truth_(or__23426__auto__4)) {
          return or__23426__auto__4;
        } else {
          return "";
        }
        ;
      }
      ;
    })().toLowerCase();
  };
  var match_score = function(el2, q3) {
    const label1 = item_label(el2);
    if (_EQ_(label1, q3)) {
      return 0;
    } else {
      if (truth_(label1.startsWith(q3))) {
        return 1;
      } else {
        if (truth_(label1.split(/[^a-z0-9]+/).some((function(w) {
          return w.startsWith(q3);
        })))) {
          return 2;
        } else {
          if (truth_(label1.includes(q3))) {
            return 3;
          } else {
            if ("else") {
              return 4;
            } else {
              return null;
            }
          }
        }
      }
    }
    ;
  };
  var active_item = function(dialog) {
    return dialog.querySelector(".command-item--active");
  };
  var set_active_BANG_ = (() => {
    const f512 = (function(...args513) {
      const G__5141 = args513.length;
      switch (G__5141) {
        case 2:
          return f512.cljs$core$IFn$_invoke$arity$2(args513[0], args513[1]);
          break;
        case 3:
          return f512.cljs$core$IFn$_invoke$arity$3(args513[0], args513[1], args513[2]);
          break;
        default:
          throw new Error(`${"Invalid arity: "}${args513.length ?? ""}`);
      }
      ;
    });
    f512.cljs$core$IFn$_invoke$arity$2 = (function(dialog, el2) {
      return set_active_BANG_(dialog, el2, true);
    });
    f512.cljs$core$IFn$_invoke$arity$3 = (function(dialog, el2, scroll_QMARK_) {
      const prev3 = active_item(dialog);
      if (truth_(prev3)) {
        prev3.classList.remove("command-item--active");
      }
      ;
      if (truth_(el2)) {
        el2.classList.add("command-item--active");
        if (truth_((() => {
          const and__23442__auto__4 = scroll_QMARK_;
          if (truth_(and__23442__auto__4)) {
            return el2.scrollIntoView;
          } else {
            return and__23442__auto__4;
          }
          ;
        })())) {
          return el2.scrollIntoView({ "block": "nearest" });
        }
        ;
      }
      ;
    });
    f512.cljs$lang$maxFixedArity = 3;
    return f512;
  })();
  var move_active_BANG_ = function(dialog, dir) {
    const vis1 = visible_items(dialog);
    const len2 = vis1.length;
    if (len2 > 0) {
      const cur3 = active_item(dialog);
      const idx4 = vis1.indexOf(cur3);
      const next_i5 = dir === "down" ? idx4 < len2 - 1 ? idx4 + 1 : 0 : dir === "up" ? idx4 > 0 ? idx4 - 1 : len2 - 1 : dir === "home" ? 0 : dir === "end" ? len2 - 1 : "else" ? idx4 : null;
      return set_active_BANG_(dialog, vis1[next_i5]);
    }
    ;
  };
  var move_group_BANG_ = function(dialog, dir) {
    const vis1 = visible_items(dialog);
    const len2 = vis1.length;
    if (len2 > 0) {
      const group_of3 = (function(el2) {
        const or__23426__auto__4 = (() => {
          const and__23442__auto__5 = el2;
          if (truth_(and__23442__auto__5)) {
            return el2.closest(".command-group");
          } else {
            return and__23442__auto__5;
          }
          ;
        })();
        if (truth_(or__23426__auto__4)) {
          return or__23426__auto__4;
        } else {
          return dialog;
        }
        ;
      });
      const groups6 = [];
      vis1.forEach((function(el2) {
        const g7 = group_of3(el2);
        if (truth_(groups6.includes(g7))) {
          return null;
        } else {
          return groups6.push(g7);
        }
        ;
      }));
      const glen8 = groups6.length;
      const cur9 = active_item(dialog);
      const gidx10 = groups6.indexOf(group_of3(cur9));
      const next_g11 = dir === "down" ? gidx10 < glen8 - 1 ? gidx10 + 1 : 0 : gidx10 > 0 ? gidx10 - 1 : glen8 - 1;
      const target12 = groups6[next_g11];
      return set_active_BANG_(dialog, vis1.find((function(el2) {
        return group_of3(el2) === target12;
      })));
    }
    ;
  };
  var quick_key_sets = { "letters": "asdfghlqweryiotzxcvbum", "numbers": "1234567890" };
  var quick_keys = function(dialog) {
    const v1 = dialog.dataset.commandQuickNav;
    if (truth_((() => {
      const and__23442__auto__2 = v1;
      if (truth_(and__23442__auto__2)) {
        return !_EQ_(v1, "");
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      const or__23426__auto__3 = quick_key_sets[v1];
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return v1;
      }
      ;
    }
    ;
  };
  var event_key = function(e) {
    const code1 = (() => {
      const or__23426__auto__2 = e.code;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return "";
      }
      ;
    })();
    if (truth_(code1.startsWith("Key"))) {
      return code1.slice(3).toLowerCase();
    } else {
      if (truth_(code1.startsWith("Digit"))) {
        return code1.slice(5);
      } else {
        return null;
      }
    }
    ;
  };
  var hide_hints_BANG_ = function(dialog) {
    if (truth_(dialog.hasAttribute("data-command-hints"))) {
      dialog.removeAttribute("data-command-hints");
      return dialog.querySelectorAll("[data-command-hint]").forEach((function(el2) {
        return el2.removeAttribute("data-command-hint");
      }));
    }
    ;
  };
  var show_hints_BANG_ = function(dialog) {
    const temp__23062__auto__1 = quick_keys(dialog);
    if (truth_(temp__23062__auto__1)) {
      const ks2 = temp__23062__auto__1;
      hide_hints_BANG_(dialog);
      visible_items(dialog).forEach((function(el2, i) {
        if (i < ks2.length) {
          return el2.setAttribute("data-command-hint", ks2[i].toUpperCase());
        }
        ;
      }));
      return dialog.setAttribute("data-command-hints", "");
    }
    ;
  };
  var quick_item = function(dialog, e) {
    const temp__23062__auto__1 = quick_keys(dialog);
    if (truth_(temp__23062__auto__1)) {
      const ks2 = temp__23062__auto__1;
      const k3 = event_key(e);
      const i4 = truth_(k3) ? ks2.indexOf(k3) : -1;
      if (i4 >= 0) {
        return visible_items(dialog)[i4];
      }
      ;
    }
    ;
  };
  var filter_BANG_ = function(dialog, query) {
    const q1 = (() => {
      const or__23426__auto__2 = query;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return "";
      }
      ;
    })().trim().toLowerCase();
    const list3 = dialog.querySelector(".command-list");
    const searching_QMARK_4 = !_EQ_(q1, "");
    items(dialog).forEach((function(el2) {
      const match5 = searching_QMARK_4 ? item_text(el2).includes(q1) : not(el2.dataset.commandSearchOnly);
      el2.hidden = not(match5);
      if (truth_((() => {
        const or__23426__auto__6 = !searching_QMARK_4;
        if (or__23426__auto__6) {
          return or__23426__auto__6;
        } else {
          return not(match5);
        }
        ;
      })())) {
        return el2.style.removeProperty("order");
      } else {
        return el2.style.order = match_score(el2, q1);
      }
      ;
    }));
    if (searching_QMARK_4) {
      list3.classList.add("command-list--searching");
    } else {
      list3.classList.remove("command-list--searching");
    }
    ;
    Array.from(dialog.querySelectorAll(".command-group")).forEach((function(grp) {
      const visible7 = Array.from(grp.querySelectorAll(".command-item")).filter((function(el2) {
        return not(el2.hidden);
      }));
      grp.hidden = visible7.length === 0;
      if (truth_(searching_QMARK_4 && visible7.length > 0)) {
        return grp.style.order = visible7.reduce((function(best, el2) {
          return Math.min(best, item_order(el2));
        }), Infinity);
      } else {
        return grp.style.removeProperty("order");
      }
      ;
    }));
    const vis8 = visible_items(dialog);
    if (vis8.length === 0) {
      list3.classList.add("command-list--empty");
    } else {
      list3.classList.remove("command-list--empty");
    }
    ;
    set_active_BANG_(dialog, vis8.length > 0 ? vis8[0] : null);
    if (truth_(dialog.hasAttribute("data-command-hints"))) {
      return show_hints_BANG_(dialog);
    }
    ;
  };
  var find_dialog = function(id) {
    if (truth_(id)) {
      return document.getElementById(id);
    }
    ;
  };
  var observe_list_BANG_ = function(dialog) {
    const temp__23062__auto__1 = dialog["__cmdListObs"];
    if (truth_(temp__23062__auto__1)) {
      const prev2 = temp__23062__auto__1;
      prev2.disconnect();
    }
    ;
    const list3 = dialog.querySelector(".command-list");
    if (truth_(list3)) {
      const obs4 = new MutationObserver((function(_, _5) {
        const input6 = dialog.querySelector(".command-input");
        return filter_BANG_(dialog, truth_(input6) ? input6.value : "");
      }));
      obs4.observe(list3, { "childList": true, "subtree": true, "characterData": true, "attributes": true, "attributeFilter": ["data-command-value", "data-command-search-only"] });
      return dialog["__cmdListObs"] = obs4;
    }
    ;
  };
  var viewport = function() {
    return window.visualViewport;
  };
  var clear_viewport_BANG_ = function(dialog) {
    const s1 = dialog.style;
    s1.removeProperty("--command-top");
    return s1.removeProperty("--command-max-h");
  };
  var sync_viewport_BANG_ = function(dialog) {
    const vv1 = viewport();
    if (truth_((() => {
      const and__23442__auto__2 = dialog;
      if (truth_(and__23442__auto__2)) {
        return vv1;
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      const h3 = vv1.height;
      const off4 = vv1.offsetTop;
      const kb5 = window.innerHeight - h3;
      if (truth_((() => {
        const or__23426__auto__6 = kb5 > 120;
        if (or__23426__auto__6) {
          return or__23426__auto__6;
        } else {
          return off4 > 1;
        }
        ;
      })())) {
        const top_gap7 = Math.max(12, Math.round(h3 * 0.08));
        const bot_gap8 = Math.max(12, Math.round(h3 * 0.06));
        const max_h9 = Math.max(160, h3 - top_gap7 - bot_gap8);
        const s10 = dialog.style;
        s10.setProperty("--command-top", `${off4 + top_gap7}px`);
        return s10.setProperty("--command-max-h", `${max_h9 ?? ""}px`);
      } else {
        return clear_viewport_BANG_(dialog);
      }
      ;
    }
    ;
  };
  var open = function(id) {
    const dialog1 = find_dialog(id);
    if (truth_((() => {
      const and__23442__auto__2 = dialog1;
      if (truth_(and__23442__auto__2)) {
        return not(dialog1.open);
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      dialog1.showModal();
      const input3 = dialog1.querySelector(".command-input");
      if (truth_(input3)) {
        input3.value = "";
        input3.focus();
      }
      ;
      filter_BANG_(dialog1, "");
      observe_list_BANG_(dialog1);
      return sync_viewport_BANG_(dialog1);
    }
    ;
  };
  var close = function(id) {
    const dialog1 = find_dialog(id);
    if (truth_((() => {
      const and__23442__auto__2 = dialog1;
      if (truth_(and__23442__auto__2)) {
        return dialog1.open;
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      return dialog1.close();
    }
    ;
  };
  var toggle = function(id) {
    const dialog1 = find_dialog(id);
    if (truth_(dialog1)) {
      if (truth_(dialog1.open)) {
        return dialog1.close();
      } else {
        return open(id);
      }
      ;
    }
    ;
  };
  var select_BANG_ = function(dialog, el2) {
    if (truth_((() => {
      const and__23442__auto__1 = el2;
      if (truth_(and__23442__auto__1)) {
        return not(el2.disabled);
      } else {
        return and__23442__auto__1;
      }
      ;
    })())) {
      dialog.close();
      if (not(el2.href)) {
        el2.click();
      }
      ;
      if (truth_(el2.href)) {
        return window.location = el2.href;
      }
      ;
    }
    ;
  };
  var hotkey_match_QMARK_ = function(spec, e) {
    const ekey1 = e.key;
    if (truth_(ekey1)) {
      const parts2 = spec.toLowerCase().split("+");
      const key3 = parts2[parts2.length - 1];
      const mod_QMARK_4 = spec.includes("mod");
      const shift_QMARK_5 = spec.includes("shift");
      const alt_QMARK_6 = spec.includes("alt");
      return _EQ_(ekey1.toLowerCase(), key3) && (_EQ_(mod_QMARK_4, (() => {
        const or__23426__auto__7 = e.metaKey;
        if (truth_(or__23426__auto__7)) {
          return or__23426__auto__7;
        } else {
          return e.ctrlKey;
        }
        ;
      })()) && (_EQ_(shift_QMARK_5, e.shiftKey) && _EQ_(alt_QMARK_6, e.altKey)));
    }
    ;
  };
  var on_global_key = function(e) {
    const dialogs1 = Array.from(document.querySelectorAll(".command-dialog[data-command-hotkey]"));
    return dialogs1.forEach((function(dialog) {
      const spec2 = dialog.dataset.commandHotkey;
      if (truth_((() => {
        const and__23442__auto__3 = spec2;
        if (truth_(and__23442__auto__3)) {
          return hotkey_match_QMARK_(spec2, e);
        } else {
          return and__23442__auto__3;
        }
        ;
      })())) {
        e.preventDefault();
        return toggle(dialog.id);
      }
      ;
    }));
  };
  var open_dialog = function() {
    return document.querySelector(".command-dialog[open]");
  };
  var on_input2 = function(e) {
    const t1 = e.target;
    if (truth_((() => {
      const and__23442__auto__2 = t1.classList;
      if (truth_(and__23442__auto__2)) {
        return t1.classList.contains("command-input");
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      const dialog3 = t1.closest(".command-dialog");
      if (truth_(dialog3)) {
        return filter_BANG_(dialog3, t1.value);
      }
      ;
    }
    ;
  };
  var on_keydown2 = function(e) {
    const dialog1 = open_dialog();
    if (truth_(dialog1)) {
      const key2 = e.key;
      if (truth_(key2 === "Alt" && quick_keys(dialog1))) {
        return show_hints_BANG_(dialog1);
      } else {
        if (truth_((() => {
          const and__23442__auto__3 = e.altKey;
          if (truth_(and__23442__auto__3)) {
            return not(e.ctrlKey) && (not(e.metaKey) && (not(e.shiftKey) && quick_item(dialog1, e)));
          } else {
            return and__23442__auto__3;
          }
          ;
        })())) {
          e.preventDefault();
          e.stopPropagation();
          return select_BANG_(dialog1, quick_item(dialog1, e));
        } else {
          if (key2 === "ArrowDown") {
            e.preventDefault();
            return move_active_BANG_(dialog1, "down");
          } else {
            if (key2 === "ArrowUp") {
              e.preventDefault();
              return move_active_BANG_(dialog1, "up");
            } else {
              if (truth_(key2 === "n" && e.ctrlKey)) {
                e.preventDefault();
                return move_active_BANG_(dialog1, "down");
              } else {
                if (truth_(key2 === "p" && e.ctrlKey)) {
                  e.preventDefault();
                  return move_active_BANG_(dialog1, "up");
                } else {
                  if (truth_(key2 === "j" && e.ctrlKey)) {
                    e.preventDefault();
                    return move_active_BANG_(dialog1, "down");
                  } else {
                    if (truth_(key2 === "k" && e.ctrlKey)) {
                      e.preventDefault();
                      return move_active_BANG_(dialog1, "up");
                    } else {
                      if (truth_((() => {
                        const and__23442__auto__5 = (() => {
                          const or__23426__auto__4 = key2 === "J";
                          if (or__23426__auto__4) {
                            return or__23426__auto__4;
                          } else {
                            return key2 === "j";
                          }
                          ;
                        })();
                        if (truth_(and__23442__auto__5)) {
                          const and__23442__auto__6 = e.altKey;
                          if (truth_(and__23442__auto__6)) {
                            return e.shiftKey;
                          } else {
                            return and__23442__auto__6;
                          }
                          ;
                        } else {
                          return and__23442__auto__5;
                        }
                        ;
                      })())) {
                        e.preventDefault();
                        return move_group_BANG_(dialog1, "down");
                      } else {
                        if (truth_((() => {
                          const and__23442__auto__8 = (() => {
                            const or__23426__auto__7 = key2 === "K";
                            if (or__23426__auto__7) {
                              return or__23426__auto__7;
                            } else {
                              return key2 === "k";
                            }
                            ;
                          })();
                          if (truth_(and__23442__auto__8)) {
                            const and__23442__auto__9 = e.altKey;
                            if (truth_(and__23442__auto__9)) {
                              return e.shiftKey;
                            } else {
                              return and__23442__auto__9;
                            }
                            ;
                          } else {
                            return and__23442__auto__8;
                          }
                          ;
                        })())) {
                          e.preventDefault();
                          return move_group_BANG_(dialog1, "up");
                        } else {
                          if (truth_(key2 === "j" && e.altKey)) {
                            e.preventDefault();
                            return move_active_BANG_(dialog1, "down");
                          } else {
                            if (truth_(key2 === "k" && e.altKey)) {
                              e.preventDefault();
                              return move_active_BANG_(dialog1, "up");
                            } else {
                              if (truth_(key2 === "n" && e.altKey)) {
                                e.preventDefault();
                                return move_active_BANG_(dialog1, "down");
                              } else {
                                if (truth_(key2 === "p" && e.altKey)) {
                                  e.preventDefault();
                                  return move_active_BANG_(dialog1, "up");
                                } else {
                                  if (truth_(key2 === "Home" && e.metaKey)) {
                                    e.preventDefault();
                                    return move_active_BANG_(dialog1, "home");
                                  } else {
                                    if (truth_(key2 === "End" && e.metaKey)) {
                                      e.preventDefault();
                                      return move_active_BANG_(dialog1, "end");
                                    } else {
                                      if (key2 === "Enter") {
                                        e.preventDefault();
                                        return select_BANG_(dialog1, active_item(dialog1));
                                      } else {
                                        return null;
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      ;
    }
    ;
  };
  var on_keyup = function(e) {
    if (e.key === "Alt") {
      const temp__23062__auto__1 = open_dialog();
      if (truth_(temp__23062__auto__1)) {
        const dialog2 = temp__23062__auto__1;
        return hide_hints_BANG_(dialog2);
      }
      ;
    }
    ;
  };
  var on_click2 = function(e) {
    const t1 = e.target;
    const dialog2 = truth_(t1.closest) ? t1.closest(".command-dialog") : null;
    if (truth_(dialog2)) {
      const item3 = t1.closest(".command-item");
      if (t1 === dialog2) {
        return dialog2.close();
      } else {
        if (truth_(item3)) {
          if (not(item3.disabled)) {
            return dialog2.close();
          }
        } else {
          return null;
        }
      }
      ;
    }
    ;
  };
  var on_pointermove2 = function(e) {
    if (!(e.pointerType === "touch")) {
      const t1 = e.target;
      if (truth_(t1.closest)) {
        const item2 = t1.closest(".command-item");
        if (truth_((() => {
          const and__23442__auto__3 = item2;
          if (truth_(and__23442__auto__3)) {
            return not(item2.hidden) && not(item2.disabled);
          } else {
            return and__23442__auto__3;
          }
          ;
        })())) {
          const dialog4 = item2.closest(".command-dialog");
          if (truth_(dialog4)) {
            return set_active_BANG_(dialog4, item2, false);
          }
          ;
        }
        ;
      }
      ;
    }
    ;
  };
  var on_viewport_change = function() {
    const temp__23062__auto__1 = open_dialog();
    if (truth_(temp__23062__auto__1)) {
      const dialog2 = temp__23062__auto__1;
      return sync_viewport_BANG_(dialog2);
    }
    ;
  };
  var on_dialog_close = function(e) {
    const t1 = e.target;
    if (truth_((() => {
      const and__23442__auto__2 = t1;
      if (truth_(and__23442__auto__2)) {
        const and__23442__auto__3 = t1.classList;
        if (truth_(and__23442__auto__3)) {
          return t1.classList.contains("command-dialog");
        } else {
          return and__23442__auto__3;
        }
        ;
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      hide_hints_BANG_(t1);
      return clear_viewport_BANG_(t1);
    }
    ;
  };
  var on_blur = function() {
    const temp__23062__auto__1 = open_dialog();
    if (truth_(temp__23062__auto__1)) {
      const dialog2 = temp__23062__auto__1;
      return hide_hints_BANG_(dialog2);
    }
    ;
  };
  var init_BANG_2 = function() {
    document.addEventListener("input", on_input2, true);
    document.addEventListener("keydown", on_keydown2, true);
    document.addEventListener("keyup", on_keyup, true);
    window.addEventListener("blur", on_blur);
    document.addEventListener("keydown", on_global_key);
    document.addEventListener("click", on_click2);
    document.addEventListener("pointermove", on_pointermove2, true);
    document.addEventListener("close", on_dialog_close, true);
    const temp__23062__auto__1 = viewport();
    if (truth_(temp__23062__auto__1)) {
      const vv2 = temp__23062__auto__1;
      vv2.addEventListener("resize", on_viewport_change);
      return vv2.addEventListener("scroll", on_viewport_change);
    }
    ;
  };
  init_BANG_2();
  window["__uiCommand"] = { "open": open, "close": close, "toggle": toggle };

  // .compiled/context_menu.mjs
  var get_state = function() {
    const or__23426__auto__1 = window["__uiCtxState"];
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const s2 = { "armed": null, "placedY": null, "placedX": null, "growthObserver": null, "back": null, "swap": null, "anchor": null, "menu": null, "cleanup": null };
      window["__uiCtxState"] = s2;
      return s2;
    }
    ;
  };
  var open_class = "clj-ui-menu-open";
  var create_icon = function(elements) {
    const ns_uri1 = "http://www.w3.org/2000/svg";
    const svg2 = document.createElementNS(ns_uri1, "svg");
    svg2.setAttribute("viewBox", "0 0 24 24");
    svg2.setAttribute("width", "18");
    svg2.setAttribute("height", "18");
    svg2.setAttribute("fill", "none");
    svg2.setAttribute("stroke", "currentColor");
    svg2.setAttribute("stroke-width", "1.6");
    svg2.setAttribute("stroke-linecap", "round");
    svg2.setAttribute("stroke-linejoin", "round");
    svg2.setAttribute("style", "flex-shrink:0");
    elements.forEach((function(el2) {
      const tag3 = el2[0];
      const attrs4 = el2[1];
      const node5 = document.createElementNS(ns_uri1, tag3);
      attrs4.forEach((function(pair) {
        return node5.setAttribute(pair[0], pair[1]);
      }));
      return svg2.appendChild(node5);
    }));
    return svg2;
  };
  var arm_ms = 3e3;
  var disarm_BANG_ = function() {
    const state1 = get_state();
    const armed2 = state1.armed;
    if (truth_(armed2)) {
      clearTimeout(armed2.timer);
      armed2.el.classList.remove("context-menu-item--armed");
      armed2.menu.classList.remove("context-menu--armed");
      armed2.labelEl.textContent = armed2.label;
      state1.armed = null;
      return true;
    }
    ;
  };
  var again_label = function(label) {
    const verb1 = truth_(document.documentElement.classList.contains("clj-ui-touch")) ? "Tap" : "Click";
    return `${verb1 ?? ""}${" again to "}${label.charAt(0).toLowerCase() ?? ""}${label.slice(1) ?? ""}`;
  };
  var arm_BANG_ = function(menu, el2, label_el, item) {
    disarm_BANG_();
    const label1 = item["label"];
    el2.style.setProperty("--context-menu-arm-duration", `${arm_ms ?? ""}ms`);
    el2.classList.add("context-menu-item--armed");
    menu.classList.add("context-menu--armed");
    label_el.textContent = again_label(label1);
    return get_state().armed = { "el": el2, "menu": menu, "labelEl": label_el, "label": label1, "timer": setTimeout(disarm_BANG_, arm_ms) };
  };
  var dismiss_BANG_ = function() {
    disarm_BANG_();
    const state1 = get_state();
    state1.back = null;
    state1.swap = null;
    if (truth_(state1.menu)) {
      state1.menu.remove();
      state1.menu = null;
      document.dispatchEvent(new CustomEvent("clj-ui-menu-dismiss"));
    }
    ;
    if (truth_(state1.growthObserver)) {
      state1.growthObserver.disconnect();
      state1.growthObserver = null;
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
  var make_el = function(tag, class_name) {
    const el1 = document.createElement(tag);
    el1.className = class_name;
    return el1;
  };
  var make_button = function(class_name, text, on_click7) {
    const btn1 = make_el("button", class_name);
    btn1.setAttribute("type", "button");
    btn1.setAttribute("tabindex", "-1");
    if (truth_(text)) {
      btn1.appendChild(document.createTextNode(text));
    }
    ;
    btn1.addEventListener("click", (function(e) {
      e.preventDefault();
      e.stopPropagation();
      return on_click7();
    }));
    return btn1;
  };
  var chevron_left = [["path", [["d", "m15 18-6-6 6-6"]]]];
  var chevron_right = [["path", [["d", "m9 18 6-6-6-6"]]]];
  var make_head = function(label, on_back) {
    const head1 = make_el("div", "context-menu-panel-head");
    const back2 = make_button("context-menu-panel-back context-menu-action", null, on_back);
    const span3 = make_el("span", "context-menu-panel-label");
    back2.setAttribute("aria-label", "Back");
    back2.appendChild(create_icon(chevron_left));
    head1.appendChild(back2);
    span3.textContent = label;
    head1.appendChild(span3);
    return head1;
  };
  var clamp_to_viewport_BANG_ = function(menu) {
    const x1 = parseFloat(menu.style.left);
    const y2 = parseFloat(menu.style.top);
    const w3 = menu.offsetWidth;
    const h4 = menu.offsetHeight;
    const vw5 = window.innerWidth;
    const vh6 = window.innerHeight;
    if (x1 + w3 > vw5 - 8) {
      menu.style.left = `${max(8, vw5 - w3 - 8) ?? ""}px`;
    }
    ;
    if (y2 + h4 > vh6 - 8) {
      return menu.style.top = `${max(8, vh6 - h4 - 8) ?? ""}px`;
    }
    ;
  };
  var place_BANG_ = function(menu) {
    const state1 = get_state();
    const style2 = menu.style;
    if (truth_((() => {
      const or__23426__auto__3 = !_EQ_(style2.left, state1.placedX);
      if (or__23426__auto__3) {
        return or__23426__auto__3;
      } else {
        return !_EQ_(style2.top, state1.placedY);
      }
      ;
    })())) {
      state1.anchor = { "x": style2.left, "y": style2.top };
    }
    ;
    style2.left = state1.anchor.x;
    style2.top = state1.anchor.y;
    clamp_to_viewport_BANG_(menu);
    state1.placedX = style2.left;
    return state1.placedY = style2.top;
  };
  var position_menu_BANG_ = function(menu, x, y) {
    const state1 = get_state();
    const style2 = menu.style;
    style2.left = `${x ?? ""}px`;
    style2.top = `${y ?? ""}px`;
    state1.anchor = { "x": style2.left, "y": style2.top };
    state1.placedX = style2.left;
    state1.placedY = style2.top;
    document.body.appendChild(menu);
    place_BANG_(menu);
    const mo3 = new MutationObserver((function(_) {
      if (not(get_state().swap)) {
        return clamp_to_viewport_BANG_(menu);
      }
      ;
    }));
    mo3.observe(menu, { "childList": true, "subtree": true });
    return state1.growthObserver = mo3;
  };
  var swap_ms = 240;
  var swap_easing = "cubic-bezier(0.2, 0, 0, 1)";
  var reduced_motion_QMARK_ = function() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  };
  var finish_swap_BANG_ = function() {
    const state1 = get_state();
    const swap2 = state1.swap;
    if (truth_(swap2)) {
      state1.swap = null;
      swap2.anims.forEach((function(a) {
        return a.cancel();
      }));
      swap2.ghost.remove();
      return swap2.menu.style.overflow = "";
    }
    ;
  };
  var px = function(n) {
    return `${n ?? ""}px`;
  };
  var slide_BANG_ = function(menu, page, old, forward_QMARK_, h0, w0, x0, y0) {
    const state1 = get_state();
    const style2 = menu.style;
    const opts3 = { "duration": swap_ms, "easing": swap_easing };
    const box4 = menu.animate({ "height": [px(h0), px(menu.offsetHeight)], "width": [px(w0), px(menu.offsetWidth)], "left": [x0, style2.left], "top": [y0, style2.top] }, opts3);
    const in$5 = page.animate(truth_(forward_QMARK_) ? { "transform": ["translateX(100%)", "translateX(0)"] } : { "transform": ["translateX(-30%)", "translateX(0)"], "opacity": [0, 1] }, opts3);
    const out6 = old.animate(truth_(forward_QMARK_) ? { "transform": ["translateX(0)", "translateX(-30%)"], "opacity": [1, 0] } : { "transform": ["translateX(0)", "translateX(100%)"] }, opts3);
    const swap7 = { "menu": menu, "ghost": old, "anims": [box4, in$5, out6] };
    style2.overflow = "hidden";
    state1.swap = swap7;
    return box4.onfinish = (function() {
      if (_EQ_(state1.swap, swap7)) {
        return finish_swap_BANG_();
      }
      ;
    });
  };
  var show_page_BANG_ = function(menu, page, direction) {
    finish_swap_BANG_();
    disarm_BANG_();
    const state1 = get_state();
    const style2 = menu.style;
    const old3 = menu.querySelector(":scope > .context-menu-page");
    const h04 = menu.offsetHeight;
    const w05 = menu.offsetWidth;
    const x06 = style2.left;
    const y07 = style2.top;
    const slide_QMARK_8 = (() => {
      const and__23442__auto__9 = direction;
      if (truth_(and__23442__auto__9)) {
        const and__23442__auto__10 = old3;
        if (truth_(and__23442__auto__10)) {
          const and__23442__auto__11 = menu.isConnected;
          if (truth_(and__23442__auto__11)) {
            const and__23442__auto__12 = menu.animate;
            if (truth_(and__23442__auto__12)) {
              return not(reduced_motion_QMARK_());
            } else {
              return and__23442__auto__12;
            }
            ;
          } else {
            return and__23442__auto__11;
          }
          ;
        } else {
          return and__23442__auto__10;
        }
        ;
      } else {
        return and__23442__auto__9;
      }
      ;
    })();
    const root_QMARK_13 = page.classList.contains("context-menu-page--root");
    if (truth_(slide_QMARK_8)) {
      const s14 = old3.style;
      old3.setAttribute("inert", "");
      s14.position = "absolute";
      s14.left = "0";
      s14.top = px(-menu.scrollTop);
      s14.width = px(menu.clientWidth);
      s14.zIndex = direction === "back" ? "1" : "0";
    } else {
      if (truth_(old3)) {
        old3.remove();
      }
    }
    ;
    menu.classList.toggle("context-menu--panel", page.classList.contains("context-menu-panel"));
    style2.minWidth = truth_(root_QMARK_13) ? "" : px(w05);
    menu.appendChild(page);
    menu.scrollTop = (() => {
      const or__23426__auto__15 = page.scrollTop0;
      if (truth_(or__23426__auto__15)) {
        return or__23426__auto__15;
      } else {
        return 0;
      }
      ;
    })();
    state1.back = page.onBack;
    if (truth_(menu.isConnected)) {
      place_BANG_(menu);
    }
    ;
    if (truth_(slide_QMARK_8)) {
      slide_BANG_(menu, page, old3, direction === "forward", h04, w05, x06, y07);
    }
    ;
    const f16 = page.focusEl;
    if (truth_(f16)) {
      return f16.focus({ "preventScroll": true });
    }
    ;
  };
  var confirm_page = function(item, on_back) {
    const label1 = item["label"];
    const confirm2 = item["confirm"];
    const title3 = item["confirm-title"];
    const message4 = truth_(string_QMARK_(confirm2)) ? confirm2 : "This can't be undone.";
    const danger5 = item["variant"] === "danger";
    const page6 = make_el("div", "context-menu-page context-menu-panel");
    const body7 = make_el("div", "context-menu-panel-body");
    const actions8 = make_el("div", "context-menu-panel-actions");
    const run9 = make_button(`${"btn btn-sm context-menu-action "}${(danger5 ? "btn-danger" : "btn-primary") ?? ""}`, null, (function() {
      dismiss_BANG_();
      return execute_item_BANG_(item);
    }));
    const cancel10 = make_button("btn btn-sm btn-ghost context-menu-action", "Cancel", on_back);
    if (truth_(title3)) {
      const t11 = make_el("p", "context-menu-panel-title");
      t11.textContent = title3;
      body7.appendChild(t11);
    }
    ;
    const m12 = make_el("p", "context-menu-panel-message");
    m12.textContent = message4;
    body7.appendChild(m12);
    const paths13 = item["icon-paths"];
    if (truth_((() => {
      const and__23442__auto__14 = paths13;
      if (truth_(and__23442__auto__14)) {
        return paths13.length > 0;
      } else {
        return and__23442__auto__14;
      }
      ;
    })())) {
      run9.appendChild(create_icon(paths13));
    }
    ;
    run9.appendChild(document.createTextNode(label1));
    actions8.appendChild(run9);
    actions8.appendChild(cancel10);
    page6.appendChild(make_head(label1, on_back));
    page6.appendChild(body7);
    page6.appendChild(actions8);
    page6.onBack = on_back;
    page6.focusEl = cancel10;
    return page6;
  };
  var first_item_idx = function(items2) {
    return items2.findIndex((function(item) {
      return !(item["type"] === "separator");
    }));
  };
  var items_page = function(menu, items2, focus_idx, parent) {
    const page1 = make_el("div", truth_(parent) ? "context-menu-page context-menu-page--sub" : "context-menu-page context-menu-page--root");
    if (truth_(parent)) {
      page1.appendChild(make_head(parent["label"], parent["back"]));
      page1.onBack = parent["back"];
    }
    ;
    items2.forEach((function(item, idx) {
      if (item["type"] === "separator") {
        const sep2 = make_el("div", "context-menu-separator");
        sep2.setAttribute("role", "separator");
        return page1.appendChild(sep2);
      } else {
        const url3 = item["url"];
        const sub4 = item["items"];
        const el5 = make_el(truth_(url3) ? "a" : "button", item["variant"] === "danger" ? "context-menu-item context-menu-item--danger" : "context-menu-item");
        const span6 = make_el("span", "context-menu-item-label");
        const back7 = (function(scroll) {
          return function() {
            const p8 = items_page(menu, items2, idx, parent);
            p8.scrollTop0 = scroll;
            return show_page_BANG_(menu, p8, "back");
          };
        });
        el5.setAttribute("role", "menuitem");
        el5.setAttribute("tabindex", "-1");
        if (truth_(url3)) {
          el5.setAttribute("href", url3);
        }
        ;
        const paths9 = item["icon-paths"];
        if (truth_((() => {
          const and__23442__auto__10 = paths9;
          if (truth_(and__23442__auto__10)) {
            return paths9.length > 0;
          } else {
            return and__23442__auto__10;
          }
          ;
        })())) {
          el5.appendChild(create_icon(paths9));
        }
        ;
        span6.textContent = item["label"];
        el5.appendChild(span6);
        if (truth_(sub4)) {
          el5.setAttribute("aria-haspopup", "menu");
          const chev11 = create_icon(chevron_right);
          chev11.setAttribute("class", "context-menu-item-chevron");
          el5.appendChild(chev11);
        }
        ;
        el5.addEventListener("click", (function(e) {
          e.preventDefault();
          e.stopPropagation();
          if (truth_(sub4)) {
            return show_page_BANG_(menu, items_page(menu, sub4, first_item_idx(sub4), { "label": item["label"], "back": back7(menu.scrollTop) }), "forward");
          } else {
            if (not(item["confirm"])) {
              dismiss_BANG_();
              return execute_item_BANG_(item);
            } else {
              if (item["confirm-style"] === "panel") {
                return show_page_BANG_(menu, confirm_page(item, back7(menu.scrollTop)), "forward");
              } else {
                if (truth_(el5.classList.contains("context-menu-item--armed"))) {
                  dismiss_BANG_();
                  return execute_item_BANG_(item);
                } else {
                  if ("else") {
                    return arm_BANG_(menu, el5, span6, item);
                  } else {
                    return null;
                  }
                }
              }
            }
          }
          ;
        }));
        page1.appendChild(el5);
        if (_EQ_(idx, focus_idx)) {
          return page1.focusEl = el5;
        }
        ;
      }
      ;
    }));
    return page1;
  };
  var create_menu = function(items2) {
    const menu1 = make_el("div", "context-menu");
    menu1.setAttribute("role", "menu");
    show_page_BANG_(menu1, items_page(menu1, items2, null, null), null);
    return menu1;
  };
  var moves_trigger_QMARK_ = function(trigger, e) {
    const t1 = e.target;
    const or__23426__auto__2 = not(trigger);
    if (or__23426__auto__2) {
      return or__23426__auto__2;
    } else {
      const or__23426__auto__3 = not(t1.contains);
      if (or__23426__auto__3) {
        return or__23426__auto__3;
      } else {
        return t1.contains(trigger);
      }
      ;
    }
    ;
  };
  var focusable = ".context-menu-page:not([inert]) :is(.context-menu-item, .context-menu-action)";
  var focus_first_item_BANG_ = function(menu) {
    const first_item1 = menu.querySelector(focusable);
    if (truth_(first_item1)) {
      return first_item1.focus();
    }
    ;
  };
  var focus_next_BANG_ = function(menu, direction) {
    const items1 = menu.querySelectorAll(focusable);
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
    const f515 = (function(var_args) {
      const args5161 = [];
      const len__23321__auto__2 = arguments.length;
      let i5173 = 0;
      while (true) {
        if (i5173 < len__23321__auto__2) {
          args5161.push(arguments[i5173]);
          let G__4 = i5173 + 1;
          i5173 = G__4;
          continue;
        }
        ;
        break;
      }
      ;
      const argseq__23513__auto__5 = 1 < args5161.length ? args5161.slice(1) : null;
      return f515.cljs$core$IFn$_invoke$arity$variadic(arguments[0], argseq__23513__auto__5);
    });
    f515.cljs$core$IFn$_invoke$arity$variadic = (function(event, args) {
      dismiss_BANG_();
      const trigger6 = event.currentTarget;
      const items7 = (() => {
        const passed8 = first(args);
        if (truth_(passed8)) {
          return passed8;
        } else {
          const json9 = event.currentTarget.dataset.contextMenu;
          if (truth_(json9)) {
            return JSON.parse(json9);
          }
          ;
        }
        ;
      })();
      const _10 = not(items7) ? console.warn("Context menu: no items provided") : null;
      const menu11 = create_menu(items7);
      const state12 = get_state();
      state12.menu = menu11;
      position_menu_BANG_(menu11, event.clientX, event.clientY);
      focus_first_item_BANG_(menu11);
      document.documentElement.classList.add(open_class);
      const on_click13 = (function(e) {
        if (not(menu11.contains(e.target))) {
          e.preventDefault();
          e.stopPropagation();
          return dismiss_BANG_();
        }
        ;
      });
      const on_ctx14 = (function(e) {
        e.preventDefault();
        e.stopImmediatePropagation();
        return dismiss_BANG_();
      });
      const on_key15 = (function(e) {
        const key16 = e.key;
        if (key16 === "Escape") {
          e.preventDefault();
          if (truth_(disarm_BANG_())) {
            return null;
          } else {
            if (truth_(state12.back)) {
              return state12.back();
            } else {
              if ("else") {
                return dismiss_BANG_();
              } else {
                return null;
              }
            }
          }
          ;
        } else {
          if (key16 === "ArrowDown") {
            e.preventDefault();
            return focus_next_BANG_(menu11, "down");
          } else {
            if (key16 === "ArrowUp") {
              e.preventDefault();
              return focus_next_BANG_(menu11, "up");
            } else {
              return null;
            }
          }
        }
        ;
      });
      const on_scroll17 = (function(e) {
        if (truth_(moves_trigger_QMARK_(trigger6, e))) {
          return dismiss_BANG_();
        }
        ;
      });
      const on_resize18 = (function(_) {
        return dismiss_BANG_();
      });
      const cleanup19 = (function() {
        document.documentElement.classList.remove(open_class);
        document.removeEventListener("click", on_click13, true);
        document.removeEventListener("contextmenu", on_ctx14, true);
        document.removeEventListener("keydown", on_key15, true);
        window.removeEventListener("scroll", on_scroll17, true);
        return window.removeEventListener("resize", on_resize18);
      });
      document.addEventListener("click", on_click13, true);
      document.addEventListener("contextmenu", on_ctx14, true);
      document.addEventListener("keydown", on_key15, true);
      window.addEventListener("scroll", on_scroll17, true);
      window.addEventListener("resize", on_resize18);
      return state12.cleanup = cleanup19;
    });
    f515.cljs$lang$maxFixedArity = 1;
    return f515;
  })();
  window["__uiContextMenu"] = open_context_menu;

  // ../../../dev/squint/node_modules/squint-cljs/src/squint/string.js
  function join(sep, coll) {
    if (coll === void 0) {
      coll = sep;
      sep = "";
    }
    if (coll instanceof Array) {
      return coll.join(sep);
    }
    let ret = "";
    let addSep = false;
    for (const o of iterable(coll)) {
      if (addSep) ret += sep;
      ret += o;
      addSep = true;
    }
    return ret;
  }
  function discardTrailingIfNeeded(limit, v) {
    if (limit == null && v.length > 1) {
      for (; ; )
        if (v[v.length - 1] === "") {
          v.pop();
        } else break;
    }
    return v;
  }
  function split(s, re, limit) {
    const split2 = s.split(re, limit);
    return discardTrailingIfNeeded(limit, split2);
  }
  function starts_with_QMARK_(s, substr) {
    return s.startsWith(substr);
  }
  function ends_with_QMARK_(s, substr) {
    return s.endsWith(substr);
  }

  // .compiled/dial.mjs
  var tof = function(v) {
    return typeof v;
  };
  var mk2 = function(tag, class$) {
    const e1 = document.createElement(tag);
    if (truth_(class$)) {
      e1.className = class$;
    }
    ;
    return e1;
  };
  var add_BANG_ = (() => {
    const f519 = (function(var_args) {
      const args5201 = [];
      const len__23321__auto__2 = arguments.length;
      let i5213 = 0;
      while (true) {
        if (i5213 < len__23321__auto__2) {
          args5201.push(arguments[i5213]);
          let G__4 = i5213 + 1;
          i5213 = G__4;
          continue;
        }
        ;
        break;
      }
      ;
      const argseq__23513__auto__5 = 1 < args5201.length ? args5201.slice(1) : null;
      return f519.cljs$core$IFn$_invoke$arity$variadic(arguments[0], argseq__23513__auto__5);
    });
    f519.cljs$core$IFn$_invoke$arity$variadic = (function(parent, children) {
      for (let G__6 of iterable(children)) {
        const c7 = G__6;
        if (truth_(c7)) {
          parent.appendChild(c7);
        }
      }
      ;
      return parent;
    });
    f519.cljs$lang$maxFixedArity = 1;
    return f519;
  })();
  var txt_BANG_ = function(e, s) {
    e.textContent = `${s ?? ""}`;
    return e;
  };
  var on_BANG_ = function(e, ev, f) {
    e.addEventListener(ev, f);
    return e;
  };
  var attr_BANG_ = function(e, k, v) {
    e.setAttribute(k, v);
    return e;
  };
  var clamp2 = function(v, lo, hi) {
    return Math.max(lo, Math.min(v, hi));
  };
  var path_str = function(path) {
    return path.join(".");
  };
  var fmt_num2 = function(n) {
    if (not(isFinite(n))) {
      return "0";
    } else {
      return `${Math.round(n * 1e3) / 1e3}`;
    }
    ;
  };
  var round_step = function(v, step) {
    if (truth_((() => {
      const and__23442__auto__1 = step;
      if (truth_(and__23442__auto__1)) {
        return step > 0;
      } else {
        return and__23442__auto__1;
      }
      ;
    })())) {
      return Math.round(v / step) * step;
    } else {
      return v;
    }
    ;
  };
  var infer_range = function(v) {
    if (v < 0) {
      return { "min": v * 3, "max": -v * 3, "step": 1 };
    } else {
      if (v <= 1) {
        return { "min": 0, "max": 1, "step": 0.01 };
      } else {
        if (v <= 10) {
          return { "min": 0, "max": v * 3, "step": 0.1 };
        } else {
          if (v <= 100) {
            return { "min": 0, "max": v * 3, "step": 1 };
          } else {
            if ("else") {
              return { "min": 0, "max": v * 3, "step": 10 };
            } else {
              return null;
            }
          }
        }
      }
    }
    ;
  };
  var infer_step = function(mx) {
    if (mx <= 1) {
      return 0.01;
    } else {
      if (mx <= 10) {
        return 0.1;
      } else {
        if (mx <= 100) {
          return 1;
        } else {
          if ("else") {
            return 10;
          } else {
            return null;
          }
        }
      }
    }
    ;
  };
  var color_re = new RegExp("^\\s*(#([0-9a-fA-F]{3,8})|(rgb|rgba|hsl|hsla|oklch|oklab|color)\\()");
  var color_str_QMARK_ = function(s) {
    return tof(s) === "string" && color_re.test(s);
  };
  var humanize = function(k) {
    const s1 = `${k ?? ""}`.replace(new RegExp("([a-z0-9])([A-Z])", "g"), "$1 $2").replace(new RegExp("[_\\-]", "g"), " ").trim();
    if (s1.length === 0) {
      return s1;
    } else {
      return `${s1.charAt(0).toUpperCase() ?? ""}${s1.slice(1) ?? ""}`;
    }
    ;
  };
  var obj_label = function(v, k) {
    const l1 = v["label"];
    if (truth_((() => {
      const or__23426__auto__2 = false_QMARK_(l1);
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return _EQ_(l1, "");
      }
      ;
    })())) {
      return null;
    } else {
      if (!(l1 == null)) {
        return l1;
      } else {
        if ("else") {
          return humanize(k);
        } else {
          return null;
        }
      }
    }
    ;
  };
  var normalize_options = function(opts) {
    return mapv((function(o) {
      if (tof(o) === "object") {
        return { "value": o["value"], "label": (() => {
          const or__23426__auto__1 = o["label"];
          if (truth_(or__23426__auto__1)) {
            return or__23426__auto__1;
          } else {
            return o["value"];
          }
          ;
        })() };
      } else {
        return { "value": o, "label": o };
      }
      ;
    }), opts);
  };
  var parse_node = function(k, v, path) {
    if (truth_(Array.isArray(v))) {
      const step1 = v.length > 3 ? v[3] : infer_step(v[2]);
      return { "ctype": "slider", "key": k, "path": path, "label": humanize(k), "default": v[0], "min": v[1], "max": v[2], "step": step1 };
    } else {
      if (tof(v) === "number") {
        const r2 = infer_range(v);
        return { "ctype": "slider", "key": k, "path": path, "label": humanize(k), "default": v, "min": r2["min"], "max": r2["max"], "step": r2["step"] };
      } else {
        if (tof(v) === "boolean") {
          return { "ctype": "toggle", "key": k, "path": path, "label": humanize(k), "default": v };
        } else {
          if (tof(v) === "string") {
            if (truth_(color_str_QMARK_(v))) {
              return { "ctype": "color", "key": k, "path": path, "label": humanize(k), "default": v };
            } else {
              return { "ctype": "text", "key": k, "path": path, "label": humanize(k), "default": v, "placeholder": "" };
            }
          } else {
            if (tof(v) === "object") {
              const t3 = v["type"];
              if (t3 === "slider") {
                const d4 = (() => {
                  const or__23426__auto__5 = v["default"];
                  if (truth_(or__23426__auto__5)) {
                    return or__23426__auto__5;
                  } else {
                    return 0;
                  }
                  ;
                })();
                const r6 = infer_range(d4);
                const mx7 = !(v["max"] == null) ? v["max"] : r6["max"];
                return { "ctype": "slider", "key": k, "path": path, "label": obj_label(v, k), "default": d4, "min": !(v["min"] == null) ? v["min"] : r6["min"], "max": mx7, "step": !(v["step"] == null) ? v["step"] : infer_step(mx7) };
              } else {
                if (t3 === "toggle") {
                  return { "ctype": "toggle", "key": k, "path": path, "label": obj_label(v, k), "default": boolean$(v["default"]) };
                } else {
                  if (t3 === "text") {
                    return { "ctype": "text", "key": k, "path": path, "label": obj_label(v, k), "default": (() => {
                      const or__23426__auto__8 = v["default"];
                      if (truth_(or__23426__auto__8)) {
                        return or__23426__auto__8;
                      } else {
                        return "";
                      }
                      ;
                    })(), "placeholder": (() => {
                      const or__23426__auto__9 = v["placeholder"];
                      if (truth_(or__23426__auto__9)) {
                        return or__23426__auto__9;
                      } else {
                        return "";
                      }
                      ;
                    })() };
                  } else {
                    if (t3 === "select") {
                      const os10 = normalize_options((() => {
                        const or__23426__auto__11 = v["options"];
                        if (truth_(or__23426__auto__11)) {
                          return or__23426__auto__11;
                        } else {
                          return [];
                        }
                        ;
                      })());
                      return { "ctype": "select", "key": k, "path": path, "label": obj_label(v, k), "options": os10, "default": (() => {
                        const or__23426__auto__12 = v["default"];
                        if (truth_(or__23426__auto__12)) {
                          return or__23426__auto__12;
                        } else {
                          const or__23426__auto__13 = truth_(seq(os10)) ? first(os10)["value"] : null;
                          if (truth_(or__23426__auto__13)) {
                            return or__23426__auto__13;
                          } else {
                            return "";
                          }
                          ;
                        }
                        ;
                      })() };
                    } else {
                      if (t3 === "color") {
                        return { "ctype": "color", "key": k, "path": path, "label": obj_label(v, k), "default": (() => {
                          const or__23426__auto__14 = v["default"];
                          if (truth_(or__23426__auto__14)) {
                            return or__23426__auto__14;
                          } else {
                            return "#000000";
                          }
                          ;
                        })() };
                      } else {
                        if (t3 === "image") {
                          const os15 = truth_(v["options"]) ? normalize_options(v["options"]) : null;
                          return { "ctype": "image", "key": k, "path": path, "label": obj_label(v, k), "options": os15, "default": (() => {
                            const or__23426__auto__16 = v["default"];
                            if (truth_(or__23426__auto__16)) {
                              return or__23426__auto__16;
                            } else {
                              const or__23426__auto__17 = truth_(seq(os15)) ? first(os15)["value"] : null;
                              if (truth_(or__23426__auto__17)) {
                                return or__23426__auto__17;
                              } else {
                                return "";
                              }
                              ;
                            }
                            ;
                          })() };
                        } else {
                          if (t3 === "pad") {
                            const ax18 = (function(a, def_) {
                              const tup19 = (() => {
                                const or__23426__auto__20 = v[a];
                                if (truth_(or__23426__auto__20)) {
                                  return or__23426__auto__20;
                                } else {
                                  return def_;
                                }
                                ;
                              })();
                              return { "default": tup19[0], "min": tup19[1], "max": tup19[2], "step": (() => {
                                const or__23426__auto__21 = tup19[3];
                                if (truth_(or__23426__auto__21)) {
                                  return or__23426__auto__21;
                                } else {
                                  return (tup19[2] - tup19[1]) / 200;
                                }
                                ;
                              })() };
                            });
                            const default_axis22 = [0, -1, 1, 0.01];
                            const xa23 = ax18("x", default_axis22);
                            const ya24 = ax18("y", default_axis22);
                            const labels25 = (() => {
                              const or__23426__auto__26 = v["labels"];
                              if (truth_(or__23426__auto__26)) {
                                return or__23426__auto__26;
                              } else {
                                return {};
                              }
                              ;
                            })();
                            return { "y": ya24, "path": path, "key": k, "default": { "x": xa23["default"], "y": ya24["default"] }, "y-label": (() => {
                              const or__23426__auto__27 = labels25["y"];
                              if (truth_(or__23426__auto__27)) {
                                return or__23426__auto__27;
                              } else {
                                return "Y";
                              }
                              ;
                            })(), "x-label": (() => {
                              const or__23426__auto__28 = labels25["x"];
                              if (truth_(or__23426__auto__28)) {
                                return or__23426__auto__28;
                              } else {
                                return "X";
                              }
                              ;
                            })(), "label": obj_label(v, k), "ctype": "pad", "x": xa23 };
                          } else {
                            if (truth_((() => {
                              const or__23426__auto__29 = t3 === "spring";
                              if (or__23426__auto__29) {
                                return or__23426__auto__29;
                              } else {
                                return t3 === "easing";
                              }
                              ;
                            })())) {
                              return { "ctype": "transition", "key": k, "path": path, "label": obj_label(v, k), "default": JSON.parse(JSON.stringify(v)) };
                            } else {
                              if (t3 === "action") {
                                return { "ctype": "action", "key": k, "path": path, "label": obj_label(v, k), "variant": (() => {
                                  const or__23426__auto__30 = v["variant"];
                                  if (truth_(or__23426__auto__30)) {
                                    return or__23426__auto__30;
                                  } else {
                                    return "secondary";
                                  }
                                  ;
                                })(), "loud": boolean$(v["loud"]) };
                              } else {
                                if ("else") {
                                  return { "ctype": "folder", "key": k, "path": path, "label": (() => {
                                    const or__23426__auto__31 = v["_label"];
                                    if (truth_(or__23426__auto__31)) {
                                      return or__23426__auto__31;
                                    } else {
                                      return humanize(k);
                                    }
                                    ;
                                  })(), "collapsed": boolean$(v["_collapsed"]), "children": parse_config(v, path) };
                                } else {
                                  return null;
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
              ;
            } else {
              if ("else") {
                return null;
              } else {
                return null;
              }
            }
          }
        }
      }
    }
    ;
  };
  var parse_config = function(config, path) {
    const out1 = [];
    for (let G__2 of iterable(Object.keys(config))) {
      const k3 = G__2;
      if (truth_((() => {
        const or__23426__auto__4 = k3 === "_collapsed";
        if (or__23426__auto__4) {
          return or__23426__auto__4;
        } else {
          return k3 === "_label";
        }
        ;
      })())) {
      } else {
        const node5 = parse_node(k3, config[k3], conj(path, k3));
        if (truth_(node5)) {
          out1.push(node5);
        }
      }
    }
    ;
    return out1;
  };
  var init_values_BANG_ = function(store, controls) {
    for (let G__1 of iterable(controls)) {
      const c2 = G__1;
      if (get(c2, "ctype") === "folder") {
        init_values_BANG_(store, get(c2, "children"));
      } else {
        if (get(c2, "ctype") === "action") {
        } else {
          if ("else") {
            swap_BANG_(store, assoc_in, get(c2, "path"), get(c2, "default"));
          } else {
          }
        }
      }
    }
    return null;
  };
  var notify_BANG_ = function(panel) {
    const vs1 = deref(get(panel, "store"));
    const temp__23062__auto__2 = get(panel, "onChange");
    if (truth_(temp__23062__auto__2)) {
      const cb3 = temp__23062__auto__2;
      cb3(clj__GT_js(vs1));
    }
    ;
    for (let G__4 of iterable(Array.from(get(panel, "subs")))) {
      const s5 = G__4;
      s5(clj__GT_js(vs1));
    }
    return null;
  };
  var commit_BANG_2 = function(panel, path, v) {
    swap_BANG_(get(panel, "store"), assoc_in, path, v);
    notify_BANG_(panel);
    return persist_save_BANG_(panel);
  };
  var refresh_updaters_BANG_ = function(panel) {
    const vs1 = deref(get(panel, "store"));
    const us2 = get(panel, "updaters");
    for (let G__3 of iterable(Object.keys(us2))) {
      const k4 = G__3;
      us2[k4](get_in(vs1, k4.split(".")));
    }
    return null;
  };
  var set_value_BANG_2 = function(panel, path, v) {
    swap_BANG_(get(panel, "store"), assoc_in, path, v);
    const temp__23062__auto__1 = get(panel, "updaters")[path_str(path)];
    if (truth_(temp__23062__auto__1)) {
      const u2 = temp__23062__auto__1;
      u2(v);
    }
    ;
    notify_BANG_(panel);
    return persist_save_BANG_(panel);
  };
  var reg_updater_BANG_ = function(panel, path, f) {
    return get(panel, "updaters")[path_str(path)] = f;
  };
  var row = function(label) {
    const r1 = mk2("div", "dial-row");
    if (truth_(label)) {
      add_BANG_(r1, txt_BANG_(mk2("label", "dial-label"), label));
    }
    ;
    return r1;
  };
  var reset_icon = [["path", [["d", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"]]], ["path", [["d", "M3 3v5h5"]]]];
  var snap_mult = function(e) {
    if (truth_(e.shiftKey)) {
      return 10;
    } else {
      if (truth_((() => {
        const or__23426__auto__1 = e.ctrlKey;
        if (truth_(or__23426__auto__1)) {
          return or__23426__auto__1;
        } else {
          return e.metaKey;
        }
        ;
      })())) {
        return 5;
      } else {
        if ("else") {
          return 1;
        } else {
          return null;
        }
      }
    }
    ;
  };
  var render_slider = function(panel, c) {
    const map__12 = c;
    const path3 = get(map__12, "path");
    const label4 = get(map__12, "label");
    const min5 = get(map__12, "min");
    const max6 = get(map__12, "max");
    const step7 = get(map__12, "step");
    const r8 = mk2("div", "dial-row dial-row--slider");
    const field9 = mk2("div", "dial-slider");
    const fill10 = mk2("div", "dial-slider-fill");
    const lab11 = mk2("span", "dial-slider-label");
    const num12 = mk2("input", "dial-num");
    const cur13 = (function() {
      return get_in(deref(get(panel, "store")), path3);
    });
    const paint14 = (function(v) {
      const pct15 = 100 * clamp2((v - min5) / (max6 - min5), 0, 1);
      fill10.style.width = `${pct15 ?? ""}${"%"}`;
      return num12.value = fmt_num2(v);
    });
    const put_BANG_16 = (function(v) {
      commit_BANG_2(panel, path3, v);
      return paint14(v);
    });
    const set_at17 = (function(clientx, mult) {
      const rect18 = field9.getBoundingClientRect();
      const t19 = clamp2((clientx - rect18.left) / rect18.width, 0, 1);
      const raw20 = min5 + t19 * (max6 - min5);
      return put_BANG_16(clamp2(round_step(raw20, step7 * mult), min5, max6));
    });
    const reset_value_BANG_21 = (function() {
      return set_value_BANG_2(panel, path3, get(c, "default"));
    });
    txt_BANG_(lab11, label4);
    attr_BANG_(lab11, "data-tooltip", label4);
    num12.type = "text";
    attr_BANG_(num12, "inputmode", "decimal");
    attr_BANG_(num12, "spellcheck", "false");
    add_BANG_(field9, fill10, lab11, num12);
    add_BANG_(r8, field9);
    attr_BANG_(field9, "tabindex", "0");
    attr_BANG_(field9, "data-long-press", "true");
    const drag22 = { "on": false, "x": 0 };
    on_BANG_(field9, "pointerdown", (function(e) {
      if (truth_(!_EQ_(e.target, num12) && e.button === 0)) {
        field9.setPointerCapture(e.pointerId);
        drag22["on"] = true;
        drag22["x"] = e.clientX;
        return set_at17(e.clientX, snap_mult(e));
      }
      ;
    }));
    on_BANG_(field9, "pointermove", (function(e) {
      if (truth_(drag22["on"])) {
        drag22["x"] = e.clientX;
        return set_at17(e.clientX, snap_mult(e));
      }
      ;
    }));
    on_BANG_(field9, "pointerup", (function(e) {
      if (truth_(drag22["on"])) {
        drag22["on"] = false;
        if (truth_(field9.hasPointerCapture(e.pointerId))) {
          return field9.releasePointerCapture(e.pointerId);
        }
        ;
      }
      ;
    }));
    on_BANG_(field9, "pointercancel", (function(_) {
      return drag22["on"] = false;
    }));
    for (let G__23 of iterable(["keydown", "keyup"])) {
      const ev24 = G__23;
      on_BANG_(field9, ev24, (function(e) {
        if (truth_((() => {
          const and__23442__auto__25 = drag22["on"];
          if (truth_(and__23442__auto__25)) {
            const k26 = e.key;
            const or__23426__auto__27 = k26 === "Shift";
            if (or__23426__auto__27) {
              return or__23426__auto__27;
            } else {
              const or__23426__auto__28 = k26 === "Control";
              if (or__23426__auto__28) {
                return or__23426__auto__28;
              } else {
                return k26 === "Meta";
              }
              ;
            }
            ;
          } else {
            return and__23442__auto__25;
          }
          ;
        })())) {
          return set_at17(drag22["x"], snap_mult(e));
        }
        ;
      }));
    }
    ;
    on_BANG_(field9, "contextmenu", (function(e) {
      const temp__23062__auto__29 = window.__uiContextMenu;
      if (truth_(temp__23062__auto__29)) {
        const open30 = temp__23062__auto__29;
        e.preventDefault();
        return open30(e, [{ "label": "Reset", "icon-paths": reset_icon, "on-click": reset_value_BANG_21 }]);
      }
      ;
    }));
    on_BANG_(num12, "focus", (function(_) {
      return num12.select();
    }));
    on_BANG_(num12, "change", (function(_) {
      const v31 = parseFloat(num12.value);
      if (truth_(isFinite(v31))) {
        return put_BANG_16(clamp2(round_step(v31, step7), min5, max6));
      } else {
        return paint14(cur13());
      }
      ;
    }));
    on_BANG_(field9, "keydown", (function(e) {
      if (!_EQ_(e.target, num12)) {
        const k32 = e.key;
        const d33 = step7 * (truth_((() => {
          const or__23426__auto__34 = k32 === "PageUp";
          if (or__23426__auto__34) {
            return or__23426__auto__34;
          } else {
            return k32 === "PageDown";
          }
          ;
        })()) ? 10 : snap_mult(e));
        if (truth_((() => {
          const or__23426__auto__35 = k32 === "Enter";
          if (or__23426__auto__35) {
            return or__23426__auto__35;
          } else {
            return k32 === " ";
          }
          ;
        })())) {
          e.preventDefault();
          num12.focus();
          return num12.select();
        } else {
          if (truth_((() => {
            const or__23426__auto__36 = k32 === "ArrowUp";
            if (or__23426__auto__36) {
              return or__23426__auto__36;
            } else {
              const or__23426__auto__37 = k32 === "ArrowRight";
              if (or__23426__auto__37) {
                return or__23426__auto__37;
              } else {
                return k32 === "PageUp";
              }
              ;
            }
            ;
          })())) {
            e.preventDefault();
            return put_BANG_16(clamp2(cur13() + d33, min5, max6));
          } else {
            if (truth_((() => {
              const or__23426__auto__38 = k32 === "ArrowDown";
              if (or__23426__auto__38) {
                return or__23426__auto__38;
              } else {
                const or__23426__auto__39 = k32 === "ArrowLeft";
                if (or__23426__auto__39) {
                  return or__23426__auto__39;
                } else {
                  return k32 === "PageDown";
                }
                ;
              }
              ;
            })())) {
              e.preventDefault();
              return put_BANG_16(clamp2(cur13() - d33, min5, max6));
            } else {
              if (k32 === "Home") {
                e.preventDefault();
                return put_BANG_16(min5);
              } else {
                if (k32 === "End") {
                  e.preventDefault();
                  return put_BANG_16(max6);
                } else {
                  return null;
                }
              }
            }
          }
        }
        ;
      }
      ;
    }));
    paint14(cur13());
    reg_updater_BANG_(panel, path3, paint14);
    return r8;
  };
  var render_toggle = function(panel, c) {
    const map__12 = c;
    const path3 = get(map__12, "path");
    const label4 = get(map__12, "label");
    const r5 = row(label4);
    const wrap6 = mk2("label", "switch dial-switch");
    const input7 = mk2("input", "switch-input");
    const track8 = mk2("span", "switch-track");
    const thumb9 = mk2("span", "switch-thumb");
    const paint10 = (function(v) {
      input7.checked = boolean$(v);
      if (truth_(v)) {
        return track8.classList.add("switch-track--checked");
      } else {
        return track8.classList.remove("switch-track--checked");
      }
      ;
    });
    r5.classList.add("dial-row--toggle");
    attr_BANG_(input7, "type", "checkbox");
    add_BANG_(track8, thumb9);
    add_BANG_(wrap6, input7, track8);
    on_BANG_(input7, "change", (function(_) {
      const v11 = input7.checked;
      commit_BANG_2(panel, path3, v11);
      return paint10(v11);
    }));
    add_BANG_(r5, wrap6);
    paint10(get_in(deref(get(panel, "store")), path3));
    reg_updater_BANG_(panel, path3, paint10);
    return r5;
  };
  var render_text = function(panel, c) {
    const map__12 = c;
    const path3 = get(map__12, "path");
    const label4 = get(map__12, "label");
    const placeholder5 = get(map__12, "placeholder");
    const r6 = row(label4);
    const ta7 = mk2("textarea", "dial-text");
    const grow8 = (function() {
      ta7.style.height = "auto";
      const h9 = ta7.scrollHeight;
      if (h9 > 0) {
        return ta7.style.height = `${Math.min(200, h9) ?? ""}px`;
      }
      ;
    });
    r6.classList.add("dial-row--text");
    attr_BANG_(ta7, "rows", "3");
    if (truth_(seq(placeholder5))) {
      attr_BANG_(ta7, "placeholder", placeholder5);
    }
    ;
    on_BANG_(ta7, "input", (function(_) {
      grow8();
      return commit_BANG_2(panel, path3, ta7.value);
    }));
    add_BANG_(r6, ta7);
    ta7.value = (() => {
      const or__23426__auto__10 = get_in(deref(get(panel, "store")), path3);
      if (truth_(or__23426__auto__10)) {
        return or__23426__auto__10;
      } else {
        return "";
      }
      ;
    })();
    reg_updater_BANG_(panel, path3, (function(v) {
      ta7.value = (() => {
        const or__23426__auto__11 = v;
        if (truth_(or__23426__auto__11)) {
          return or__23426__auto__11;
        } else {
          return "";
        }
        ;
      })();
      return grow8();
    }));
    return r6;
  };
  var render_select = function(panel, c) {
    const map__12 = c;
    const path3 = get(map__12, "path");
    const label4 = get(map__12, "label");
    const options5 = get(map__12, "options");
    const r6 = row(label4);
    const wrap7 = mk2("div", "select dial-select");
    const trigger8 = mk2("button", "select-trigger");
    const valspan9 = mk2("span", "select-value");
    const opt_for10 = (function(v) {
      return options5.find((function(o) {
        return _EQ_(o["value"], v);
      }));
    });
    const set_lbl11 = (function(v) {
      const o12 = opt_for10(v);
      return txt_BANG_(valspan9, truth_(o12) ? o12["label"] : (() => {
        const or__23426__auto__13 = v;
        if (truth_(or__23426__auto__13)) {
          return or__23426__auto__13;
        } else {
          return "";
        }
        ;
      })());
    });
    attr_BANG_(trigger8, "type", "button");
    attr_BANG_(trigger8, "role", "combobox");
    attr_BANG_(trigger8, "aria-haspopup", "listbox");
    attr_BANG_(trigger8, "aria-expanded", "false");
    if (truth_(empty_QMARK_(options5))) {
      trigger8.disabled = true;
    }
    ;
    add_BANG_(trigger8, valspan9);
    add_BANG_(wrap7, trigger8);
    add_BANG_(r6, wrap7);
    const cur14 = get_in(deref(get(panel, "store")), path3);
    attr_BANG_(trigger8, "data-select-value", (() => {
      const or__23426__auto__15 = cur14;
      if (truth_(or__23426__auto__15)) {
        return or__23426__auto__15;
      } else {
        return "";
      }
      ;
    })());
    set_lbl11(cur14);
    on_BANG_(trigger8, "click", (function(_) {
      const temp__23062__auto__16 = window.__uiSelect;
      if (truth_(temp__23062__auto__16)) {
        const f17 = temp__23062__auto__16;
        return f17(trigger8, options5, (function(v) {
          attr_BANG_(trigger8, "data-select-value", v);
          set_lbl11(v);
          return commit_BANG_2(panel, path3, v);
        }));
      }
      ;
    }));
    reg_updater_BANG_(panel, path3, (function(v) {
      attr_BANG_(trigger8, "data-select-value", (() => {
        const or__23426__auto__18 = v;
        if (truth_(or__23426__auto__18)) {
          return or__23426__auto__18;
        } else {
          return "";
        }
        ;
      })());
      return set_lbl11(v);
    }));
    return r6;
  };
  var render_color = function(panel, c) {
    const map__12 = c;
    const path3 = get(map__12, "path");
    const label4 = get(map__12, "label");
    const r5 = mk2("div", "dial-row dial-row--color");
    const picker6 = create({ "value": get_in(deref(get(panel, "store")), path3), "alpha": true });
    const value7 = picker6.querySelector("[data-ui-color-value]");
    on_BANG_(value7, "input", (function(_) {
      return commit_BANG_2(panel, path3, value7.value);
    }));
    if (truth_(label4)) {
      add_BANG_(r5, txt_BANG_(mk2("label", "dial-label"), label4));
    }
    ;
    add_BANG_(r5, picker6);
    reg_updater_BANG_(panel, path3, (function(v) {
      return set_value_BANG_(picker6, v);
    }));
    return r5;
  };
  var render_image = function(panel, c) {
    const map__12 = c;
    const path3 = get(map__12, "path");
    const label4 = get(map__12, "label");
    const options5 = get(map__12, "options");
    const r6 = row(label4);
    const wrap7 = mk2("div", "dial-image");
    const grid8 = mk2("div", "dial-image-grid");
    const drop9 = mk2("label", "dial-image-drop");
    const file10 = mk2("input", null);
    const cur11 = (function() {
      return get_in(deref(get(panel, "store")), path3);
    });
    const mark12 = (function(v) {
      for (let G__13 of iterable(Array.from(grid8.children))) {
        const ch14 = G__13;
        if (_EQ_(ch14.getAttribute("data-value"), v)) {
          ch14.classList.add("is-active");
        } else {
          ch14.classList.remove("is-active");
        }
      }
      return null;
    });
    const read_BANG_15 = (function(f) {
      if (truth_(f)) {
        const rd16 = new FileReader();
        rd16.onload = (function(_) {
          const v17 = rd16.result;
          commit_BANG_2(panel, path3, v17);
          return mark12(v17);
        });
        return rd16.readAsDataURL(f);
      }
      ;
    });
    file10.type = "file";
    attr_BANG_(file10, "accept", "image/*");
    file10.style.display = "none";
    if (truth_(seq(options5))) {
      for (let G__18 of iterable(options5)) {
        const o19 = G__18;
        const b20 = mk2("button", "dial-image-opt");
        attr_BANG_(b20, "type", "button");
        attr_BANG_(b20, "data-value", o19["value"]);
        attr_BANG_(b20, "data-tooltip", o19["label"]);
        attr_BANG_(b20, "aria-label", o19["label"]);
        b20.style.backgroundImage = `${"url("}${JSON.stringify(o19["value"]) ?? ""}${")"}`;
        on_BANG_(b20, "click", (function(_) {
          const v21 = o19["value"];
          commit_BANG_2(panel, path3, v21);
          return mark12(v21);
        }));
        add_BANG_(grid8, b20);
      }
    }
    ;
    txt_BANG_(drop9, "Drop / upload");
    add_BANG_(drop9, file10);
    on_BANG_(file10, "change", (function(_) {
      return read_BANG_15(file10.files[0]);
    }));
    on_BANG_(drop9, "dragover", (function(e) {
      e.preventDefault();
      return drop9.classList.add("is-over");
    }));
    on_BANG_(drop9, "dragleave", (function(_) {
      return drop9.classList.remove("is-over");
    }));
    on_BANG_(drop9, "drop", (function(e) {
      e.preventDefault();
      drop9.classList.remove("is-over");
      return read_BANG_15(e.dataTransfer.files[0]);
    }));
    add_BANG_(wrap7, grid8, drop9);
    add_BANG_(r6, wrap7);
    mark12(cur11());
    reg_updater_BANG_(panel, path3, (function(v) {
      return mark12(v);
    }));
    return r6;
  };
  var render_pad = function(panel, c) {
    const map__12 = c;
    const path3 = get(map__12, "path");
    const x4 = get(map__12, "x");
    const y5 = get(map__12, "y");
    const x_label6 = get(map__12, "x-label");
    const y_label7 = get(map__12, "y-label");
    const r8 = row(get(c, "label"));
    const area9 = mk2("div", "dial-pad");
    const dot10 = mk2("div", "dial-pad-dot");
    const meta11 = mk2("div", "dial-pad-meta");
    const cur12 = (function() {
      return get_in(deref(get(panel, "store")), path3);
    });
    const to_pct13 = (function(v, ax) {
      return clamp2((v - ax["min"]) / (ax["max"] - ax["min"]), 0, 1);
    });
    const paint14 = (function(val) {
      const px15 = 100 * to_pct13(val["x"], x4);
      const py16 = 100 * (1 - to_pct13(val["y"], y5));
      dot10.style.left = `${px15 ?? ""}${"%"}`;
      dot10.style.top = `${py16 ?? ""}${"%"}`;
      return txt_BANG_(meta11, `${x_label6 ?? ""}${" "}${fmt_num2(val["x"]) ?? ""}${"   "}${y_label7 ?? ""}${" "}${fmt_num2(val["y"]) ?? ""}`);
    });
    const set_at17 = (function(cx, cy) {
      const rect18 = area9.getBoundingClientRect();
      const tx19 = clamp2((cx - rect18.left) / rect18.width, 0, 1);
      const ty20 = clamp2((cy - rect18.top) / rect18.height, 0, 1);
      const vx21 = clamp2(round_step(x4["min"] + tx19 * (x4["max"] - x4["min"]), x4["step"]), x4["min"], x4["max"]);
      const vy22 = clamp2(round_step(y5["min"] + (1 - ty20) * (y5["max"] - y5["min"]), y5["step"]), y5["min"], y5["max"]);
      const v23 = { "x": vx21, "y": vy22 };
      commit_BANG_2(panel, path3, v23);
      return paint14(v23);
    });
    add_BANG_(area9, dot10);
    add_BANG_(r8, area9, meta11);
    attr_BANG_(area9, "tabindex", "0");
    const dragging24 = { "on": false };
    on_BANG_(area9, "pointerdown", (function(e) {
      area9.setPointerCapture(e.pointerId);
      dragging24["on"] = true;
      return set_at17(e.clientX, e.clientY);
    }));
    on_BANG_(area9, "pointermove", (function(e) {
      if (truth_(dragging24["on"])) {
        return set_at17(e.clientX, e.clientY);
      }
      ;
    }));
    on_BANG_(area9, "pointerup", (function(_) {
      return dragging24["on"] = false;
    }));
    on_BANG_(area9, "dblclick", (function(_) {
      commit_BANG_2(panel, path3, get(c, "default"));
      return paint14(get(c, "default"));
    }));
    paint14(cur12());
    reg_updater_BANG_(panel, path3, paint14);
    return r8;
  };
  var cubic_bezier = function(p1x, p1y, p2x, p2y, t) {
    const mt1 = 1 - t;
    const y2 = 3 * mt1 * mt1 * t * p1y + 3 * mt1 * t * t * p2y + t * t * t;
    return { "x": t, "y": y2 };
  };
  var sample_spring = function(visual_dur, bounce, t) {
    const zeta1 = clamp2(1 - bounce, 0.05, 1);
    const omega2 = 2 * Math.PI / Math.max(0.05, visual_dur);
    if (zeta1 < 1) {
      const wd3 = omega2 * Math.sqrt(1 - zeta1 * zeta1);
      return 1 - Math.exp(-zeta1 * omega2 * t) * (Math.cos(wd3 * t) + zeta1 * omega2 / wd3 * Math.sin(wd3 * t));
    } else {
      return 1 - Math.exp(-omega2 * t) * (1 + omega2 * t);
    }
    ;
  };
  var render_transition = function(panel, c) {
    const map__12 = c;
    const path3 = get(map__12, "path");
    const r4 = row(get(c, "label"));
    const wrap5 = mk2("div", "dial-transition");
    const modes6 = mk2("div", "dial-seg");
    const canvas7 = mk2("canvas", "dial-curve");
    const fields8 = mk2("div", "dial-fields");
    const cur9 = (function() {
      return get_in(deref(get(panel, "store")), path3);
    });
    const get_mode10 = (function() {
      const v11 = cur9();
      if (v11["type"] === "easing") {
        return "easing";
      } else {
        if (truth_(v11["stiffness"])) {
          return "physics";
        } else {
          if ("else") {
            return "time";
          } else {
            return null;
          }
        }
      }
      ;
    });
    const draw12 = (function() {
      const ctx13 = canvas7.getContext("2d");
      const w14 = 220;
      const h15 = 90;
      const accent16 = getComputedStyle(document.documentElement).getPropertyValue("--accent").trim();
      canvas7.width = w14;
      canvas7.height = h15;
      ctx13.clearRect(0, 0, w14, h15);
      ctx13.lineWidth = 2;
      ctx13.strokeStyle = truth_(seq(accent16)) ? accent16 : "#7c5cfc";
      ctx13.beginPath();
      const v17 = cur9();
      const mode18 = get_mode10();
      const n52319 = 61;
      let i20 = 0;
      for (; i20 < n52319; i20++) {
        (() => {
          const t21 = i20 / 60;
          const y22 = mode18 === "easing" ? (() => {
            const e23 = (() => {
              const or__23426__auto__24 = v17["ease"];
              if (truth_(or__23426__auto__24)) {
                return or__23426__auto__24;
              } else {
                return [0.25, 0.1, 0.25, 1];
              }
              ;
            })();
            return cubic_bezier(e23[0], e23[1], e23[2], e23[3], t21)["y"];
          })() : mode18 === "physics" ? sample_spring(0.5, 0.2, t21 * 1) : "else" ? (() => {
            const vd25 = (() => {
              const or__23426__auto__26 = v17["visualDuration"];
              if (truth_(or__23426__auto__26)) {
                return or__23426__auto__26;
              } else {
                return 0.4;
              }
              ;
            })();
            return sample_spring(vd25, (() => {
              const or__23426__auto__27 = v17["bounce"];
              if (truth_(or__23426__auto__27)) {
                return or__23426__auto__27;
              } else {
                return 0.2;
              }
              ;
            })(), t21 * (vd25 * 2));
          })() : null;
          const px28 = t21 * w14;
          const py29 = h15 - clamp2(y22, -0.2, 1.4) * (h15 * 0.7) - h15 * 0.1;
          if (i20 === 0) {
            return ctx13.moveTo(px28, py29);
          } else {
            return ctx13.lineTo(px28, py29);
          }
          ;
        })();
      }
      ;
      return ctx13.stroke();
    });
    const num_field30 = (function(key_, lbl, mn, mx, st) {
      const fw31 = mk2("label", "dial-field");
      const inp32 = mk2("input", null);
      inp32.type = "number";
      inp32.min = `${mn ?? ""}`;
      inp32.max = `${mx ?? ""}`;
      inp32.step = `${st ?? ""}`;
      inp32.value = `${(() => {
        const or__23426__auto__33 = cur9()[key_];
        if (truth_(or__23426__auto__33)) {
          return or__23426__auto__33;
        } else {
          return "";
        }
        ;
      })() ?? ""}`;
      add_BANG_(fw31, txt_BANG_(mk2("span", null), lbl), inp32);
      on_BANG_(inp32, "input", (function(_) {
        const v34 = JSON.parse(JSON.stringify(cur9()));
        v34[key_] = parseFloat(inp32.value);
        commit_BANG_2(panel, path3, v34);
        return draw12();
      }));
      return { "el": fw31, "inp": inp32 };
    });
    const rebuild35 = (function() {
      fields8.innerHTML = "";
      const mode36 = get_mode10();
      const v37 = cur9();
      if (mode36 === "easing") {
        add_BANG_(fields8, num_field30("duration", "Duration", 0, 5, 0.05)["el"]);
      } else {
        if (mode36 === "physics") {
          for (let G__38 of iterable([num_field30("stiffness", "Stiffness", 1, 500, 1), num_field30("damping", "Damping", 1, 60, 1), num_field30("mass", "Mass", 0.1, 5, 0.1)])) {
            const f39 = G__38;
            add_BANG_(fields8, f39["el"]);
          }
        } else {
          if ("else") {
            for (let G__40 of iterable([num_field30("visualDuration", "Duration", 0.05, 2, 0.01), num_field30("bounce", "Bounce", 0, 1, 0.01)])) {
              const f41 = G__40;
              add_BANG_(fields8, f41["el"]);
            }
          } else {
          }
        }
      }
      ;
      return draw12();
    });
    const set_mode42 = (function(m) {
      const v43 = JSON.parse(JSON.stringify(cur9()));
      if (m === "easing") {
        v43["type"] = "easing";
        if (truth_(v43["ease"])) {
        } else {
          v43["ease"] = [0.25, 0.1, 0.25, 1];
        }
        ;
        if (truth_(v43["duration"])) {
        } else {
          v43["duration"] = 0.3;
        }
      } else {
        if (m === "physics") {
          v43["type"] = "spring";
          v43["stiffness"] = (() => {
            const or__23426__auto__44 = v43["stiffness"];
            if (truth_(or__23426__auto__44)) {
              return or__23426__auto__44;
            } else {
              return 200;
            }
            ;
          })();
          v43["damping"] = (() => {
            const or__23426__auto__45 = v43["damping"];
            if (truth_(or__23426__auto__45)) {
              return or__23426__auto__45;
            } else {
              return 25;
            }
            ;
          })();
          v43["mass"] = (() => {
            const or__23426__auto__46 = v43["mass"];
            if (truth_(or__23426__auto__46)) {
              return or__23426__auto__46;
            } else {
              return 1;
            }
            ;
          })();
        } else {
          if ("else") {
            v43["type"] = "spring";
            delete v43["stiffness"];
            delete v43["damping"];
            delete v43["mass"];
            v43["visualDuration"] = (() => {
              const or__23426__auto__47 = v43["visualDuration"];
              if (truth_(or__23426__auto__47)) {
                return or__23426__auto__47;
              } else {
                return 0.4;
              }
              ;
            })();
            v43["bounce"] = (() => {
              const or__23426__auto__48 = v43["bounce"];
              if (truth_(or__23426__auto__48)) {
                return or__23426__auto__48;
              } else {
                return 0.2;
              }
              ;
            })();
          } else {
          }
        }
      }
      ;
      commit_BANG_2(panel, path3, v43);
      return rebuild35();
    });
    for (let G__49 of iterable([["easing", "Easing"], ["time", "Time"], ["physics", "Physics"]])) {
      const pair50 = G__49;
      const b51 = mk2("button", "dial-seg-btn");
      attr_BANG_(b51, "type", "button");
      txt_BANG_(b51, pair50[1]);
      on_BANG_(b51, "click", (function(_) {
        return set_mode42(pair50[0]);
      }));
      add_BANG_(modes6, b51);
    }
    ;
    add_BANG_(wrap5, modes6, canvas7, fields8);
    add_BANG_(r4, wrap5);
    rebuild35();
    reg_updater_BANG_(panel, path3, (function(_) {
      return rebuild35();
    }));
    return r4;
  };
  var render_action = function(panel, c) {
    const map__12 = c;
    const path3 = get(map__12, "path");
    const label4 = get(map__12, "label");
    const variant5 = get(map__12, "variant");
    const loud6 = get(map__12, "loud");
    const btn7 = mk2("button", `${"btn btn-sm btn-"}${variant5 ?? ""}${(truth_(loud6) ? " btn-loud" : null) ?? ""}${" dial-action"}`);
    attr_BANG_(btn7, "type", "button");
    if (variant5 === "primary") {
      attr_BANG_(btn7, "data-dial-submit", "true");
    }
    ;
    txt_BANG_(btn7, label4);
    on_BANG_(btn7, "click", (function(_) {
      const temp__23062__auto__8 = get(panel, "onAction");
      if (truth_(temp__23062__auto__8)) {
        const f9 = temp__23062__auto__8;
        return f9(path_str(path3));
      }
      ;
    }));
    return btn7;
  };
  var render_folder = function(panel, c) {
    const wrap1 = mk2("div", "dial-folder");
    const head2 = mk2("button", "dial-folder-head");
    const body3 = mk2("div", "dial-folder-body");
    const chev4 = mk2("span", "dial-chevron");
    const open5 = atom(not(get(c, "collapsed")));
    const sync6 = (function() {
      if (truth_(deref(open5))) {
        return wrap1.classList.remove("is-collapsed");
      } else {
        return wrap1.classList.add("is-collapsed");
      }
      ;
    });
    attr_BANG_(head2, "type", "button");
    add_BANG_(head2, chev4, txt_BANG_(mk2("span", null), get(c, "label")));
    on_BANG_(head2, "click", (function(_) {
      swap_BANG_(open5, not);
      return sync6();
    }));
    render_controls_BANG_(panel, body3, get(c, "children"));
    add_BANG_(wrap1, head2, body3);
    sync6();
    return wrap1;
  };
  var render_control = function(panel, c) {
    const t1 = get(c, "ctype");
    if (t1 === "slider") {
      return render_slider(panel, c);
    } else {
      if (t1 === "toggle") {
        return render_toggle(panel, c);
      } else {
        if (t1 === "text") {
          return render_text(panel, c);
        } else {
          if (t1 === "select") {
            return render_select(panel, c);
          } else {
            if (t1 === "color") {
              return render_color(panel, c);
            } else {
              if (t1 === "image") {
                return render_image(panel, c);
              } else {
                if (t1 === "pad") {
                  return render_pad(panel, c);
                } else {
                  if (t1 === "transition") {
                    return render_transition(panel, c);
                  } else {
                    if (t1 === "action") {
                      return render_action(panel, c);
                    } else {
                      if (t1 === "folder") {
                        return render_folder(panel, c);
                      } else {
                        if ("else") {
                          return mk2("div", null);
                        } else {
                          return null;
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    ;
  };
  var render_controls_BANG_ = function(panel, parent, controls) {
    const actions1 = atom(null);
    for (let G__2 of iterable(controls)) {
      const c3 = G__2;
      if (get(c3, "ctype") === "action") {
        if (truth_(deref(actions1))) {
        } else {
          reset_BANG_(actions1, mk2("div", "dial-row dial-actions"));
          add_BANG_(parent, deref(actions1));
        }
        ;
        add_BANG_(deref(actions1), render_control(panel, c3));
      } else {
        reset_BANG_(actions1, null);
        add_BANG_(parent, render_control(panel, c3));
      }
    }
    return null;
  };
  var storage_for = function(panel) {
    const p1 = get(panel, "persist");
    if (truth_((() => {
      const and__23442__auto__2 = p1;
      if (truth_(and__23442__auto__2)) {
        return p1["storage"] === "sessionStorage";
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      return sessionStorage;
    } else {
      return localStorage;
    }
    ;
  };
  var storage_key = function(panel) {
    const p1 = get(panel, "persist");
    const or__23426__auto__2 = (() => {
      const and__23442__auto__3 = p1;
      if (truth_(and__23442__auto__3)) {
        return p1["key"];
      } else {
        return and__23442__auto__3;
      }
      ;
    })();
    if (truth_(or__23426__auto__2)) {
      return or__23426__auto__2;
    } else {
      return `${"dialkit:"}${get(panel, "id") ?? ""}`;
    }
    ;
  };
  var persist_save_BANG_ = function(panel) {
    if (truth_(get(panel, "persist"))) {
      return (() => {
        try {
          const payload1 = { "values": clj__GT_js(deref(get(panel, "store"))), "presets": get(panel, "presets"), "active": get(panel, "activePreset") };
          return storage_for(panel).setItem(storage_key(panel), JSON.stringify(payload1));
        } catch (_2) {
          return null;
        }
      })();
    }
    ;
  };
  var persist_load_BANG_ = function(panel) {
    if (truth_(get(panel, "persist"))) {
      return (() => {
        try {
          const raw1 = storage_for(panel).getItem(storage_key(panel));
          if (truth_(raw1)) {
            const data2 = JSON.parse(raw1);
            if (truth_(data2["values"])) {
              reset_BANG_(get(panel, "store"), data2["values"]);
            }
            ;
            if (truth_(data2["presets"])) {
              panel["presets"] = data2["presets"];
            }
            ;
            if (truth_(data2["active"])) {
              return panel["activePreset"] = data2["active"];
            }
            ;
          }
          ;
        } catch (_3) {
          return null;
        }
      })();
    }
    ;
  };
  var root_el = atom(null);
  var panels = atom({});
  var drag_state = { "on": false, "x": 0, "y": 0, "sx": 0, "sy": 0 };
  var apply_root_position_BANG_ = function(el2, pos) {
    const s1 = el2.style;
    const bottom_QMARK_2 = (() => {
      const or__23426__auto__3 = pos === "bottom-right";
      if (or__23426__auto__3) {
        return or__23426__auto__3;
      } else {
        return pos === "bottom-left";
      }
      ;
    })();
    const left_QMARK_4 = (() => {
      const or__23426__auto__5 = pos === "top-left";
      if (or__23426__auto__5) {
        return or__23426__auto__5;
      } else {
        return pos === "bottom-left";
      }
      ;
    })();
    s1.top = "auto";
    s1.bottom = "auto";
    s1.left = "auto";
    s1.right = "auto";
    if (truth_(bottom_QMARK_2)) {
      s1.bottom = "12px";
    } else {
      s1.top = "12px";
    }
    ;
    if (truth_(left_QMARK_4)) {
      return s1.left = "12px";
    } else {
      return s1.right = "12px";
    }
    ;
  };
  var ensure_root_BANG_ = function() {
    const or__23426__auto__1 = deref(root_el);
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const el2 = mk2("div", "dialkit-root");
      attr_BANG_(el2, "data-theme", "system");
      apply_root_position_BANG_(el2, "top-right");
      document.body.appendChild(el2);
      reset_BANG_(root_el, el2);
      window.addEventListener("pointermove", (function(e) {
        if (truth_(drag_state["on"])) {
          const dx3 = e.clientX - drag_state["sx"];
          const dy4 = e.clientY - drag_state["sy"];
          el2.style.right = "auto";
          el2.style.bottom = "auto";
          el2.style.left = `${drag_state["x"] + dx3}px`;
          return el2.style.top = `${drag_state["y"] + dy4}px`;
        }
        ;
      }));
      window.addEventListener("pointerup", (function(_) {
        return drag_state["on"] = false;
      }));
      return el2;
    }
    ;
  };
  var start_drag_BANG_ = function(e) {
    const el1 = deref(root_el);
    const rect2 = el1.getBoundingClientRect();
    drag_state["on"] = true;
    drag_state["sx"] = e.clientX;
    drag_state["sy"] = e.clientY;
    drag_state["x"] = rect2.left;
    return drag_state["y"] = rect2.top;
  };
  var build_body_BANG_ = function(panel) {
    const body1 = get(panel, "bodyEl");
    body1.innerHTML = "";
    panel["updaters"] = {};
    return render_controls_BANG_(panel, body1, get(panel, "controls"));
  };
  var select_version_BANG_ = function(panel, id) {
    panel["activePreset"] = id;
    if (truth_(id)) {
      const p1 = get(panel, "presets").find((function(x) {
        return _EQ_(x["id"], id);
      }));
      if (truth_(p1)) {
        reset_BANG_(get(panel, "store"), p1["values"]);
      }
    } else {
      reset_BANG_(get(panel, "store"), get(panel, "baseValues"));
    }
    ;
    refresh_updaters_BANG_(panel);
    notify_BANG_(panel);
    persist_save_BANG_(panel);
    return render_versions_BANG_(panel);
  };
  var save_version_BANG_ = function(panel) {
    const id1 = `v${Date.now() ?? ""}`;
    const n2 = `${"Version "}${2 + get(panel, "presets").length}`;
    const preset3 = { "id": id1, "name": n2, "values": clj__GT_js(deref(get(panel, "store"))) };
    get(panel, "presets").push(preset3);
    return select_version_BANG_(panel, id1);
  };
  var render_versions_BANG_ = function(panel) {
    const sel1 = get(panel, "versionSel");
    sel1.innerHTML = "";
    const o02 = mk2("option", null);
    o02.value = "";
    txt_BANG_(o02, "Version 1");
    add_BANG_(sel1, o02);
    for (let G__3 of iterable(get(panel, "presets"))) {
      const p4 = G__3;
      const o5 = mk2("option", null);
      o5.value = p4["id"];
      txt_BANG_(o5, p4["name"]);
      add_BANG_(sel1, o5);
    }
    ;
    return sel1.value = (() => {
      const or__23426__auto__6 = get(panel, "activePreset");
      if (truth_(or__23426__auto__6)) {
        return or__23426__auto__6;
      } else {
        return "";
      }
      ;
    })();
  };
  var copy_config_BANG_ = function(panel) {
    const vals1 = JSON.stringify(clj__GT_js(deref(get(panel, "store"))), null, 2);
    const text2 = `${'// DialKit values for "'}${get(panel, "name") ?? ""}${'"\n'}${"// Replace your config defaults with these tuned values:\n"}${vals1 ?? ""}`;
    if (truth_(navigator.clipboard)) {
      navigator.clipboard.writeText(text2);
    }
    ;
    const temp__23062__auto__3 = window.__uiToast;
    if (truth_(temp__23062__auto__3)) {
      const t4 = temp__23062__auto__3;
      return t4("Copied values to clipboard", { "variant": "success" });
    }
    ;
  };
  var submit_on_enter_BANG_ = function(card, e) {
    const t1 = e.target;
    const tag2 = t1.tagName;
    if (truth_(e.key === "Enter" && (not(e.isComposing) && (not(e.shiftKey) && (() => {
      const or__23426__auto__3 = tag2 === "INPUT" && t1.type === "text";
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return tag2 === "TEXTAREA" && (() => {
          const or__23426__auto__4 = e.ctrlKey;
          if (truth_(or__23426__auto__4)) {
            return or__23426__auto__4;
          } else {
            return e.metaKey;
          }
          ;
        })();
      }
      ;
    })())))) {
      const temp__23062__auto__5 = card.querySelector("[data-dial-submit]");
      if (truth_(temp__23062__auto__5)) {
        const btn6 = temp__23062__auto__5;
        e.preventDefault();
        t1.blur();
        return btn6.click();
      }
      ;
    }
    ;
  };
  var make_panel = function(name, config, opts) {
    const id1 = (() => {
      const or__23426__auto__2 = opts["id"];
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return `dial-${Math.random().toString(36).slice(2, 8) ?? ""}`;
      }
      ;
    })();
    const controls3 = parse_config(config, []);
    const store4 = atom({});
    const panel5 = { "open": not(opts["defaultCollapsed"]), "store": store4, "activePreset": null, "name": name, "persist": (() => {
      const p6 = opts["persist"];
      if (p6 === true) {
        return {};
      } else {
        if (tof(p6) === "object") {
          return p6;
        } else {
          if ("else") {
            return null;
          } else {
            return null;
          }
        }
      }
      ;
    })(), "controls": controls3, "icon": (() => {
      const or__23426__auto__7 = opts["icon"];
      if (truth_(or__23426__auto__7)) {
        return or__23426__auto__7;
      } else {
        return "\u2699";
      }
      ;
    })(), "onAction": opts["onAction"], "presets": [], "id": id1, "position": (() => {
      const or__23426__auto__8 = opts["position"];
      if (truth_(or__23426__auto__8)) {
        return or__23426__auto__8;
      } else {
        return "top-right";
      }
      ;
    })(), "subs": [], "onChange": opts["onChange"], "updaters": {} };
    init_values_BANG_(store4, controls3);
    panel5["baseValues"] = clj__GT_js(deref(store4));
    persist_load_BANG_(panel5);
    return panel5;
  };
  var mount_panel_BANG_ = function(panel, opts) {
    const inline1 = opts["container"];
    const root2 = (() => {
      const or__23426__auto__3 = inline1;
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return ensure_root_BANG_();
      }
      ;
    })();
    const pos4 = get(panel, "position");
    const side5 = truth_((() => {
      const or__23426__auto__6 = pos4 === "top-left";
      if (or__23426__auto__6) {
        return or__23426__auto__6;
      } else {
        return pos4 === "bottom-left";
      }
      ;
    })()) ? "pos-left" : "pos-right";
    const card7 = mk2("div", truth_(inline1) ? "dial-panel dial-panel--inline" : `${"dial-panel "}${side5 ?? ""}`);
    const head8 = mk2("div", "dial-panel-head");
    const title9 = mk2("div", "dial-panel-title");
    const tools10 = mk2("div", "dial-panel-tools");
    const vsel11 = mk2("select", "dial-version");
    const addb12 = mk2("button", "dial-tool");
    const copyb13 = mk2("button", "dial-tool");
    const resetb14 = mk2("button", "dial-tool");
    const collb15 = mk2("button", "dial-tool");
    const iconb16 = mk2("button", "dial-panel-icon");
    const body17 = mk2("div", "dial-panel-body");
    if (truth_(inline1)) {
    } else {
      apply_root_position_BANG_(root2, pos4);
    }
    ;
    txt_BANG_(title9, get(panel, "name"));
    panel["bodyEl"] = body17;
    panel["versionSel"] = vsel11;
    panel["cardEl"] = card7;
    attr_BANG_(iconb16, "type", "button");
    attr_BANG_(iconb16, "data-tooltip", `${"Expand "}${get(panel, "name") ?? ""}`);
    attr_BANG_(iconb16, "aria-label", `${"Expand "}${get(panel, "name") ?? ""}`);
    txt_BANG_(iconb16, get(panel, "icon"));
    for (let G__18 of iterable([[addb12, "+", "Save version"], [copyb13, "\u29C9", "Copy values"], [resetb14, "\u21BA", "Reset"], [collb15, "\u2013", "Minimize"]])) {
      const spec19 = G__18;
      const b20 = spec19[0];
      attr_BANG_(b20, "type", "button");
      attr_BANG_(b20, "data-tooltip", spec19[2]);
      attr_BANG_(b20, "aria-label", spec19[2]);
      txt_BANG_(b20, spec19[1]);
    }
    ;
    on_BANG_(vsel11, "change", (function(_) {
      return select_version_BANG_(panel, (() => {
        const v21 = vsel11.value;
        if (truth_(seq(v21))) {
          return v21;
        }
        ;
      })());
    }));
    on_BANG_(addb12, "click", (function(_) {
      return save_version_BANG_(panel);
    }));
    on_BANG_(copyb13, "click", (function(_) {
      return copy_config_BANG_(panel);
    }));
    on_BANG_(resetb14, "click", (function(_) {
      return select_version_BANG_(panel, null);
    }));
    on_BANG_(collb15, "click", (function(_) {
      card7.classList.add("is-iconified");
      return panel["open"] = false;
    }));
    on_BANG_(iconb16, "click", (function(_) {
      card7.classList.remove("is-iconified");
      return panel["open"] = true;
    }));
    on_BANG_(card7, "keydown", (function(e) {
      return submit_on_enter_BANG_(card7, e);
    }));
    on_BANG_(head8, "pointerdown", (function(e) {
      if (truth_((() => {
        const or__23426__auto__22 = _EQ_(e.target, head8);
        if (or__23426__auto__22) {
          return or__23426__auto__22;
        } else {
          return _EQ_(e.target, title9);
        }
        ;
      })())) {
        return start_drag_BANG_(e);
      }
      ;
    }));
    add_BANG_(tools10, vsel11, addb12, copyb13, resetb14, collb15);
    add_BANG_(head8, title9, tools10);
    if (truth_(inline1)) {
      add_BANG_(card7, body17);
    } else {
      add_BANG_(card7, head8, body17, iconb16);
    }
    ;
    add_BANG_(root2, card7);
    build_body_BANG_(panel);
    render_versions_BANG_(panel);
    if (truth_(opts["defaultCollapsed"])) {
      card7.classList.add("is-iconified");
      panel["open"] = false;
    }
    ;
    return card7;
  };
  var setvals_walk_BANG_ = function(panel, prefix, o) {
    for (let G__1 of iterable(Object.keys(o))) {
      const k2 = G__1;
      const v3 = o[k2];
      const path4 = conj(prefix, k2);
      if (truth_(tof(v3) === "object" && (not(Array.isArray(v3)) && not(get(panel, "updaters")[path_str(path4)])))) {
        setvals_walk_BANG_(panel, path4, v3);
      } else {
        set_value_BANG_2(panel, path4, v3);
      }
    }
    return null;
  };
  var controller = function(panel) {
    return { "setOpen": (function(o) {
      panel["open"] = o;
      const temp__23062__auto__1 = get(panel, "cardEl");
      if (truth_(temp__23062__auto__1)) {
        const card2 = temp__23062__auto__1;
        if (truth_(o)) {
          card2.classList.remove("is-iconified");
        } else {
          card2.classList.add("is-iconified");
        }
      }
      ;
      return void 0;
    }), "destroy": (function() {
      const temp__23062__auto__3 = get(panel, "cardEl");
      if (truth_(temp__23062__auto__3)) {
        const card4 = temp__23062__auto__3;
        card4.remove();
      }
      ;
      delete deref(panels)[get(panel, "id")];
      return void 0;
    }), "setValue": (function(p, v) {
      set_value_BANG_2(panel, p.split("."), v);
      return void 0;
    }), "getOpen": (function() {
      return get(panel, "open");
    }), "id": get(panel, "id"), "setValues": (function(obj) {
      setvals_walk_BANG_(panel, [], obj);
      return void 0;
    }), "resetValues": (function() {
      select_version_BANG_(panel, null);
      return void 0;
    }), "getValues": (function() {
      return clj__GT_js(deref(get(panel, "store")));
    }), "subscribe": (function(cb, immediate) {
      get(panel, "subs").push(cb);
      if (!(immediate === false)) {
        cb(clj__GT_js(deref(get(panel, "store"))));
      }
      ;
      return function() {
        const i5 = get(panel, "subs").indexOf(cb);
        if (i5 >= 0) {
          return get(panel, "subs").splice(i5, 1);
        }
        ;
      };
    }) };
  };
  var use_dial = function(name, config, opts) {
    const opts1 = (() => {
      const or__23426__auto__2 = opts;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return {};
      }
      ;
    })();
    const panel3 = make_panel(name, config, opts1);
    const ctrl4 = controller(panel3);
    deref(panels)[get(panel3, "id")] = panel3;
    panel3["ctrl"] = ctrl4;
    Object.defineProperty(ctrl4, "values", { "get": (function() {
      return clj__GT_js(deref(get(panel3, "store")));
    }) });
    if (!(opts1["enabled"] === false)) {
      mount_panel_BANG_(panel3, opts1);
    }
    ;
    return ctrl4;
  };
  var panel_by_id = function(id) {
    return deref(panels)[id];
  };
  var dial_store = { "setPanelOpen": (function(id, o) {
    const temp__23062__auto__1 = panel_by_id(id);
    if (truth_(temp__23062__auto__1)) {
      const p2 = temp__23062__auto__1;
      return get(p2, "ctrl")["setOpen"](o);
    }
    ;
  }), "togglePanelOpen": (function(id) {
    const temp__23062__auto__3 = panel_by_id(id);
    if (truth_(temp__23062__auto__3)) {
      const p4 = temp__23062__auto__3;
      return get(p4, "ctrl")["setOpen"](not(get(p4, "open")));
    }
    ;
  }), "getPanelOpen": (function(id) {
    const temp__23062__auto__5 = panel_by_id(id);
    if (truth_(temp__23062__auto__5)) {
      const p6 = temp__23062__auto__5;
      return get(p6, "open");
    }
    ;
  }), "getPresets": (function(id) {
    const temp__23062__auto__7 = panel_by_id(id);
    if (truth_(temp__23062__auto__7)) {
      const p8 = temp__23062__auto__7;
      return get(p8, "presets");
    }
    ;
  }), "getActivePresetId": (function(id) {
    const temp__23062__auto__9 = panel_by_id(id);
    if (truth_(temp__23062__auto__9)) {
      const p10 = temp__23062__auto__9;
      return get(p10, "activePreset");
    }
    ;
  }), "clearActivePreset": (function(id) {
    const temp__23062__auto__11 = panel_by_id(id);
    if (truth_(temp__23062__auto__11)) {
      const p12 = temp__23062__auto__11;
      return select_version_BANG_(p12, null);
    }
    ;
  }), "savePreset": (function(id) {
    const temp__23062__auto__13 = panel_by_id(id);
    if (truth_(temp__23062__auto__13)) {
      const p14 = temp__23062__auto__13;
      return save_version_BANG_(p14);
    }
    ;
  }), "deletePreset": (function(id, pid) {
    const temp__23062__auto__15 = panel_by_id(id);
    if (truth_(temp__23062__auto__15)) {
      const p16 = temp__23062__auto__15;
      p16["presets"] = get(p16, "presets").filter((function(x) {
        return !_EQ_(x["id"], pid);
      }));
      render_versions_BANG_(p16);
      return persist_save_BANG_(p16);
    }
    ;
  }) };
  window["__uiDial"] = use_dial;
  window["DialStore"] = dial_store;

  // .compiled/drop_zone.mjs
  var closest_zone = function(el2) {
    if (truth_((() => {
      const and__23442__auto__1 = el2;
      if (truth_(and__23442__auto__1)) {
        return el2.closest;
      } else {
        return and__23442__auto__1;
      }
      ;
    })())) {
      return el2.closest("[data-ui-drop-zone]");
    }
    ;
  };
  var emit_files_BANG_ = function(zone, files) {
    if (files.length > 0) {
      return zone.dispatchEvent(new CustomEvent("ui:drop-zone-files", { "bubbles": true, "detail": { "files": files } }));
    }
    ;
  };
  var on_dragover = function(e) {
    const temp__23062__auto__1 = closest_zone(e.target);
    if (truth_(temp__23062__auto__1)) {
      const zone2 = temp__23062__auto__1;
      e.preventDefault();
      return zone2.classList.add("drop-zone-active");
    }
    ;
  };
  var on_dragleave = function(e) {
    const temp__23062__auto__1 = closest_zone(e.target);
    if (truth_(temp__23062__auto__1)) {
      const zone2 = temp__23062__auto__1;
      if (truth_((() => {
        const and__23442__auto__3 = e.relatedTarget;
        if (truth_(and__23442__auto__3)) {
          return zone2.contains(e.relatedTarget);
        } else {
          return and__23442__auto__3;
        }
        ;
      })())) {
        return null;
      } else {
        return zone2.classList.remove("drop-zone-active");
      }
      ;
    }
    ;
  };
  var on_drop = function(e) {
    const temp__23062__auto__1 = closest_zone(e.target);
    if (truth_(temp__23062__auto__1)) {
      const zone2 = temp__23062__auto__1;
      e.preventDefault();
      zone2.classList.remove("drop-zone-active");
      return emit_files_BANG_(zone2, Array.from(e.dataTransfer.files));
    }
    ;
  };
  var on_change2 = function(e) {
    const input1 = e.target;
    if (truth_((() => {
      const and__23442__auto__2 = input1;
      if (truth_(and__23442__auto__2)) {
        const and__23442__auto__3 = input1.matches;
        if (truth_(and__23442__auto__3)) {
          return input1.matches('input[type="file"]');
        } else {
          return and__23442__auto__3;
        }
        ;
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      const temp__23062__auto__4 = closest_zone(input1);
      if (truth_(temp__23062__auto__4)) {
        const zone5 = temp__23062__auto__4;
        return emit_files_BANG_(zone5, Array.from(input1.files));
      }
      ;
    }
    ;
  };
  var init_BANG_3 = function() {
    document.addEventListener("dragover", on_dragover);
    document.addEventListener("dragleave", on_dragleave);
    document.addEventListener("drop", on_drop);
    return document.addEventListener("change", on_change2);
  };
  init_BANG_3();

  // .compiled/flip.mjs
  var default_duration = 250;
  var default_easing = "cubic-bezier(0.32, 0.72, 0, 1)";
  var reduced_motion_QMARK_2 = function() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  };
  var resolve_els = function(target) {
    if (truth_(string_QMARK_(target))) {
      return Array.from(document.querySelectorAll(target));
    } else {
      if (truth_((() => {
        const c__23404__auto__1 = Element;
        const x__23405__auto__2 = target;
        const ret__23406__auto__3 = x__23405__auto__2 instanceof c__23404__auto__1;
        return ret__23406__auto__3;
      })())) {
        return Array.from(target.children);
      } else {
        if ("else") {
          return Array.from(target);
        } else {
          return null;
        }
      }
    }
    ;
  };
  var capture = function(target) {
    const rects1 = /* @__PURE__ */ new Map();
    for (let G__2 of iterable(resolve_els(target))) {
      const el3 = G__2;
      rects1.set(el3, el3.getBoundingClientRect());
    }
    ;
    return { "target": target, "rects": rects1 };
  };
  var play_moves_BANG_ = function(rects, timing, scale_QMARK_, lift_QMARK_) {
    return rects.forEach((function(rect, el2) {
      if (truth_(el2.isConnected)) {
        const now1 = el2.getBoundingClientRect();
        const dx2 = rect.x - now1.x;
        const dy3 = rect.y - now1.y;
        const sx4 = truth_((() => {
          const and__23442__auto__5 = scale_QMARK_;
          if (truth_(and__23442__auto__5)) {
            return now1.width > 0;
          } else {
            return and__23442__auto__5;
          }
          ;
        })()) ? rect.width / now1.width : 1;
        const sy6 = truth_((() => {
          const and__23442__auto__7 = scale_QMARK_;
          if (truth_(and__23442__auto__7)) {
            return now1.height > 0;
          } else {
            return and__23442__auto__7;
          }
          ;
        })()) ? rect.height / now1.height : 1;
        const moved_QMARK_8 = (() => {
          const or__23426__auto__9 = Math.abs(dx2) >= 0.5;
          if (or__23426__auto__9) {
            return or__23426__auto__9;
          } else {
            const or__23426__auto__10 = Math.abs(dy3) >= 0.5;
            if (or__23426__auto__10) {
              return or__23426__auto__10;
            } else {
              const or__23426__auto__11 = Math.abs(sx4 - 1) >= 5e-3;
              if (or__23426__auto__11) {
                return or__23426__auto__11;
              } else {
                return Math.abs(sy6 - 1) >= 5e-3;
              }
              ;
            }
            ;
          }
          ;
        })();
        if (truth_(moved_QMARK_8)) {
          if (truth_(lift_QMARK_)) {
            el2.style.zIndex = "1";
          }
          ;
          const from12 = `${"translate("}${dx2 ?? ""}${"px, "}${dy3 ?? ""}${"px)"}${(truth_(scale_QMARK_) ? `${" scale("}${sx4 ?? ""}${", "}${sy6 ?? ""}${")"}` : "") ?? ""}`;
          const kf13 = { "transform": [from12, "none"] };
          const _14 = truth_(scale_QMARK_) ? kf13["transformOrigin"] = ["0 0", "0 0"] : null;
          const anim15 = el2.animate(kf13, timing);
          const unlift16 = (function() {
            return el2.style.zIndex = "";
          });
          if (truth_(lift_QMARK_)) {
            anim15.onfinish = unlift16;
            return anim15.oncancel = unlift16;
          }
          ;
        }
        ;
      }
      ;
    }));
  };
  var play_enters_BANG_ = function(target, rects, timing) {
    for (let G__1 of iterable(resolve_els(target))) {
      const el2 = G__1;
      if (truth_(rects.has(el2))) {
      } else {
        el2.animate({ "opacity": [0, 1], "transform": ["scale(0.96)", "none"] }, timing);
      }
    }
    return null;
  };
  var play = (() => {
    const f524 = (function(...args525) {
      const G__5261 = args525.length;
      switch (G__5261) {
        case 1:
          return f524.cljs$core$IFn$_invoke$arity$1(args525[0]);
          break;
        case 2:
          return f524.cljs$core$IFn$_invoke$arity$2(args525[0], args525[1]);
          break;
        default:
          throw new Error(`${"Invalid arity: "}${args525.length ?? ""}`);
      }
      ;
    });
    f524.cljs$core$IFn$_invoke$arity$1 = (function(snap) {
      return play(snap, null);
    });
    f524.cljs$core$IFn$_invoke$arity$2 = (function(snap, opts) {
      if (truth_(reduced_motion_QMARK_2())) {
      } else {
        const opts3 = (() => {
          const or__23426__auto__4 = opts;
          if (truth_(or__23426__auto__4)) {
            return or__23426__auto__4;
          } else {
            return {};
          }
          ;
        })();
        const timing5 = { "duration": (() => {
          const or__23426__auto__6 = opts3["duration"];
          if (truth_(or__23426__auto__6)) {
            return or__23426__auto__6;
          } else {
            return default_duration;
          }
          ;
        })(), "easing": (() => {
          const or__23426__auto__7 = opts3["easing"];
          if (truth_(or__23426__auto__7)) {
            return or__23426__auto__7;
          } else {
            return default_easing;
          }
          ;
        })() };
        const rects8 = snap["rects"];
        play_moves_BANG_(rects8, timing5, truth_(opts3["scale"]) ? true : false, truth_(opts3["lift"]) ? true : false);
        if (truth_(opts3["enter"])) {
          play_enters_BANG_(snap["target"], rects8, timing5);
        }
      }
      ;
      return null;
    });
    f524.cljs$lang$maxFixedArity = 2;
    return f524;
  })();
  var wrap = (() => {
    const f527 = (function(...args528) {
      const G__5291 = args528.length;
      switch (G__5291) {
        case 2:
          return f527.cljs$core$IFn$_invoke$arity$2(args528[0], args528[1]);
          break;
        case 3:
          return f527.cljs$core$IFn$_invoke$arity$3(args528[0], args528[1], args528[2]);
          break;
        default:
          throw new Error(`${"Invalid arity: "}${args528.length ?? ""}`);
      }
      ;
    });
    f527.cljs$core$IFn$_invoke$arity$2 = (function(target, mutate) {
      return wrap(target, mutate, null);
    });
    f527.cljs$core$IFn$_invoke$arity$3 = (function(target, mutate, opts) {
      const snap3 = capture(target);
      mutate();
      return play(snap3, opts);
    });
    f527.cljs$lang$maxFixedArity = 3;
    return f527;
  })();
  window.__uiFlip = { "capture": capture, "play": play, "wrap": wrap };

  // .compiled/gestures.mjs
  var press_ms = 500;
  var press_visual_ms = 250;
  var slop_px = 10;
  var selector = ".context-menu-trigger, [data-long-press]";
  var press_class = "clj-ui-pressing";
  var press = atom(null);
  var suppress_click_QMARK_ = atom(false);
  var held = atom(null);
  var clear_press_visual_BANG_ = function(el2) {
    if (truth_(el2)) {
      return el2.classList.remove(press_class);
    }
    ;
  };
  var clear_held_BANG_ = function() {
    const temp__23062__auto__1 = deref(held);
    if (truth_(temp__23062__auto__1)) {
      const el2 = temp__23062__auto__1;
      clear_press_visual_BANG_(el2);
      return reset_BANG_(held, null);
    }
    ;
  };
  var cancel_BANG_ = function() {
    const temp__23062__auto__1 = deref(press);
    if (truth_(temp__23062__auto__1)) {
      const p2 = temp__23062__auto__1;
      clearTimeout(get(p2, "timer"));
      clearTimeout(get(p2, "visual-timer"));
      clear_press_visual_BANG_(get(p2, "el"));
      return reset_BANG_(press, null);
    }
    ;
  };
  var dispatch_contextmenu_BANG_ = function(el2, x, y) {
    return el2.dispatchEvent(new MouseEvent("contextmenu", { "bubbles": true, "cancelable": true, "view": window, "clientX": x, "clientY": y }));
  };
  var on_pointerdown2 = function(e) {
    reset_BANG_(suppress_click_QMARK_, false);
    clear_held_BANG_();
    if (e.pointerType === "touch") {
      const temp__23062__auto__1 = (() => {
        const G__5302 = e.target;
        if (G__5302 == null) {
          return null;
        } else {
          return G__5302.closest(selector);
        }
        ;
      })();
      if (truth_(temp__23062__auto__1)) {
        const el3 = temp__23062__auto__1;
        cancel_BANG_();
        const x4 = e.clientX;
        const y5 = e.clientY;
        return reset_BANG_(press, { "el": el3, "x": x4, "y": y5, "visual-timer": setTimeout((function() {
          return el3.classList.add(press_class);
        }), press_visual_ms), "timer": setTimeout((function() {
          clearTimeout(get(deref(press), "visual-timer"));
          reset_BANG_(press, null);
          el3.classList.add(press_class);
          reset_BANG_(held, el3);
          reset_BANG_(suppress_click_QMARK_, true);
          return dispatch_contextmenu_BANG_(el3, x4, y5);
        }), press_ms) });
      }
      ;
    }
    ;
  };
  var on_pointermove3 = function(e) {
    const temp__23062__auto__1 = deref(press);
    if (truth_(temp__23062__auto__1)) {
      const p2 = temp__23062__auto__1;
      if (truth_((() => {
        const or__23426__auto__3 = Math.abs(e.clientX - get(p2, "x")) > slop_px;
        if (or__23426__auto__3) {
          return or__23426__auto__3;
        } else {
          return Math.abs(e.clientY - get(p2, "y")) > slop_px;
        }
        ;
      })())) {
        return cancel_BANG_();
      }
      ;
    }
    ;
  };
  var on_pointer_end = function(_e) {
    return cancel_BANG_();
  };
  var on_native_contextmenu = function(e) {
    if (truth_(e.isTrusted)) {
      const temp__23062__auto__1 = deref(press);
      if (truth_(temp__23062__auto__1)) {
        const p2 = temp__23062__auto__1;
        clearTimeout(get(p2, "timer"));
        clearTimeout(get(p2, "visual-timer"));
        get(p2, "el").classList.add(press_class);
        reset_BANG_(press, null);
        return reset_BANG_(held, get(p2, "el"));
      }
      ;
    }
    ;
  };
  var on_click_capture = function(e) {
    if (truth_(deref(suppress_click_QMARK_))) {
      reset_BANG_(suppress_click_QMARK_, false);
      e.preventDefault();
      return e.stopPropagation();
    }
    ;
  };
  document.addEventListener("pointerdown", on_pointerdown2, true);
  document.addEventListener("pointermove", on_pointermove3, true);
  document.addEventListener("pointerup", on_pointer_end, true);
  document.addEventListener("pointercancel", on_pointer_end, true);
  document.addEventListener("contextmenu", on_native_contextmenu, true);
  document.addEventListener("click", on_click_capture, true);
  document.addEventListener("clj-ui-menu-dismiss", (function(_) {
    return clear_held_BANG_();
  }));
  window["__uiLongPress"] = dispatch_contextmenu_BANG_;

  // .compiled/lightbox.mjs
  var overlay_sel = ".lightbox-overlay";
  var image_sel = ".lightbox-image";
  var max_scale = 5;
  var pinch_min = 0.75;
  var pinch_max = 6;
  var double_tap_scale = 2.5;
  var double_tap_ms = 300;
  var tap_slop_px = 10;
  var double_tap_slop_px = 30;
  var identity_zoom = { "s": 1, "tx": 0, "ty": 0 };
  var pointers = /* @__PURE__ */ new Map();
  var gesture = atom(null);
  var suppress_click_QMARK_2 = atom(false);
  var last_tap = atom(null);
  var clamp3 = function(lo, hi, v) {
    return Math.min(hi, Math.max(lo, v));
  };
  var rel = function(x, y) {
    return [x - window.innerWidth / 2, y - window.innerHeight / 2];
  };
  var zoom_of = function(img) {
    const or__23426__auto__1 = img["__uiZoom"];
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      return identity_zoom;
    }
    ;
  };
  var zoom_at = function(z, s, px2, py) {
    const k1 = s / get(z, "s");
    return assoc(z, "s", s, "tx", px2 - k1 * (px2 - get(z, "tx")), "ty", py - k1 * (py - get(z, "ty")));
  };
  var settle = function(img, z) {
    const s1 = clamp3(1, max_scale, get(z, "s"));
    const mx2 = Math.max(0, (s1 * img.offsetWidth - window.innerWidth) / 2);
    const my3 = Math.max(0, (s1 * img.offsetHeight - window.innerHeight) / 2);
    return assoc(z, "s", s1, "tx", clamp3(-mx2, mx2, get(z, "tx")), "ty", clamp3(-my3, my3, get(z, "ty")));
  };
  var apply_zoom_BANG_ = function(img, z, animate_QMARK_) {
    img["__uiZoom"] = z;
    const st1 = img.style;
    st1.transition = truth_(animate_QMARK_) ? "" : "none";
    st1.transform = 1 === get(z, "s") ? "" : `${"translate("}${get(z, "tx") ?? ""}${"px, "}${get(z, "ty") ?? ""}${"px) scale("}${get(z, "s") ?? ""}${")"}`;
    if (get(z, "s") > 1) {
      return img.setAttribute("data-zoomed", "");
    } else {
      return img.removeAttribute("data-zoomed");
    }
    ;
  };
  var reset_zoom_BANG_ = function(img) {
    img["__uiZoom"] = null;
    img.style.transform = "";
    return img.removeAttribute("data-zoomed");
  };
  var current_points = function() {
    return Array.from(pointers.values());
  };
  var rebase_BANG_ = function() {
    const temp__23062__auto__1 = deref(gesture);
    if (truth_(temp__23062__auto__1)) {
      const g2 = temp__23062__auto__1;
      const pts3 = current_points();
      return reset_BANG_(gesture, assoc(g2, "z0", zoom_of(get(g2, "img")), "pts0", pts3, "max-pointers", Math.max(get(g2, "max-pointers"), pts3.length)));
    }
    ;
  };
  var midpoint = function(a, b) {
    return rel((get(a, "x") + get(b, "x")) / 2, (get(a, "y") + get(b, "y")) / 2);
  };
  var distance = function(a, b) {
    return Math.hypot(get(a, "x") - get(b, "x"), get(a, "y") - get(b, "y"));
  };
  var track_BANG_ = function() {
    const map__12 = deref(gesture);
    const img3 = get(map__12, "img");
    const z04 = get(map__12, "z0");
    const pts05 = get(map__12, "pts0");
    const pts6 = current_points();
    if (pts6.length >= 2) {
      const a013 = pts05[0];
      const b014 = pts05[1];
      const a15 = pts6[0];
      const b16 = pts6[1];
      const vec__717 = midpoint(a013, b014);
      const mx018 = nth(vec__717, 0, null);
      const my019 = nth(vec__717, 1, null);
      const vec__1020 = midpoint(a15, b16);
      const mx21 = nth(vec__1020, 0, null);
      const my22 = nth(vec__1020, 1, null);
      const s23 = clamp3(pinch_min, pinch_max, get(z04, "s") * (distance(a15, b16) / Math.max(1, distance(a013, b014))));
      const z24 = zoom_at(z04, s23, mx018, my019);
      return apply_zoom_BANG_(img3, assoc(z24, "tx", get(z24, "tx") + (mx21 - mx018), "ty", get(z24, "ty") + (my22 - my019)), false);
    } else {
      if (get(z04, "s") > 1) {
        const p025 = pts05[0];
        const p26 = pts6[0];
        return apply_zoom_BANG_(img3, assoc(z04, "tx", get(z04, "tx") + (get(p26, "x") - get(p025, "x")), "ty", get(z04, "ty") + (get(p26, "y") - get(p025, "y"))), false);
      }
    }
    ;
  };
  var tap_BANG_ = function(img, x, y) {
    const now1 = Date.now();
    const prev2 = deref(last_tap);
    if (truth_((() => {
      const and__23442__auto__3 = prev2;
      if (truth_(and__23442__auto__3)) {
        return now1 - get(prev2, "t") < double_tap_ms && Math.hypot(x - get(prev2, "x"), y - get(prev2, "y")) < double_tap_slop_px;
      } else {
        return and__23442__auto__3;
      }
      ;
    })())) {
      const z7 = zoom_of(img);
      const vec__48 = rel(x, y);
      const px9 = nth(vec__48, 0, null);
      const py10 = nth(vec__48, 1, null);
      reset_BANG_(last_tap, null);
      return apply_zoom_BANG_(img, get(z7, "s") > 1 ? identity_zoom : settle(img, zoom_at(z7, double_tap_scale, px9, py10)), true);
    } else {
      return reset_BANG_(last_tap, { "t": now1, "x": x, "y": y });
    }
    ;
  };
  var on_pointerdown3 = function(e) {
    const t1 = e.target;
    const overlay2 = (() => {
      const G__5313 = t1;
      if (G__5313 == null) {
        return null;
      } else {
        return G__5313.closest(overlay_sel);
      }
      ;
    })();
    if (truth_((() => {
      const and__23442__auto__4 = overlay2;
      if (truth_(and__23442__auto__4)) {
        return not(t1.closest(".lightbox-close")) && (() => {
          const or__23426__auto__5 = !("mouse" === e.pointerType);
          if (or__23426__auto__5) {
            return or__23426__auto__5;
          } else {
            return e.button === 0;
          }
          ;
        })();
      } else {
        return and__23442__auto__4;
      }
      ;
    })())) {
      const temp__23062__auto__6 = overlay2.querySelector(image_sel);
      if (truth_(temp__23062__auto__6)) {
        const img7 = temp__23062__auto__6;
        if (truth_((() => {
          const or__23426__auto__8 = pointers.size === 0;
          if (or__23426__auto__8) {
            return or__23426__auto__8;
          } else {
            return !_EQ_(img7, get(deref(gesture), "img"));
          }
          ;
        })())) {
          pointers.clear();
          reset_BANG_(suppress_click_QMARK_2, false);
          reset_BANG_(gesture, { "img": img7, "moved": false, "on-image": img7.contains(t1), "max-pointers": 0 });
        }
        ;
        const x9 = e.clientX;
        const y10 = e.clientY;
        pointers.set(e.pointerId, { "x": x9, "y": y10, "x0": x9, "y0": y10 });
        if ("mouse" === e.pointerType) {
          e.preventDefault();
        }
        ;
        return rebase_BANG_();
      }
      ;
    }
    ;
  };
  var on_pointermove4 = function(e) {
    const id1 = e.pointerId;
    if (truth_(pointers.has(id1))) {
      const p2 = pointers.get(id1);
      const x3 = e.clientX;
      const y4 = e.clientY;
      pointers.set(id1, assoc(p2, "x", x3, "y", y4));
      if (Math.hypot(x3 - get(p2, "x0"), y4 - get(p2, "y0")) > tap_slop_px) {
        swap_BANG_(gesture, assoc, "moved", true);
      }
      ;
      return track_BANG_();
    }
    ;
  };
  var on_pointer_end2 = function(e) {
    const id1 = e.pointerId;
    if (truth_(pointers.has(id1))) {
      pointers.delete(id1);
      if (pointers.size > 0) {
        return rebase_BANG_();
      } else {
        const map__23 = deref(gesture);
        const img4 = get(map__23, "img");
        const moved5 = get(map__23, "moved");
        const on_image6 = get(map__23, "on-image");
        const max_pointers7 = get(map__23, "max-pointers");
        reset_BANG_(gesture, null);
        if (truth_(moved5)) {
          reset_BANG_(suppress_click_QMARK_2, true);
        }
        ;
        if (truth_(not(moved5) && (() => {
          const and__23442__auto__8 = on_image6;
          if (truth_(and__23442__auto__8)) {
            return 1 === max_pointers7 && "pointerup" === e.type;
          } else {
            return and__23442__auto__8;
          }
          ;
        })())) {
          return tap_BANG_(img4, e.clientX, e.clientY);
        } else {
          return apply_zoom_BANG_(img4, settle(img4, zoom_of(img4)), true);
        }
        ;
      }
      ;
    }
    ;
  };
  var on_wheel = function(e) {
    const temp__23062__auto__1 = (() => {
      const G__5322 = e.target;
      if (G__5322 == null) {
        return null;
      } else {
        return G__5322.closest(overlay_sel);
      }
      ;
    })();
    if (truth_(temp__23062__auto__1)) {
      const overlay3 = temp__23062__auto__1;
      const temp__23062__auto__4 = overlay3.querySelector(image_sel);
      if (truth_(temp__23062__auto__4)) {
        const img5 = temp__23062__auto__4;
        e.preventDefault();
        const z9 = zoom_of(img5);
        const dy10 = e.deltaY * (1 === e.deltaMode ? 16 : 1);
        const k11 = Math.exp(-dy10 * (truth_(e.ctrlKey) ? 0.01 : 2e-3));
        const vec__612 = rel(e.clientX, e.clientY);
        const px13 = nth(vec__612, 0, null);
        const py14 = nth(vec__612, 1, null);
        return apply_zoom_BANG_(img5, settle(img5, zoom_at(z9, clamp3(1, max_scale, get(z9, "s") * k11), px13, py14)), false);
      }
      ;
    }
    ;
  };
  var on_click_capture2 = function(e) {
    if (truth_(deref(suppress_click_QMARK_2))) {
      reset_BANG_(suppress_click_QMARK_2, false);
      e.preventDefault();
      return e.stopPropagation();
    }
    ;
  };
  var on_load_capture = function(e) {
    const t1 = e.target;
    if (truth_((() => {
      const and__23442__auto__2 = t1.matches;
      if (truth_(and__23442__auto__2)) {
        return t1.matches(image_sel);
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      return reset_zoom_BANG_(t1);
    }
    ;
  };
  var on_dragstart = function(e) {
    const t1 = e.target;
    if (truth_((() => {
      const and__23442__auto__2 = t1;
      if (truth_(and__23442__auto__2)) {
        const and__23442__auto__3 = t1.closest;
        if (truth_(and__23442__auto__3)) {
          return t1.closest(overlay_sel);
        } else {
          return and__23442__auto__3;
        }
        ;
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      return e.preventDefault();
    }
    ;
  };
  if (truth_(window["__uiLightboxZoom"])) {
  } else {
    window["__uiLightboxZoom"] = true;
    document.addEventListener("pointerdown", on_pointerdown3, true);
    document.addEventListener("pointermove", on_pointermove4, true);
    document.addEventListener("pointerup", on_pointer_end2, true);
    document.addEventListener("pointercancel", on_pointer_end2, true);
    document.addEventListener("click", on_click_capture2, true);
    document.addEventListener("wheel", on_wheel, { "capture": true, "passive": false });
    document.addEventListener("load", on_load_capture, true);
    document.addEventListener("dragstart", on_dragstart, true);
  }

  // .compiled/masonry.mjs
  var raf = atom(null);
  var observed = /* @__PURE__ */ new Set();
  var ro = new ResizeObserver((function(_) {
    return schedule_BANG_();
  }));
  var native_QMARK_ = function(container) {
    return "masonry" === getComputedStyle(container).gridTemplateRows;
  };
  var clear_BANG_ = function(container) {
    container.style.gridAutoRows = "";
    container.style.removeProperty("row-gap");
    for (let G__1 of iterable(Array.from(container.children))) {
      const item2 = G__1;
      item2.style.gridRowEnd = "";
    }
    return null;
  };
  var layout_BANG_ = function(container) {
    const items1 = Array.from(container.children);
    container.style.gridAutoRows = "0px";
    container.style.setProperty("row-gap", "1px", "important");
    const col_gap2 = parseFloat(getComputedStyle(container).columnGap);
    const col_gap3 = truth_(isNaN(col_gap2)) ? 0 : col_gap2;
    const heights4 = mapv((function(item) {
      return item.offsetHeight;
    }), items1);
    for (let G__5 of iterable(map(vector, items1, heights4))) {
      const vec__69 = G__5;
      const item10 = nth(vec__69, 0, null);
      const h11 = nth(vec__69, 1, null);
      item10.style.gridRowEnd = `${"span "}${Math.round(h11 + col_gap3) ?? ""}`;
    }
    return null;
  };
  var run_BANG_ = function() {
    const els1 = Array.from(document.querySelectorAll(".tile-grid-masonry"));
    for (let G__2 of iterable(els1)) {
      const el3 = G__2;
      if (truth_(observed.has(el3))) {
      } else {
        observed.add(el3);
        ro.observe(el3);
      }
    }
    ;
    for (let G__4 of iterable(Array.from(observed))) {
      const el5 = G__4;
      if (truth_((() => {
        const or__23426__auto__6 = not(el5.isConnected);
        if (or__23426__auto__6) {
          return or__23426__auto__6;
        } else {
          return not(el5.classList.contains("tile-grid-masonry"));
        }
        ;
      })())) {
        observed.delete(el5);
        ro.unobserve(el5);
        if (truth_(el5.isConnected)) {
          clear_BANG_(el5);
        }
      }
    }
    ;
    for (let G__7 of iterable(els1)) {
      const el8 = G__7;
      if (truth_(native_QMARK_(el8))) {
        clear_BANG_(el8);
      } else {
        layout_BANG_(el8);
      }
    }
    return null;
  };
  var schedule_BANG_ = function() {
    if (truth_(deref(raf))) {
      cancelAnimationFrame(deref(raf));
    }
    ;
    return reset_BANG_(raf, requestAnimationFrame((function(_) {
      reset_BANG_(raf, null);
      return run_BANG_();
    })));
  };
  var init_BANG_4 = function() {
    schedule_BANG_();
    const obs1 = new MutationObserver((function(_, _2) {
      return schedule_BANG_();
    }));
    obs1.observe(document.body, { "childList": true, "subtree": true, "attributes": true, "attributeFilter": ["class"] });
    return document.addEventListener("load", (function(e) {
      const target3 = e.target;
      if (truth_("IMG" === target3.tagName && target3.closest(".tile-grid-masonry"))) {
        return schedule_BANG_();
      }
      ;
    }), true);
  };
  window["__uiMasonry"] = schedule_BANG_;
  if ("loading" === document.readyState) {
    document.addEventListener("DOMContentLoaded", init_BANG_4);
  } else {
    init_BANG_4();
  }

  // .compiled/ui/util.mjs
  var kw_name = function(s) {
    return s;
  };
  var class_attrs = function(class_list, class$, attrs) {
    return merge({ "class": (() => {
      const G__10081 = join(" ", class_list);
      if (truth_(class$)) {
        return `${G__10081 ?? ""}${" "}${class$ ?? ""}`;
      } else {
        return G__10081;
      }
      ;
    })() }, attrs);
  };

  // .compiled/ui/icon.mjs
  var icon_paths = { "arrow-left": [["path", { "d": "m12 19-7-7 7-7" }], ["path", { "d": "M19 12H5" }]], "moon": [["path", { "d": "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401" }]], "terminal": [["path", { "d": "m4 17 6-6-6-6" }], ["path", { "d": "M12 19h8" }]], "shuffle": [["path", { "d": "m18 14 4 4-4 4" }], ["path", { "d": "m18 2 4 4-4 4" }], ["path", { "d": "M2 18h1.973a4 4 0 0 0 3.3-1.7l5.454-8.6a4 4 0 0 1 3.3-1.7H22" }], ["path", { "d": "M2 6h1.972a4 4 0 0 1 3.6 2.2" }], ["path", { "d": "M22 18h-6.041a4 4 0 0 1-3.3-1.8l-.359-.45" }]], "skip-back": [["path", { "d": "M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z" }], ["path", { "d": "M3 20V4" }]], "file-text": [["path", { "d": "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z" }], ["path", { "d": "M14 2v5a1 1 0 0 0 1 1h5" }], ["path", { "d": "M16 13H8" }], ["path", { "d": "M16 17H8" }], ["path", { "d": "M10 9H8" }]], "layout-dashboard": [["rect", { "width": "7", "height": "9", "x": "3", "y": "3", "rx": "1" }], ["rect", { "width": "7", "height": "5", "x": "14", "y": "3", "rx": "1" }], ["rect", { "width": "7", "height": "9", "x": "14", "y": "12", "rx": "1" }], ["rect", { "width": "7", "height": "5", "x": "3", "y": "16", "rx": "1" }]], "package": [["path", { "d": "M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z" }], ["path", { "d": "M12 22V12" }], ["polyline", { "points": "3.29 7 12 12 20.71 7" }], ["path", { "d": "m7.5 4.27 9 5.15" }]], "arrow-right": [["path", { "d": "M5 12h14" }], ["path", { "d": "m12 5 7 7-7 7" }]], "log-out": [["path", { "d": "m16 17 5-5-5-5" }], ["path", { "d": "M21 12H9" }], ["path", { "d": "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" }]], "home": [["path", { "d": "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8" }], ["path", { "d": "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" }]], "copy": [["rect", { "width": "14", "height": "14", "x": "8", "y": "8", "rx": "2", "ry": "2" }], ["path", { "d": "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2" }]], "heart": [["path", { "d": "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5" }]], "chevron-left": [["path", { "d": "m15 18-6-6 6-6" }]], "book-open": [["path", { "d": "M12 7v14" }], ["path", { "d": "M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z" }]], "inbox": [["polyline", { "points": "22 12 16 12 14 15 10 15 8 12 2 12" }], ["path", { "d": "M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" }]], "circle-x": [["circle", { "cx": "12", "cy": "12", "r": "10" }], ["path", { "d": "m15 9-6 6" }], ["path", { "d": "m9 9 6 6" }]], "zap": [["path", { "d": "M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z" }]], "bookmark": [["path", { "d": "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z" }]], "music": [["path", { "d": "M9 18V5l12-2v13" }], ["circle", { "cx": "6", "cy": "18", "r": "3", "fill": "currentColor" }], ["circle", { "cx": "18", "cy": "16", "r": "3", "fill": "currentColor" }]], "chevron-down": [["path", { "d": "m6 9 6 6 6-6" }]], "chevron-right": [["path", { "d": "m9 18 6-6-6-6" }]], "arrow-up": [["path", { "d": "m5 12 7-7 7 7" }], ["path", { "d": "M12 19V5" }]], "refresh": [["path", { "d": "M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" }], ["path", { "d": "M3 3v5h5" }], ["path", { "d": "M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16" }], ["path", { "d": "M16 16h5v5" }]], "film": [["rect", { "width": "18", "height": "18", "x": "3", "y": "3", "rx": "2" }], ["path", { "d": "M7 3v18" }], ["path", { "d": "M3 7.5h4" }], ["path", { "d": "M3 12h18" }], ["path", { "d": "M3 16.5h4" }], ["path", { "d": "M17 3v18" }], ["path", { "d": "M17 7.5h4" }], ["path", { "d": "M17 16.5h4" }]], "gauge": [["path", { "d": "m12 14 4-4" }], ["path", { "d": "M3.34 19a10 10 0 1 1 17.32 0" }]], "alert-circle": [["circle", { "cx": "12", "cy": "12", "r": "10" }], ["line", { "x1": "12", "x2": "12", "y1": "8", "y2": "12" }], ["line", { "x1": "12", "x2": "12.01", "y1": "16", "y2": "16" }]], "rotate-ccw": [["path", { "d": "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" }], ["path", { "d": "M3 3v5h5" }]], "alert-triangle": [["path", { "d": "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3" }], ["path", { "d": "M12 9v4" }], ["path", { "d": "M12 17h.01" }]], "arrow-down": [["path", { "d": "M12 5v14" }], ["path", { "d": "m19 12-7 7-7-7" }]], "shield": [["path", { "d": "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z" }]], "folder": [["path", { "d": "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z" }]], "check": [["path", { "d": "M20 6 9 17l-5-5" }]], "grid": [["rect", { "x": "3", "y": "3", "width": "18", "height": "18", "rx": "2" }], ["path", { "d": "M3 9h18" }], ["path", { "d": "M3 15h18" }], ["path", { "d": "M9 3v18" }], ["path", { "d": "M15 3v18" }]], "rotate-cw": [["path", { "d": "M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" }], ["path", { "d": "M21 3v5h-5" }]], "eye": [["path", { "d": "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0" }], ["circle", { "cx": "12", "cy": "12", "r": "3" }]], "palette": [["path", { "d": "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z" }], ["circle", { "cx": "13.5", "cy": "6.5", "r": ".5", "fill": "currentColor" }], ["circle", { "cx": "17.5", "cy": "10.5", "r": ".5", "fill": "currentColor" }], ["circle", { "cx": "6.5", "cy": "12.5", "r": ".5", "fill": "currentColor" }], ["circle", { "cx": "8.5", "cy": "7.5", "r": ".5", "fill": "currentColor" }]], "mail": [["rect", { "x": "2", "y": "4", "width": "20", "height": "16", "rx": "2" }], ["path", { "d": "m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7" }]], "file-spreadsheet": [["path", { "d": "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z" }], ["path", { "d": "M14 2v5a1 1 0 0 0 1 1h5" }], ["path", { "d": "M8 13h2" }], ["path", { "d": "M14 13h2" }], ["path", { "d": "M8 17h2" }], ["path", { "d": "M14 17h2" }]], "edit": [["path", { "d": "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z" }]], "file": [["path", { "d": "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z" }], ["path", { "d": "M14 2v5a1 1 0 0 0 1 1h5" }]], "external-link": [["path", { "d": "M15 3h6v6" }], ["path", { "d": "M10 14 21 3" }], ["path", { "d": "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" }]], "settings": [["path", { "d": "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915" }], ["circle", { "cx": "12", "cy": "12", "r": "3" }]], "minus": [["path", { "d": "M5 12h14" }]], "volume-2": [["path", { "d": "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z" }], ["path", { "d": "M16 9a5 5 0 0 1 0 6" }], ["path", { "d": "M19.364 18.364a9 9 0 0 0 0-12.728" }]], "star": [["path", { "d": "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z" }]], "grip-vertical": [["circle", { "cx": "9", "cy": "12", "r": "1" }], ["circle", { "cx": "9", "cy": "5", "r": "1" }], ["circle", { "cx": "9", "cy": "19", "r": "1" }], ["circle", { "cx": "15", "cy": "12", "r": "1" }], ["circle", { "cx": "15", "cy": "5", "r": "1" }], ["circle", { "cx": "15", "cy": "19", "r": "1" }]], "chevron-up": [["path", { "d": "m18 15-6-6-6 6" }]], "search": [["circle", { "cx": "11", "cy": "11", "r": "8" }], ["path", { "d": "m21 21-4.34-4.34" }]], "volume-1": [["path", { "d": "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z" }], ["path", { "d": "M16 9a5 5 0 0 1 0 6" }]], "eye-off": [["path", { "d": "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49" }], ["path", { "d": "M14.084 14.158a3 3 0 0 1-4.242-4.242" }], ["path", { "d": "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143" }], ["path", { "d": "m2 2 20 20" }]], "filter": [["polygon", { "points": "22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" }]], "circle-check": [["circle", { "cx": "12", "cy": "12", "r": "10" }], ["path", { "d": "m9 12 2 2 4-4" }]], "maximize": [["path", { "d": "M8 3H5a2 2 0 0 0-2 2v3" }], ["path", { "d": "M21 8V5a2 2 0 0 0-2-2h-3" }], ["path", { "d": "M3 16v3a2 2 0 0 0 2 2h3" }], ["path", { "d": "M16 21h3a2 2 0 0 0 2-2v-3" }]], "calendar": [["rect", { "width": "18", "height": "18", "x": "3", "y": "4", "rx": "2" }], ["path", { "d": "M16 2v4" }], ["path", { "d": "M8 2v4" }], ["path", { "d": "M3 10h18" }]], "captions": [["rect", { "width": "18", "height": "14", "x": "3", "y": "5", "rx": "2", "ry": "2" }], ["path", { "d": "M7 15h4M15 15h2M7 11h2M13 11h4" }]], "list": [["path", { "d": "M3 5h.01" }], ["path", { "d": "M3 12h.01" }], ["path", { "d": "M3 19h.01" }], ["path", { "d": "M8 5h13" }], ["path", { "d": "M8 12h13" }], ["path", { "d": "M8 19h13" }]], "signpost": [["path", { "d": "M12 13v8" }], ["path", { "d": "M12 3v3" }], ["path", { "d": "M2.354 10.354a1.207 1.207 0 0 1 0-1.708l2.06-2.06A2 2 0 0 1 5.828 6h12.344a2 2 0 0 1 1.414.586l2.06 2.06a1.207 1.207 0 0 1 0 1.708l-2.06 2.06a2 2 0 0 1-1.414.586H5.828a2 2 0 0 1-1.414-.586z" }]], "link": [["path", { "d": "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" }], ["path", { "d": "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" }]], "mic": [["path", { "d": "M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" }], ["path", { "d": "M19 10v2a7 7 0 0 1-14 0v-2" }], ["path", { "d": "M12 19v3" }]], "monitor": [["rect", { "width": "20", "height": "14", "x": "2", "y": "3", "rx": "2" }], ["line", { "x1": "8", "x2": "16", "y1": "21", "y2": "21" }], ["line", { "x1": "12", "x2": "12", "y1": "17", "y2": "21" }]], "plus": [["path", { "d": "M5 12h14" }], ["path", { "d": "M12 5v14" }]], "list-video": [["path", { "d": "M12 12H3" }], ["path", { "d": "M16 6H3" }], ["path", { "d": "M12 18H3" }], ["path", { "d": "m16 12 5 3-5 3v-6Z" }]], "bell": [["path", { "d": "M10.268 21a2 2 0 0 0 3.464 0" }], ["path", { "d": "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326" }]], "sun": [["circle", { "cx": "12", "cy": "12", "r": "4" }], ["path", { "d": "M12 2v2" }], ["path", { "d": "M12 20v2" }], ["path", { "d": "m4.93 4.93 1.41 1.41" }], ["path", { "d": "m17.66 17.66 1.41 1.41" }], ["path", { "d": "M2 12h2" }], ["path", { "d": "M20 12h2" }], ["path", { "d": "m6.34 17.66-1.41 1.41" }], ["path", { "d": "m19.07 4.93-1.41 1.41" }]], "minimize": [["path", { "d": "M8 3v3a2 2 0 0 1-2 2H3" }], ["path", { "d": "M21 8h-3a2 2 0 0 1-2-2V3" }], ["path", { "d": "M3 16h3a2 2 0 0 1 2 2v3" }], ["path", { "d": "M16 21v-3a2 2 0 0 1 2-2h3" }]], "info": [["circle", { "cx": "12", "cy": "12", "r": "10" }], ["path", { "d": "M12 16v-4" }], ["path", { "d": "M12 8h.01" }]], "code": [["path", { "d": "m16 18 6-6-6-6" }], ["path", { "d": "m8 6-6 6 6 6" }]], "scissors": [["circle", { "cx": "6", "cy": "6", "r": "3" }], ["path", { "d": "M8.12 8.12 12 12" }], ["path", { "d": "M20 4 8.12 15.88" }], ["circle", { "cx": "6", "cy": "18", "r": "3" }], ["path", { "d": "M14.8 14.8 20 20" }]], "camera": [["path", { "d": "M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z" }], ["circle", { "cx": "12", "cy": "13", "r": "3" }]], "database": [["ellipse", { "cx": "12", "cy": "5", "rx": "9", "ry": "3" }], ["path", { "d": "M3 5V19A9 3 0 0 0 21 19V5" }], ["path", { "d": "M3 12A9 3 0 0 0 21 12" }]], "repeat": [["path", { "d": "m17 2 4 4-4 4" }], ["path", { "d": "M3 11v-1a4 4 0 0 1 4-4h14" }], ["path", { "d": "m7 22-4-4 4-4" }], ["path", { "d": "M21 13v1a4 4 0 0 1-4 4H3" }]], "layers": [["path", { "d": "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z" }], ["path", { "d": "M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12" }], ["path", { "d": "M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17" }]], "image": [["rect", { "width": "18", "height": "18", "x": "3", "y": "3", "rx": "2", "ry": "2" }], ["circle", { "cx": "9", "cy": "9", "r": "2" }], ["path", { "d": "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21" }]], "picture-in-picture-2": [["path", { "d": "M21 9V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v10c0 1.1.9 2 2 2h4" }], ["rect", { "width": "10", "height": "7", "x": "12", "y": "13", "rx": "2" }]], "pause": [["rect", { "x": "14", "y": "4", "width": "4", "height": "16", "rx": "1" }], ["rect", { "x": "6", "y": "4", "width": "4", "height": "16", "rx": "1" }]], "clock": [["circle", { "cx": "12", "cy": "12", "r": "10" }], ["path", { "d": "M12 6v6l4 2" }]], "x": [["path", { "d": "M18 6 6 18" }], ["path", { "d": "m6 6 12 12" }]], "log-in": [["path", { "d": "m10 17 5-5-5-5" }], ["path", { "d": "M15 12H3" }], ["path", { "d": "M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" }]], "skip-forward": [["path", { "d": "M21 4v16" }], ["path", { "d": "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z" }]], "map-pin": [["path", { "d": "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0" }], ["circle", { "cx": "12", "cy": "10", "r": "3" }]], "play": [["path", { "d": "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z" }]], "download": [["path", { "d": "M12 15V3" }], ["path", { "d": "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" }], ["path", { "d": "m7 10 5 5 5-5" }]], "user": [["path", { "d": "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" }], ["circle", { "cx": "12", "cy": "7", "r": "4" }]], "globe": [["circle", { "cx": "12", "cy": "12", "r": "10" }], ["path", { "d": "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20" }], ["path", { "d": "M2 12h20" }]], "sliders-horizontal": [["path", { "d": "M21 4h-7" }], ["path", { "d": "M10 4H3" }], ["path", { "d": "M21 12h-9" }], ["path", { "d": "M8 12H3" }], ["path", { "d": "M21 20h-5" }], ["path", { "d": "M12 20H3" }], ["path", { "d": "M14 2v4" }], ["path", { "d": "M8 10v4" }], ["path", { "d": "M16 18v4" }]], "trash": [["path", { "d": "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" }], ["path", { "d": "M3 6h18" }], ["path", { "d": "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" }]], "menu": [["path", { "d": "M4 5h16" }], ["path", { "d": "M4 12h16" }], ["path", { "d": "M4 19h16" }]], "upload": [["path", { "d": "M12 3v12" }], ["path", { "d": "m17 8-5-5-5 5" }], ["path", { "d": "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" }]], "lock": [["rect", { "width": "18", "height": "11", "x": "3", "y": "11", "rx": "2", "ry": "2" }], ["path", { "d": "M7 11V7a5 5 0 0 1 10 0v4" }]], "users": [["path", { "d": "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" }], ["circle", { "cx": "9", "cy": "7", "r": "4" }], ["path", { "d": "M16 3.128a4 4 0 0 1 0 7.744" }], ["path", { "d": "M22 21v-2a4 4 0 0 0-3-3.87" }]], "volume-x": [["path", { "d": "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z" }], ["path", { "d": "M22 9l-6 6" }], ["path", { "d": "m16 9 6 6" }]] };
  var filled_icon_paths = { "play": [["path", { "d": "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z" }]], "pause": [["rect", { "x": "14", "y": "4", "width": "4", "height": "16", "rx": "1" }], ["rect", { "x": "6", "y": "4", "width": "4", "height": "16", "rx": "1" }]], "skip-back": [["path", { "d": "M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z" }], ["rect", { "x": "2", "y": "4", "width": "2", "height": "16", "rx": "1" }]], "skip-forward": [["rect", { "x": "20", "y": "4", "width": "2", "height": "16", "rx": "1" }], ["path", { "d": "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z" }]], "repeat": [["path", { "d": "m17 2 4 4-4 4z" }], ["path", { "d": "M3 11v-1a4 4 0 0 1 4-4h14", "fill": "none", "stroke": "currentColor", "stroke-width": "2" }], ["path", { "d": "m7 22-4-4 4-4z" }], ["path", { "d": "M21 13v1a4 4 0 0 1-4 4H3", "fill": "none", "stroke": "currentColor", "stroke-width": "2" }]], "volume-2": [["path", { "d": "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z" }], ["path", { "d": "M16 9a5 5 0 0 1 0 6", "fill": "none", "stroke": "currentColor", "stroke-width": "2" }], ["path", { "d": "M19.364 18.364a9 9 0 0 0 0-12.728", "fill": "none", "stroke": "currentColor", "stroke-width": "2" }]] };
  var icon_names = set(keys(icon_paths));
  var default_size = "md";
  var icon_class_list = function(p__506) {
    const map__12 = p__506;
    const size3 = get(map__12, "size");
    const s4 = (() => {
      const or__23426__auto__5 = (() => {
        const G__5076 = size3;
        if (G__5076 == null) {
          return null;
        } else {
          return kw_name(G__5076);
        }
        ;
      })();
      if (truth_(or__23426__auto__5)) {
        return or__23426__auto__5;
      } else {
        return default_size;
      }
      ;
    })();
    const G__5087 = ["icon"];
    if (!(s4 === "md")) {
      return conj(G__5087, `icon-${s4 ?? ""}`);
    } else {
      return G__5087;
    }
    ;
  };
  var icon_classes = function(opts) {
    return join(" ", icon_class_list(opts));
  };
  var icon = function(p__509) {
    const map__12 = p__509;
    const _props3 = map__12;
    const icon_name4 = get(map__12, "icon-name");
    const size5 = get(map__12, "size");
    const class$6 = get(map__12, "class");
    const attrs7 = get(map__12, "attrs");
    const filled8 = get(map__12, "filled");
    const n9 = kw_name(icon_name4);
    const kw10 = n9;
    const paths11 = truth_(filled8) ? (() => {
      const or__23426__auto__12 = get(filled_icon_paths, kw10);
      if (truth_(or__23426__auto__12)) {
        return or__23426__auto__12;
      } else {
        return get(icon_paths, kw10);
      }
      ;
    })() : get(icon_paths, kw10);
    if (truth_(paths11)) {
      const classes13 = (() => {
        const G__51014 = icon_classes({ "size": size5 });
        if (truth_(class$6)) {
          return `${G__51014 ?? ""}${" "}${class$6 ?? ""}`;
        } else {
          return G__51014;
        }
        ;
      })();
      const svg_attrs15 = merge({ "stroke": truth_(filled8) ? "none" : "currentColor", "aria-hidden": "true", "fill": truth_(filled8) ? "currentColor" : "none", "stroke-linejoin": "round", "xmlns": "http://www.w3.org/2000/svg", "stroke-linecap": "round", "stroke-width": "2", "class": classes13, "viewBox": "0 0 24 24" }, attrs7);
      return into(["svg", svg_attrs15], paths11);
    }
    ;
  };

  // .compiled/ui/spinner.mjs
  var default_size2 = "md";
  var spinner_class_list = function(p__889) {
    const map__12 = p__889;
    const size3 = get(map__12, "size");
    const s4 = (() => {
      const or__23426__auto__5 = (() => {
        const G__8906 = size3;
        if (G__8906 == null) {
          return null;
        } else {
          return kw_name(G__8906);
        }
        ;
      })();
      if (truth_(or__23426__auto__5)) {
        return or__23426__auto__5;
      } else {
        return default_size2;
      }
      ;
    })();
    const G__8917 = ["spinner"];
    const G__8918 = s4 === "xs" ? conj(G__8917, "spinner-xs") : G__8917;
    const G__8919 = s4 === "sm" ? conj(G__8918, "spinner-sm") : G__8918;
    if (s4 === "lg") {
      return conj(G__8919, "spinner-lg");
    } else {
      return G__8919;
    }
    ;
  };
  var spinner_classes = function(opts) {
    return join(" ", spinner_class_list(opts));
  };
  var spinner = function(p__892) {
    const map__12 = p__892;
    const _props3 = map__12;
    const size4 = get(map__12, "size");
    const class$5 = get(map__12, "class");
    const attrs6 = get(map__12, "attrs");
    const classes7 = (() => {
      const G__8938 = spinner_classes({ "size": size4 });
      if (truth_(class$5)) {
        return `${G__8938 ?? ""}${" "}${class$5 ?? ""}`;
      } else {
        return G__8938;
      }
      ;
    })();
    const base_attrs9 = merge({ "class": classes7, "role": "status", "aria-label": "Loading" }, attrs6);
    return ["span", base_attrs9];
  };

  // .compiled/ui/button.mjs
  var default_variant = "secondary";
  var default_size3 = "md";
  var button_class_list = function(p__99) {
    const map__12 = p__99;
    const variant3 = get(map__12, "variant");
    const size4 = get(map__12, "size");
    const loud5 = get(map__12, "loud");
    const icon6 = get(map__12, "icon");
    const round7 = get(map__12, "round");
    const loading8 = get(map__12, "loading");
    const v9 = (() => {
      const or__23426__auto__10 = (() => {
        const G__10011 = variant3;
        if (G__10011 == null) {
          return null;
        } else {
          return kw_name(G__10011);
        }
        ;
      })();
      if (truth_(or__23426__auto__10)) {
        return or__23426__auto__10;
      } else {
        return default_variant;
      }
      ;
    })();
    const s12 = (() => {
      const or__23426__auto__13 = (() => {
        const G__10114 = size4;
        if (G__10114 == null) {
          return null;
        } else {
          return kw_name(G__10114);
        }
        ;
      })();
      if (truth_(or__23426__auto__13)) {
        return or__23426__auto__13;
      } else {
        return default_size3;
      }
      ;
    })();
    const G__10215 = ["btn", `btn-${v9 ?? ""}`];
    const G__10216 = truth_(loud5) ? conj(G__10215, "btn-loud") : G__10215;
    const G__10217 = !(s12 === "md") ? conj(G__10216, `btn-${s12 ?? ""}`) : G__10216;
    const G__10218 = truth_(icon6) ? conj(G__10217, "btn-icon") : G__10217;
    const G__10219 = truth_((() => {
      const and__23442__auto__20 = icon6;
      if (truth_(and__23442__auto__20)) {
        return round7;
      } else {
        return and__23442__auto__20;
      }
      ;
    })()) ? conj(G__10218, "btn-icon-round") : G__10218;
    if (truth_(loading8)) {
      return conj(G__10219, "btn-loading");
    } else {
      return G__10219;
    }
    ;
  };
  var button_classes = function(opts) {
    return join(" ", button_class_list(opts));
  };
  var button = (() => {
    const f103 = (function(var_args) {
      const args1041 = [];
      const len__23321__auto__2 = arguments.length;
      let i1053 = 0;
      while (true) {
        if (i1053 < len__23321__auto__2) {
          args1041.push(arguments[i1053]);
          let G__4 = i1053 + 1;
          i1053 = G__4;
          continue;
        }
        ;
        break;
      }
      ;
      const argseq__23513__auto__5 = 1 < args1041.length ? args1041.slice(1) : null;
      return f103.cljs$core$IFn$_invoke$arity$variadic(arguments[0], argseq__23513__auto__5);
    });
    f103.cljs$core$IFn$_invoke$arity$variadic = (function(p__107, children) {
      const map__67 = p__107;
      const _props8 = map__67;
      const href9 = get(map__67, "href");
      const icon_left10 = get(map__67, "icon-left");
      const variant11 = get(map__67, "variant");
      const disabled12 = get(map__67, "disabled");
      const icon13 = get(map__67, "icon");
      const on_click14 = get(map__67, "on-click");
      const size15 = get(map__67, "size");
      const round16 = get(map__67, "round");
      const loud17 = get(map__67, "loud");
      const icon_right18 = get(map__67, "icon-right");
      const loading19 = get(map__67, "loading");
      const class$20 = get(map__67, "class");
      const attrs21 = get(map__67, "attrs");
      const icon_size22 = (() => {
        const G__10823 = kw_name((() => {
          const or__23426__auto__24 = size15;
          if (truth_(or__23426__auto__24)) {
            return or__23426__auto__24;
          } else {
            return default_size3;
          }
          ;
        })());
        switch (G__10823) {
          case "sm":
            return "sm";
            break;
          case "lg":
            return "md";
            break;
          default:
            return "sm";
        }
        ;
      })();
      const disabled26 = (() => {
        const or__23426__auto__27 = disabled12;
        if (truth_(or__23426__auto__27)) {
          return or__23426__auto__27;
        } else {
          return loading19;
        }
        ;
      })();
      const spinner_el28 = truth_(loading19) ? spinner({ "size": "sm" === kw_name((() => {
        const or__23426__auto__29 = size15;
        if (truth_(or__23426__auto__29)) {
          return or__23426__auto__29;
        } else {
          return default_size3;
        }
        ;
      })()) ? "xs" : "sm" }) : null;
      const tag30 = truth_(href9) ? "a" : "button";
      const classes31 = (() => {
        const G__10932 = button_classes({ "variant": variant11, "size": size15, "loud": loud17, "icon": icon13, "round": round16, "loading": loading19 });
        if (truth_(class$20)) {
          return `${G__10932 ?? ""}${" "}${class$20 ?? ""}`;
        } else {
          return G__10932;
        }
        ;
      })();
      const base_attrs33 = (() => {
        const G__11034 = merge({ "class": classes31 }, attrs21);
        const G__11035 = truth_(href9) ? assoc(G__11034, "href", href9) : G__11034;
        if (truth_(disabled26)) {
          return assoc(G__11035, "disabled", true);
        } else {
          return G__11035;
        }
        ;
      })();
      const base_attrs36 = (() => {
        const G__11137 = base_attrs33;
        if (truth_(on_click14)) {
          return assoc(G__11137, "on-click", on_click14);
        } else {
          return G__11137;
        }
        ;
      })();
      if (truth_(icon13)) {
        return [tag30, base_attrs36, truth_(loading19) ? spinner_el28 : icon({ "icon-name": icon13, "size": icon_size22 })];
      } else {
        return into([tag30, base_attrs36], (() => {
          const G__11238 = [];
          const G__11239 = truth_(loading19) ? conj(G__11238, spinner_el28) : G__11238;
          const G__11240 = truth_((() => {
            const and__23442__auto__41 = icon_left10;
            if (truth_(and__23442__auto__41)) {
              return not(loading19);
            } else {
              return and__23442__auto__41;
            }
            ;
          })()) ? conj(G__11239, icon({ "icon-name": icon_left10, "size": icon_size22 })) : G__11239;
          const G__11242 = true ? into(G__11240, children) : G__11240;
          if (truth_(icon_right18)) {
            return conj(G__11242, icon({ "icon-name": icon_right18, "size": icon_size22 }));
          } else {
            return G__11242;
          }
          ;
        })());
      }
      ;
    });
    f103.cljs$lang$maxFixedArity = 1;
    return f103;
  })();

  // .compiled/ui/notification.mjs
  var default_duration2 = 4e3;
  var remove_delay = 200;
  var default_visible_toasts = 3;
  var default_gap = 14;
  var default_limit = 50;
  var default_position = "bottom-right";
  var default_hotkey = ["altKey", "KeyT"];
  var infinity = Infinity;
  var kw = function(x) {
    const G__6071 = x;
    if (G__6071 == null) {
      return null;
    } else {
      return kw_name(G__6071);
    }
    ;
  };
  var flag = function(x) {
    if (truth_(x)) {
      return "true";
    } else {
      return "false";
    }
    ;
  };
  var __GT_kw = function(s) {
    return s;
  };
  var classes = (() => {
    const f608 = (function(var_args) {
      const args6091 = [];
      const len__23321__auto__2 = arguments.length;
      let i6103 = 0;
      while (true) {
        if (i6103 < len__23321__auto__2) {
          args6091.push(arguments[i6103]);
          let G__4 = i6103 + 1;
          i6103 = G__4;
          continue;
        }
        ;
        break;
      }
      ;
      const argseq__23513__auto__5 = 0 < args6091.length ? args6091.slice(0) : null;
      return f608.cljs$core$IFn$_invoke$arity$variadic(argseq__23513__auto__5);
    });
    f608.cljs$core$IFn$_invoke$arity$variadic = (function(tokens) {
      const ts6 = remove(nil_QMARK_, tokens);
      return join(" ", ts6);
    });
    f608.cljs$lang$maxFixedArity = 0;
    return f608;
  })();
  var type_icons = { "success": "check", "info": "info", "warning": "alert-triangle", "error": "x" };
  var notification_class_list = function(p__612) {
    const map__12 = p__612;
    const type3 = get(map__12, "type");
    const multiline4 = get(map__12, "multiline");
    const G__6135 = ["notification", `notification-${(() => {
      const or__23426__auto__6 = kw(type3);
      if (truth_(or__23426__auto__6)) {
        return or__23426__auto__6;
      } else {
        return "default";
      }
      ;
    })() ?? ""}`];
    if (truth_(multiline4)) {
      return conj(G__6135, "notification-multiline");
    } else {
      return G__6135;
    }
    ;
  };
  var loader = function(visible_QMARK_) {
    return into(["span", { "class": "notification-loader", "data-visible": flag(visible_QMARK_), "aria-hidden": "true" }], repeat(12, ["span", { "class": "notification-loader-bar" }]));
  };
  var glyph = function(icon_or_hiccup) {
    if (truth_(vector_QMARK_(icon_or_hiccup))) {
      return icon_or_hiccup;
    } else {
      return icon({ "icon-name": icon_or_hiccup, "size": "sm" });
    }
    ;
  };
  var icon_el = function(p__614, icons, class_name) {
    const map__12 = p__614;
    const type3 = get(map__12, "type");
    const icon4 = get(map__12, "icon");
    const promise5 = get(map__12, "promise");
    if (truth_(false_QMARK_(icon4))) {
      return null;
    } else {
      const t6 = (() => {
        const or__23426__auto__7 = kw(type3);
        if (truth_(or__23426__auto__7)) {
          return or__23426__auto__7;
        } else {
          return "default";
        }
        ;
      })();
      const loading_QMARK_8 = t6 === "loading";
      const custom9 = get(icons, __GT_kw(t6));
      const shown10 = !(icon4 == null) ? glyph(icon4) : !(custom9 == null) ? glyph(custom9) : truth_(get(type_icons, t6)) ? glyph(get(type_icons, t6)) : null;
      if (truth_((() => {
        const or__23426__auto__11 = loading_QMARK_8;
        if (or__23426__auto__11) {
          return or__23426__auto__11;
        } else {
          const or__23426__auto__12 = promise5;
          if (truth_(or__23426__auto__12)) {
            return or__23426__auto__12;
          } else {
            return shown10;
          }
          ;
        }
        ;
      })())) {
        const G__61513 = ["span", { "class": classes("notification-icon", class_name), "data-icon": "" }];
        const G__61514 = truth_((() => {
          const or__23426__auto__15 = loading_QMARK_8;
          if (or__23426__auto__15) {
            return or__23426__auto__15;
          } else {
            return promise5;
          }
          ;
        })()) ? conj(G__61513, loader(loading_QMARK_8)) : G__61513;
        if (truth_((() => {
          const and__23442__auto__16 = shown10;
          if (truth_(and__23442__auto__16)) {
            return !loading_QMARK_8;
          } else {
            return and__23442__auto__16;
          }
          ;
        })())) {
          return conj(G__61514, ["span", { "class": "notification-glyph" }, shown10]);
        } else {
          return G__61514;
        }
        ;
      }
      ;
    }
    ;
  };
  var button_el = function(kind, spec, class_name) {
    if (truth_(vector_QMARK_(spec))) {
      return spec;
    } else {
      const map__12 = spec;
      const label3 = get(map__12, "label");
      const on_click42 = get(map__12, "on-click");
      const attrs5 = get(map__12, "attrs");
      return button((() => {
        const G__6166 = { "size": "sm", "variant": kind === "action" ? "secondary" : "ghost", "class": class_name, "attrs": merge((() => {
          const G__6177 = {};
          G__6177["data-button"] = "";
          G__6177[__GT_kw(`data-${kind ?? ""}`)] = "";
          return G__6177;
        })(), attrs5) };
        if (truth_(on_click42)) {
          return { ...G__6166, "on-click": on_click42 };
        } else {
          return G__6166;
        }
        ;
      })(), label3);
    }
    ;
  };
  var close_el = function(label, disabled_QMARK_3, class_name) {
    return ["button", { "class": classes("notification-close", class_name), "type": "button", "aria-label": (() => {
      const or__23426__auto__1 = label;
      if (truth_(or__23426__auto__1)) {
        return or__23426__auto__1;
      } else {
        return "Close toast";
      }
      ;
    })(), "data-close-button": "", "data-disabled": flag(disabled_QMARK_3) }, icon({ "icon-name": "x", "size": "sm" })];
  };
  var card_children = function(p__618, icons, class_names, extra) {
    const map__12 = p__618;
    const item3 = map__12;
    const close_label4 = get(map__12, "close-label");
    const description5 = get(map__12, "description");
    const meta6 = get(map__12, "meta");
    const close_button7 = get(map__12, "close-button");
    const dismissible8 = get(map__12, "dismissible");
    const type9 = get(map__12, "type");
    const title10 = get(map__12, "title");
    const cancel11 = get(map__12, "cancel");
    const action12 = get(map__12, "action");
    const cn13 = (() => {
      const or__23426__auto__14 = class_names;
      if (truth_(or__23426__auto__14)) {
        return or__23426__auto__14;
      } else {
        return {};
      }
      ;
    })();
    const loading_QMARK_15 = "loading" === kw(type9);
    const G__61916 = [];
    const G__61917 = truth_((() => {
      const and__23442__auto__18 = close_button7;
      if (truth_(and__23442__auto__18)) {
        return !loading_QMARK_15;
      } else {
        return and__23442__auto__18;
      }
      ;
    })()) ? conj(G__61916, close_el(close_label4, false_QMARK_(dismissible8), get(cn13, "close-button"))) : G__61916;
    const G__61919 = true ? conj(G__61917, icon_el(item3, icons, get(cn13, "icon"))) : G__61917;
    const G__61920 = true ? conj(G__61919, (() => {
      const G__62021 = ["div", { "class": classes("notification-text", get(cn13, "content")), "data-content": "" }, ["p", { "class": classes("notification-title", get(cn13, "title")), "data-title": "" }, title10]];
      const G__62022 = truth_(description5) ? conj(G__62021, ["div", { "class": classes("notification-description", get(cn13, "description")), "data-description": "" }, description5]) : G__62021;
      if (truth_(meta6)) {
        return conj(G__62022, ["div", { "class": "notification-meta" }, meta6]);
      } else {
        return G__62022;
      }
      ;
    })()) : G__61919;
    if (truth_((() => {
      const or__23426__auto__23 = cancel11;
      if (truth_(or__23426__auto__23)) {
        return or__23426__auto__23;
      } else {
        const or__23426__auto__24 = action12;
        if (truth_(or__23426__auto__24)) {
          return or__23426__auto__24;
        } else {
          return seq(extra);
        }
        ;
      }
      ;
    })())) {
      return conj(G__61920, (() => {
        const G__62125 = ["div", { "class": "notification-actions" }];
        const G__62126 = truth_(cancel11) ? conj(G__62125, button_el("cancel", cancel11, get(cn13, "cancel-button"))) : G__62125;
        const G__62127 = truth_(action12) ? conj(G__62126, button_el("action", action12, get(cn13, "action-button"))) : G__62126;
        if (truth_(seq(extra))) {
          return into(G__62127, extra);
        } else {
          return G__62127;
        }
        ;
      })());
    } else {
      return G__61920;
    }
    ;
  };
  var notification = (() => {
    const f622 = (function(var_args) {
      const args6231 = [];
      const len__23321__auto__2 = arguments.length;
      let i6243 = 0;
      while (true) {
        if (i6243 < len__23321__auto__2) {
          args6231.push(arguments[i6243]);
          let G__4 = i6243 + 1;
          i6243 = G__4;
          continue;
        }
        ;
        break;
      }
      ;
      const argseq__23513__auto__5 = 1 < args6231.length ? args6231.slice(1) : null;
      return f622.cljs$core$IFn$_invoke$arity$variadic(arguments[0], argseq__23513__auto__5);
    });
    f622.cljs$core$IFn$_invoke$arity$variadic = (function(p__626, children) {
      const map__67 = p__626;
      const item8 = map__67;
      const class_names9 = get(map__67, "class-names");
      const invert10 = get(map__67, "invert");
      const close_label11 = get(map__67, "close-label");
      const description12 = get(map__67, "description");
      const unstyled13 = get(map__67, "unstyled");
      const meta14 = get(map__67, "meta");
      const close_button15 = get(map__67, "close-button");
      const dismissible16 = get(map__67, "dismissible");
      const type17 = get(map__67, "type");
      const icon18 = get(map__67, "icon");
      const rich_colors19 = get(map__67, "rich-colors");
      const title20 = get(map__67, "title");
      const class$21 = get(map__67, "class");
      const attrs22 = get(map__67, "attrs");
      const cancel23 = get(map__67, "cancel");
      const action24 = get(map__67, "action");
      return into(["div", class_attrs(notification_class_list({ "type": type17, "multiline": boolean$((() => {
        const or__23426__auto__25 = description12;
        if (truth_(or__23426__auto__25)) {
          return or__23426__auto__25;
        } else {
          return meta14;
        }
        ;
      })()) }), class$21, merge({ "data-type": (() => {
        const or__23426__auto__26 = kw(type17);
        if (truth_(or__23426__auto__26)) {
          return or__23426__auto__26;
        } else {
          return "default";
        }
        ;
      })(), "data-styled": flag(not(unstyled13)), "data-rich-colors": flag(rich_colors19), "data-invert": flag(invert10) }, attrs22))], card_children(item8, null, class_names9, remove(nil_QMARK_, children)));
    });
    f622.cljs$lang$maxFixedArity = 1;
    return f622;
  })();
  var position_parts = function(position) {
    return split((() => {
      const or__23426__auto__1 = kw(position);
      if (truth_(or__23426__auto__1)) {
        return or__23426__auto__1;
      } else {
        return kw(default_position);
      }
      ;
    })(), /-/);
  };
  var default_swipe_directions = function(position) {
    const vec__14 = position_parts(position);
    const y5 = nth(vec__14, 0, null);
    const x6 = nth(vec__14, 1, null);
    const G__6277 = [y5];
    if (!(x6 === "center")) {
      return conj(G__6277, x6);
    } else {
      return G__6277;
    }
    ;
  };
  var hotkey_label = function(hotkey) {
    return join("+", map((function(k) {
      if (truth_(starts_with_QMARK_(k, "Key"))) {
        return subs(k, 3);
      } else {
        if (truth_(starts_with_QMARK_(k, "Digit"))) {
          return subs(k, 5);
        } else {
          if (truth_(ends_with_QMARK_(k, "Key"))) {
            return subs(k, 0, count(k) - 3);
          } else {
            if ("else") {
              return k;
            } else {
              return null;
            }
          }
        }
      }
      ;
    }), hotkey));
  };
  var css_length = function(v) {
    if (truth_(number_QMARK_(v))) {
      return `${v ?? ""}px`;
    } else {
      return `${v ?? ""}`;
    }
    ;
  };
  var offset_attrs = function(prefix, offset) {
    if (offset == null) {
      return {};
    } else {
      if (truth_(map_QMARK_(offset))) {
        return into({}, keep((function(side) {
          const temp__23062__auto__1 = get(offset, __GT_kw(side));
          if (truth_(temp__23062__auto__1)) {
            const v2 = temp__23062__auto__1;
            return [__GT_kw(`data-${prefix ?? ""}-${side ?? ""}`), css_length(v2)];
          }
          ;
        })), ["top", "right", "bottom", "left"]);
      } else {
        if ("else") {
          return into({}, map((function(side) {
            return [__GT_kw(`data-${prefix ?? ""}-${side ?? ""}`), css_length(offset)];
          })), ["top", "right", "bottom", "left"]);
        } else {
          return null;
        }
      }
    }
    ;
  };
  var toast_el = function(p__628, p__629) {
    const map__13 = p__628;
    const item4 = map__13;
    const class_names5 = get(map__13, "class-names");
    const invert6 = get(map__13, "invert");
    const description7 = get(map__13, "description");
    const unstyled8 = get(map__13, "unstyled");
    const meta9 = get(map__13, "meta");
    const promise10 = get(map__13, "promise");
    const dismissible11 = get(map__13, "dismissible");
    const type12 = get(map__13, "type");
    const rich_colors13 = get(map__13, "rich-colors");
    const id14 = get(map__13, "id");
    const class$15 = get(map__13, "class");
    const removed_at16 = get(map__13, "removed-at");
    const attrs17 = get(map__13, "attrs");
    const map__218 = p__629;
    const x19 = get(map__218, "x");
    const y20 = get(map__218, "y");
    const index21 = get(map__218, "index");
    const close_button22 = get(map__218, "close-button");
    const rich_colors_default23 = get(map__218, "rich-colors-default");
    const icons24 = get(map__218, "icons");
    const invert_default25 = get(map__218, "invert-default");
    const total26 = get(map__218, "total");
    const visible_toasts27 = get(map__218, "visible-toasts");
    const t28 = (() => {
      const or__23426__auto__29 = kw(type12);
      if (truth_(or__23426__auto__29)) {
        return or__23426__auto__29;
      } else {
        return "default";
      }
      ;
    })();
    const item30 = (() => {
      const G__63031 = item4;
      if (get(item4, "close-button") == null) {
        return assoc(G__63031, "close-button", close_button22);
      } else {
        return G__63031;
      }
      ;
    })();
    return into(["li", class_attrs(notification_class_list({ "type": type12, "multiline": boolean$((() => {
      const or__23426__auto__32 = description7;
      if (truth_(or__23426__auto__32)) {
        return or__23426__auto__32;
      } else {
        return meta9;
      }
      ;
    })()) }), class$15, merge({ "data-dismissible": flag(not(false_QMARK_(dismissible11))), "data-styled": flag(not(unstyled8)), "data-index": `${index21 ?? ""}`, "data-rich-colors": flag(!(rich_colors13 == null) ? rich_colors13 : rich_colors_default23), "data-type": t28, "tabindex": "0", "data-count": `${total26 ?? ""}`, "data-front": flag(index21 === 0), "data-id": `${id14 ?? ""}`, "data-removed": flag(removed_at16), "data-y-position": y20, "data-visible": flag(index21 < visible_toasts27), "data-invert": flag((() => {
      const or__23426__auto__33 = invert6;
      if (truth_(or__23426__auto__33)) {
        return or__23426__auto__33;
      } else {
        return invert_default25;
      }
      ;
    })()), "data-x-position": x19, "data-ui-notification": "", "data-promise": flag(promise10) }, {}, attrs17))], card_children(item30, icons24, class_names5, null));
  };
  var event_attrs = function(on_dismiss, on_pause) {
    const G__6311 = {};
    const G__6312 = truth_(on_dismiss) ? { ...G__6311, "on-notificationdismiss": (function(e) {
      return on_dismiss(e.detail.id, e.detail.reason);
    }) } : G__6311;
    if (truth_(on_pause)) {
      return assoc(G__6312, "on-notificationpause", (function(e) {
        return on_pause(e.detail.paused);
      }));
    } else {
      return G__6312;
    }
    ;
  };
  var notification_toaster = function(p__632) {
    const map__12 = p__632;
    const items3 = get(map__12, "items");
    const on_dismiss4 = get(map__12, "on-dismiss");
    const on_pause5 = get(map__12, "on-pause");
    const expand6 = get(map__12, "expand");
    const invert7 = get(map__12, "invert");
    const swipe_directions8 = get(map__12, "swipe-directions");
    const mobile_offset9 = get(map__12, "mobile-offset");
    const dir10 = get(map__12, "dir");
    const offset11 = get(map__12, "offset");
    const close_button12 = get(map__12, "close-button");
    const rich_colors13 = get(map__12, "rich-colors");
    const icons14 = get(map__12, "icons");
    const contained15 = get(map__12, "contained");
    const hotkey16 = get(map__12, "hotkey");
    const label17 = get(map__12, "label");
    const class$18 = get(map__12, "class");
    const visible_toasts19 = get(map__12, "visible-toasts");
    const gap20 = get(map__12, "gap");
    const attrs21 = get(map__12, "attrs");
    const position22 = get(map__12, "position");
    const pos23 = (() => {
      const or__23426__auto__24 = kw(position22);
      if (truth_(or__23426__auto__24)) {
        return or__23426__auto__24;
      } else {
        return kw(default_position);
      }
      ;
    })();
    const hotkey25 = (() => {
      const or__23426__auto__26 = hotkey16;
      if (truth_(or__23426__auto__26)) {
        return or__23426__auto__26;
      } else {
        return default_hotkey;
      }
      ;
    })();
    const visible_n27 = (() => {
      const or__23426__auto__28 = visible_toasts19;
      if (truth_(or__23426__auto__28)) {
        return or__23426__auto__28;
      } else {
        return default_visible_toasts;
      }
      ;
    })();
    const positions29 = distinct(cons(pos23, keep((function(i) {
      return kw(get(i, "position"));
    }), items3)));
    return into(["section", class_attrs((() => {
      const G__63330 = ["notification-toaster"];
      if (truth_(contained15)) {
        return conj(G__63330, "notification-toaster-contained");
      } else {
        return G__63330;
      }
      ;
    })(), class$18, merge({ "data-ui-notification-toaster": "", "tabindex": "-1", "aria-label": `${(() => {
      const or__23426__auto__31 = label17;
      if (truth_(or__23426__auto__31)) {
        return or__23426__auto__31;
      } else {
        return "Notifications";
      }
      ;
    })() ?? ""}${" "}${hotkey_label(hotkey25) ?? ""}`, "aria-live": "polite", "aria-relevant": "additions text", "aria-atomic": "false", "data-hotkey": join("+", hotkey25) }, event_attrs(on_dismiss4, on_pause5), attrs21))], truth_(seq(items3)) ? lazy((function* () {
      for (let G__32 of iterable(positions29)) {
        const p33 = G__32;
        const vec__3437 = position_parts(p33);
        const y38 = nth(vec__3437, 0, null);
        const x39 = nth(vec__3437, 1, null);
        const here40 = filterv((function(i) {
          return _EQ_(p33, (() => {
            const or__23426__auto__41 = kw(get(i, "position"));
            if (truth_(or__23426__auto__41)) {
              return or__23426__auto__41;
            } else {
              return pos23;
            }
            ;
          })());
        }), items3);
        const n42 = count(here40);
        const ctx43 = { "total": n42, "visible-toasts": visible_n27, "y": y38, "x": x39, "close-button": close_button12, "rich-colors-default": rich_colors13, "invert-default": invert7, "icons": icons14 };
        yield into(["ol", merge({ "class": "notification-list", "data-ui-notification-list": "", "data-y-position": y38, "data-x-position": x39, "tabindex": "-1", "data-expand": flag(expand6), "data-gap": `${(yield* (function* () {
          const or__23426__auto__44 = gap20;
          if (truth_(or__23426__auto__44)) {
            return or__23426__auto__44;
          } else {
            return default_gap;
          }
          ;
        })()) ?? ""}`, "data-swipe-directions": join(" ", yield* (function* () {
          const or__23426__auto__45 = swipe_directions8;
          if (truth_(or__23426__auto__45)) {
            return or__23426__auto__45;
          } else {
            return default_swipe_directions(p33);
          }
          ;
        })()) }, truth_(dir10) ? { "dir": dir10 } : null, offset_attrs("offset", offset11), offset_attrs("mobile-offset", mobile_offset9), {})], map_indexed((function(i, item) {
          return toast_el(item, { ...ctx43, "index": i });
        }), here40));
      }
      return null;
    })) : null);
  };
  var queue = function() {
    return { "items": [], "muted": /* @__PURE__ */ new Set([]), "paused-at": null, "seq": 0 };
  };
  var lifetime = (() => {
    const f634 = (function(...args635) {
      const G__6361 = args635.length;
      switch (G__6361) {
        case 1:
          return f634.cljs$core$IFn$_invoke$arity$1(args635[0]);
          break;
        case 2:
          return f634.cljs$core$IFn$_invoke$arity$2(args635[0], args635[1]);
          break;
        default:
          throw new Error(`${"Invalid arity: "}${args635.length ?? ""}`);
      }
      ;
    });
    f634.cljs$core$IFn$_invoke$arity$1 = (function(item) {
      return lifetime(item, default_duration2);
    });
    f634.cljs$core$IFn$_invoke$arity$2 = (function(p__637, default$) {
      const map__34 = p__637;
      const type5 = get(map__34, "type");
      const duration6 = get(map__34, "duration");
      if ("loading" === kw(type5)) {
        return null;
      } else {
        if (truth_((() => {
          const and__23442__auto__7 = number_QMARK_(duration6);
          if (truth_(and__23442__auto__7)) {
            return _EQ_(duration6, infinity);
          } else {
            return and__23442__auto__7;
          }
          ;
        })())) {
          return null;
        } else {
          if (truth_((() => {
            const and__23442__auto__8 = number_QMARK_(duration6);
            if (truth_(and__23442__auto__8)) {
              return duration6 > 0;
            } else {
              return and__23442__auto__8;
            }
            ;
          })())) {
            return duration6;
          } else {
            if ("else") {
              return default$;
            } else {
              return null;
            }
          }
        }
      }
      ;
    });
    f634.cljs$lang$maxFixedArity = 2;
    return f634;
  })();
  var stamp = function(item, now3, paused_at, default$) {
    const ms1 = lifetime(item, default$);
    return assoc(dissoc(item, "removed-at", "closed"), "expires-at", truth_(ms1) ? (() => {
      const or__23426__auto__2 = paused_at;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return now3;
      }
      ;
    })() + ms1 : null);
  };
  var close2 = function(item, now3, reason) {
    if (truth_(get(item, "removed-at"))) {
      return item;
    } else {
      return assoc(item, "removed-at", now3, "closed", reason);
    }
    ;
  };
  var post = (() => {
    const f638 = (function(...args639) {
      const G__6401 = args639.length;
      switch (G__6401) {
        case 3:
          return f638.cljs$core$IFn$_invoke$arity$3(args639[0], args639[1], args639[2]);
          break;
        case 4:
          return f638.cljs$core$IFn$_invoke$arity$4(args639[0], args639[1], args639[2], args639[3]);
          break;
        default:
          throw new Error(`${"Invalid arity: "}${args639.length ?? ""}`);
      }
      ;
    });
    f638.cljs$core$IFn$_invoke$arity$3 = (function(state3, item, now3) {
      return post(state3, item, now3, null);
    });
    f638.cljs$core$IFn$_invoke$arity$4 = (function(state3, item, now3, p__641) {
      const map__34 = p__641;
      const duration5 = get(map__34, "duration");
      const limit6 = get(map__34, "limit");
      const map__78 = state3;
      const items9 = get(map__78, "items");
      const paused_at10 = get(map__78, "paused-at");
      const dur11 = (() => {
        const or__23426__auto__12 = duration5;
        if (truth_(or__23426__auto__12)) {
          return or__23426__auto__12;
        } else {
          return default_duration2;
        }
        ;
      })();
      const id13 = get(item, "id");
      const old14 = !(id13 == null) ? some((function(i) {
        if (_EQ_(id13, get(i, "id"))) {
          return i;
        }
        ;
      }), items9) : null;
      const fresh15 = (function(i) {
        return stamp(i, now3, paused_at10, dur11);
      });
      if (truth_((() => {
        const and__23442__auto__16 = old14;
        if (truth_(and__23442__auto__16)) {
          return get(old14, "removed-at");
        } else {
          return and__23442__auto__16;
        }
        ;
      })())) {
        return assoc(state3, "items", mapv((function(i) {
          if (_EQ_(id13, get(i, "id"))) {
            return fresh15(item);
          } else {
            return i;
          }
          ;
        }), items9));
      } else {
        if (truth_(old14)) {
          return assoc(state3, "items", mapv((function(i) {
            if (_EQ_(id13, get(i, "id"))) {
              return fresh15((() => {
                const G__64217 = merge(i, item);
                if ("loading" === kw(get(i, "type"))) {
                  return assoc(G__64217, "promise", true);
                } else {
                  return G__64217;
                }
                ;
              })());
            } else {
              return i;
            }
            ;
          }), items9));
        } else {
          if ("else") {
            const n18 = (() => {
              const or__23426__auto__19 = get(state3, "seq");
              if (truth_(or__23426__auto__19)) {
                return or__23426__auto__19;
              } else {
                return 0;
              }
              ;
            })() + 1;
            const item20 = fresh15((() => {
              const G__64321 = item;
              if (id13 == null) {
                return assoc(G__64321, "id", `notification-${n18 ?? ""}`);
              } else {
                return G__64321;
              }
              ;
            })());
            return assoc(state3, "seq", n18, "items", vec(take((() => {
              const or__23426__auto__22 = limit6;
              if (truth_(or__23426__auto__22)) {
                return or__23426__auto__22;
              } else {
                return default_limit;
              }
              ;
            })(), cons(item20, items9))));
          } else {
            return null;
          }
        }
      }
      ;
    });
    f638.cljs$lang$maxFixedArity = 4;
    return f638;
  })();
  var dismiss = (() => {
    const f644 = (function(...args645) {
      const G__6461 = args645.length;
      switch (G__6461) {
        case 3:
          return f644.cljs$core$IFn$_invoke$arity$3(args645[0], args645[1], args645[2]);
          break;
        case 4:
          return f644.cljs$core$IFn$_invoke$arity$4(args645[0], args645[1], args645[2], args645[3]);
          break;
        default:
          throw new Error(`${"Invalid arity: "}${args645.length ?? ""}`);
      }
      ;
    });
    f644.cljs$core$IFn$_invoke$arity$3 = (function(state3, id, now3) {
      return dismiss(state3, id, now3, "dismiss");
    });
    f644.cljs$core$IFn$_invoke$arity$4 = (function(state3, id, now3, reason) {
      return assoc(state3, "items", mapv((function(i) {
        if (_EQ_(id, get(i, "id"))) {
          return close2(i, now3, reason);
        } else {
          return i;
        }
        ;
      }), get(state3, "items")));
    });
    f644.cljs$lang$maxFixedArity = 4;
    return f644;
  })();
  var dismiss_all = function(state3, now3) {
    return assoc(state3, "items", mapv((function(i) {
      return close2(i, now3, "dismiss");
    }), get(state3, "items")));
  };
  var pause = function(state3, now3) {
    if (truth_(get(state3, "paused-at"))) {
      return state3;
    } else {
      return assoc(state3, "paused-at", now3);
    }
    ;
  };
  var resume = function(state3, now3) {
    const temp__23007__auto__1 = get(state3, "paused-at");
    if (truth_(temp__23007__auto__1)) {
      const paused_at2 = temp__23007__auto__1;
      const d3 = now3 - paused_at2;
      return assoc(state3, "paused-at", null, "items", mapv((function(i) {
        const G__6474 = i;
        if (truth_((() => {
          const and__23442__auto__5 = get(i, "expires-at");
          if (truth_(and__23442__auto__5)) {
            return not(get(i, "removed-at"));
          } else {
            return and__23442__auto__5;
          }
          ;
        })())) {
          return assoc(G__6474, "expires-at", get(i, "expires-at") + d3);
        } else {
          return G__6474;
        }
        ;
      }), get(state3, "items")));
    } else {
      return state3;
    }
    ;
  };
  var expire = function(state3, now3) {
    const paused_QMARK_1 = get(state3, "paused-at");
    return assoc(state3, "items", into([], keep((function(i) {
      if (truth_(get(i, "removed-at"))) {
        if (now3 < get(i, "removed-at") + remove_delay) {
          return i;
        }
      } else {
        if (truth_(not(paused_QMARK_1) && (() => {
          const and__23442__auto__2 = get(i, "expires-at");
          if (truth_(and__23442__auto__2)) {
            return get(i, "expires-at") <= now3;
          } else {
            return and__23442__auto__2;
          }
          ;
        })())) {
          return close2(i, now3, "auto");
        } else {
          if ("else") {
            return i;
          } else {
            return null;
          }
        }
      }
      ;
    })), get(state3, "items")));
  };
  var next_due = function(state3) {
    const paused_QMARK_1 = get(state3, "paused-at");
    const ts2 = keep((function(i) {
      const temp__23007__auto__3 = get(i, "removed-at");
      if (truth_(temp__23007__auto__3)) {
        const r4 = temp__23007__auto__3;
        return r4 + remove_delay;
      } else {
        if (truth_(paused_QMARK_1)) {
          return null;
        } else {
          return get(i, "expires-at");
        }
      }
      ;
    }), get(state3, "items"));
    if (truth_(seq(ts2))) {
      return apply(min, ts2);
    }
    ;
  };
  var newly_closed = function(before, after) {
    const was1 = set(keep((function(i) {
      if (truth_(get(i, "removed-at"))) {
        return get(i, "id");
      }
      ;
    }), get(before, "items")));
    return filterv((function(i) {
      const and__23442__auto__2 = get(i, "removed-at");
      if (truth_(and__23442__auto__2)) {
        return not(contains_QMARK_(was1, get(i, "id")));
      } else {
        return and__23442__auto__2;
      }
      ;
    }), get(after, "items"));
  };
  var visible = function(state3) {
    const muted1 = (() => {
      const or__23426__auto__2 = get(state3, "muted");
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return /* @__PURE__ */ new Set([]);
      }
      ;
    })();
    return filterv((function(i) {
      return not(contains_QMARK_(muted1, get(i, "source")));
    }), get(state3, "items"));
  };
  var active = function(state3) {
    return filterv((function(i) {
      return not(get(i, "removed-at"));
    }), visible(state3));
  };

  // .compiled/notification.mjs
  var swipe_threshold = 45;
  var velocity_threshold = 0.11;
  var own_attrs = /* @__PURE__ */ new Set(["data-mounted", "data-expanded", "data-swiping", "data-swiped", "data-swipe-out", "data-swipe-direction", "style"]);
  var offsets = function(heights, gap3) {
    let hs1 = heights;
    let i2 = 0;
    let sum3 = 0;
    let out4 = [];
    while (true) {
      if (truth_(empty_QMARK_(hs1))) {
        return out4;
      } else {
        let G__5 = rest(hs1);
        let G__6 = i2 + 1;
        let G__7 = sum3 + first(hs1);
        let G__8 = conj(out4, i2 * gap3 + sum3);
        hs1 = G__5;
        i2 = G__6;
        sum3 = G__7;
        out4 = G__8;
        continue;
      }
      ;
      ;
      break;
    }
    ;
  };
  var dampen = function(delta) {
    const d1 = delta * (1 / (1.5 + Math.abs(delta) / 20));
    if (Math.abs(d1) < Math.abs(delta)) {
      return d1;
    } else {
      return delta;
    }
    ;
  };
  var swipe_amount = function(axis, dx, dy, directions) {
    const has_QMARK_1 = (function(d) {
      return some((function(x) {
        return _EQ_(x, d);
      }), directions);
    });
    if (axis === "y") {
      return { "x": 0, "y": not((() => {
        const or__23426__auto__2 = has_QMARK_1("top");
        if (truth_(or__23426__auto__2)) {
          return or__23426__auto__2;
        } else {
          return has_QMARK_1("bottom");
        }
        ;
      })()) ? 0 : truth_((() => {
        const or__23426__auto__3 = (() => {
          const and__23442__auto__4 = has_QMARK_1("top");
          if (truth_(and__23442__auto__4)) {
            return dy < 0;
          } else {
            return and__23442__auto__4;
          }
          ;
        })();
        if (truth_(or__23426__auto__3)) {
          return or__23426__auto__3;
        } else {
          const and__23442__auto__5 = has_QMARK_1("bottom");
          if (truth_(and__23442__auto__5)) {
            return dy > 0;
          } else {
            return and__23442__auto__5;
          }
          ;
        }
        ;
      })()) ? dy : "else" ? dampen(dy) : null };
    } else {
      if (axis === "x") {
        return { "y": 0, "x": not((() => {
          const or__23426__auto__6 = has_QMARK_1("left");
          if (truth_(or__23426__auto__6)) {
            return or__23426__auto__6;
          } else {
            return has_QMARK_1("right");
          }
          ;
        })()) ? 0 : truth_((() => {
          const or__23426__auto__7 = (() => {
            const and__23442__auto__8 = has_QMARK_1("left");
            if (truth_(and__23442__auto__8)) {
              return dx < 0;
            } else {
              return and__23442__auto__8;
            }
            ;
          })();
          if (truth_(or__23426__auto__7)) {
            return or__23426__auto__7;
          } else {
            const and__23442__auto__9 = has_QMARK_1("right");
            if (truth_(and__23442__auto__9)) {
              return dx > 0;
            } else {
              return and__23442__auto__9;
            }
            ;
          }
          ;
        })()) ? dx : "else" ? dampen(dx) : null };
      } else {
        if ("else") {
          return { "x": 0, "y": 0 };
        } else {
          return null;
        }
      }
    }
    ;
  };
  var swipe_out = function(axis, x, y, ms, directions) {
    const amount1 = axis === "x" ? x : y;
    const dir2 = axis === "x" ? x > 0 ? "right" : "left" : y > 0 ? "bottom" : "top";
    const allowed3 = some((function(d) {
      return _EQ_(d, dir2);
    }), directions);
    const v4 = Math.abs(amount1) / max(ms, 1);
    if (truth_((() => {
      const and__23442__auto__5 = axis;
      if (truth_(and__23442__auto__5)) {
        const and__23442__auto__6 = allowed3;
        if (truth_(and__23442__auto__6)) {
          const or__23426__auto__7 = Math.abs(amount1) >= swipe_threshold;
          if (or__23426__auto__7) {
            return or__23426__auto__7;
          } else {
            return v4 > velocity_threshold;
          }
          ;
        } else {
          return and__23442__auto__6;
        }
        ;
      } else {
        return and__23442__auto__5;
      }
      ;
    })())) {
      return { "right": "right", "left": "left", "bottom": "down", "top": "up" }[dir2];
    }
    ;
  };
  var attr = function(el2, k) {
    return el2.getAttribute(k);
  };
  var set_attr_BANG_ = function(el2, k, v) {
    if (!_EQ_(attr(el2, k), v)) {
      return el2.setAttribute(k, v);
    }
    ;
  };
  var set_var_BANG_ = function(el2, k, v) {
    return el2.style.setProperty(k, v);
  };
  var toasts = function(ol) {
    return Array.from(ol.querySelectorAll(":scope > [data-ui-notification]"));
  };
  var lists = function(toaster) {
    return Array.from(toaster.querySelectorAll("[data-ui-notification-list]"));
  };
  var live_QMARK_ = function(li) {
    return !("true" === attr(li, "data-removed"));
  };
  var emit_BANG_2 = function(el2, type, detail) {
    return el2.dispatchEvent(new CustomEvent(type, { "bubbles": true, "detail": clj__GT_js(detail) }));
  };
  var natural_height = function(li) {
    const style1 = li.style;
    const h02 = style1.height;
    style1.height = "auto";
    const h3 = li.getBoundingClientRect().height;
    style1.height = h02;
    return h3;
  };
  var state2 = function(toaster) {
    const or__23426__auto__1 = toaster["__uiN"];
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const s2 = { "expanded": false, "interacting": false, "paused": false, "lastFocus": null, "focusWithin": false };
      toaster["__uiN"] = s2;
      return s2;
    }
    ;
  };
  var update_pause_BANG_ = function(toaster) {
    const s1 = state2(toaster);
    const paused2 = boolean$((() => {
      const or__23426__auto__3 = s1["expanded"];
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        const or__23426__auto__4 = s1["interacting"];
        if (truth_(or__23426__auto__4)) {
          return or__23426__auto__4;
        } else {
          return document.hidden;
        }
        ;
      }
      ;
    })());
    if (!_EQ_(paused2, s1["paused"])) {
      s1["paused"] = paused2;
      return emit_BANG_2(toaster, "notificationpause", { "paused": paused2 });
    }
    ;
  };
  var set_expanded_BANG_ = function(toaster, expanded_QMARK_) {
    const s1 = state2(toaster);
    if (!_EQ_(expanded_QMARK_, s1["expanded"])) {
      s1["expanded"] = expanded_QMARK_;
      sync_toaster_BANG_(toaster);
      return update_pause_BANG_(toaster);
    }
    ;
  };
  var copy_offsets_BANG_ = function(ol) {
    for (let G__1 of iterable(["offset", "mobile-offset"])) {
      const prefix2 = G__1;
      for (let G__3 of iterable(["top", "right", "bottom", "left"])) {
        const side4 = G__3;
        const temp__23062__auto__5 = attr(ol, `data-${prefix2 ?? ""}-${side4 ?? ""}`);
        if (truth_(temp__23062__auto__5)) {
          const v6 = temp__23062__auto__5;
          set_var_BANG_(ol, `--${prefix2 ?? ""}-${side4 ?? ""}`, v6);
        }
      }
    }
    ;
    const temp__23062__auto__7 = attr(ol, "data-gap");
    if (truth_(temp__23062__auto__7)) {
      const g8 = temp__23062__auto__7;
      return set_var_BANG_(ol, "--gap", `${g8 ?? ""}px`);
    }
    ;
  };
  var sync_list_BANG_ = function(toaster, ol) {
    copy_offsets_BANG_(ol);
    const items1 = toasts(ol);
    const gap22 = (() => {
      const or__23426__auto__3 = parseFloat(attr(ol, "data-gap"));
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return default_gap;
      }
      ;
    })();
    const live4 = filterv(live_QMARK_, items1);
    const heights5 = mapv(natural_height, live4);
    const offs6 = offsets(heights5, gap22);
    const by_li7 = /* @__PURE__ */ new Map();
    const total8 = count(items1);
    const expand_QMARK_9 = "true" === attr(ol, "data-expand");
    const expanded_QMARK_10 = (() => {
      const and__23442__auto__11 = state2(toaster)["expanded"];
      if (truth_(and__23442__auto__11)) {
        return count(live4) > 1;
      } else {
        return and__23442__auto__11;
      }
      ;
    })();
    for (let G__12 of iterable(map_indexed(vector, live4))) {
      const vec__1316 = G__12;
      const i17 = nth(vec__1316, 0, null);
      const li18 = nth(vec__1316, 1, null);
      by_li7.set(li18, [nth(heights5, i17), nth(offs6, i17)]);
    }
    ;
    set_var_BANG_(ol, "--front-toast-height", `${(() => {
      const or__23426__auto__19 = first(heights5);
      if (truth_(or__23426__auto__19)) {
        return or__23426__auto__19;
      } else {
        return 0;
      }
      ;
    })() ?? ""}px`);
    for (let G__20 of iterable(map_indexed(vector, items1))) {
      const vec__2124 = G__20;
      const idx25 = nth(vec__2124, 0, null);
      const li26 = nth(vec__2124, 1, null);
      const mounted_QMARK_27 = "true" === attr(li26, "data-mounted");
      set_var_BANG_(li26, "--index", `${idx25 ?? ""}`);
      set_var_BANG_(li26, "--toasts-before", `${idx25 ?? ""}`);
      set_var_BANG_(li26, "--z-index", `${total8 - idx25}`);
      const temp__23062__auto__28 = by_li7.get(li26);
      if (truth_(temp__23062__auto__28)) {
        const hv29 = temp__23062__auto__28;
        set_var_BANG_(li26, "--offset", `${hv29[1] ?? ""}px`);
        set_var_BANG_(li26, "--initial-height", expand_QMARK_9 ? "auto" : `${hv29[0] ?? ""}px`);
      }
      ;
      for (let G__30 of iterable(["data-swiping", "data-swiped", "data-swipe-out"])) {
        const k31 = G__30;
        if (truth_(attr(li26, k31))) {
        } else {
          li26.setAttribute(k31, "false");
        }
      }
      ;
      set_attr_BANG_(li26, "data-expanded", truth_((() => {
        const or__23426__auto__32 = expanded_QMARK_10;
        if (truth_(or__23426__auto__32)) {
          return or__23426__auto__32;
        } else {
          return expand_QMARK_9 && mounted_QMARK_27;
        }
        ;
      })()) ? "true" : "false");
      if (mounted_QMARK_27) {
      } else {
        if (truth_(li26["__uiNMounting"])) {
        } else {
          li26["__uiNMounting"] = true;
          requestAnimationFrame((function() {
            li26.setAttribute("data-mounted", "true");
            return set_attr_BANG_(li26, "data-expanded", truth_((() => {
              const or__23426__auto__33 = state2(toaster)["expanded"];
              if (truth_(or__23426__auto__33)) {
                return or__23426__auto__33;
              } else {
                return expand_QMARK_9;
              }
              ;
            })()) ? "true" : "false");
          }));
        }
      }
    }
    return null;
  };
  var swipe_directions = function(li) {
    const ol1 = li.closest("[data-ui-notification-list]");
    return (() => {
      const or__23426__auto__2 = (() => {
        const G__5333 = ol1;
        if (G__5333 == null) {
          return null;
        } else {
          return attr(G__5333, "data-swipe-directions");
        }
        ;
      })();
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return "";
      }
      ;
    })().split(" ");
  };
  var swipable_QMARK_ = function(li) {
    return "true" === attr(li, "data-dismissible") && !("loading" === attr(li, "data-type"));
  };
  var bind_toast_BANG_ = function(li) {
    if (truth_(li["__uiNBound"])) {
      return null;
    } else {
      li["__uiNBound"] = true;
      const drag1 = { "start": null, "t": 0, "axis": null };
      li.addEventListener("pointerdown", (function(e) {
        if (truth_(!(2 === e.button) && swipable_QMARK_(li))) {
          drag1["t"] = Date.now();
          e.target.setPointerCapture(e.pointerId);
          if ("BUTTON" === e.target.tagName) {
            return null;
          } else {
            li.setAttribute("data-swiping", "true");
            return drag1["start"] = { "x": e.clientX, "y": e.clientY };
          }
          ;
        }
        ;
      }));
      li.addEventListener("pointermove", (function(e) {
        const temp__23062__auto__2 = drag1["start"];
        if (truth_(temp__23062__auto__2)) {
          const start3 = temp__23062__auto__2;
          if (`${window.getSelection() ?? ""}`.length > 0) {
            return null;
          } else {
            const dx4 = e.clientX - start3["x"];
            const dy5 = e.clientY - start3["y"];
            if (truth_(drag1["axis"] == null && (() => {
              const or__23426__auto__6 = Math.abs(dx4) > 1;
              if (or__23426__auto__6) {
                return or__23426__auto__6;
              } else {
                return Math.abs(dy5) > 1;
              }
              ;
            })())) {
              drag1["axis"] = Math.abs(dx4) > Math.abs(dy5) ? "x" : "y";
            }
            ;
            const map__78 = swipe_amount(drag1["axis"], dx4, dy5, swipe_directions(li));
            const x9 = get(map__78, "x");
            const y10 = get(map__78, "y");
            if (truth_((() => {
              const or__23426__auto__11 = Math.abs(x9) > 0;
              if (or__23426__auto__11) {
                return or__23426__auto__11;
              } else {
                return Math.abs(y10) > 0;
              }
              ;
            })())) {
              li.setAttribute("data-swiped", "true");
            }
            ;
            set_var_BANG_(li, "--swipe-amount-x", `${x9 ?? ""}px`);
            return set_var_BANG_(li, "--swipe-amount-y", `${y10 ?? ""}px`);
          }
          ;
        }
        ;
      }));
      li.addEventListener("pointerup", (function(_) {
        if (truth_((() => {
          const and__23442__auto__12 = swipable_QMARK_(li);
          if (truth_(and__23442__auto__12)) {
            return !("true" === attr(li, "data-swipe-out"));
          } else {
            return and__23442__auto__12;
          }
          ;
        })())) {
          const x13 = (() => {
            const or__23426__auto__14 = parseFloat(li.style.getPropertyValue("--swipe-amount-x"));
            if (truth_(or__23426__auto__14)) {
              return or__23426__auto__14;
            } else {
              return 0;
            }
            ;
          })();
          const y15 = (() => {
            const or__23426__auto__16 = parseFloat(li.style.getPropertyValue("--swipe-amount-y"));
            if (truth_(or__23426__auto__16)) {
              return or__23426__auto__16;
            } else {
              return 0;
            }
            ;
          })();
          const out17 = swipe_out(drag1["axis"], x13, y15, Date.now() - drag1["t"], swipe_directions(li));
          drag1["start"] = null;
          if (truth_(out17)) {
            li.setAttribute("data-swipe-direction", out17);
            li.setAttribute("data-swipe-out", "true");
            return emit_BANG_2(li, "notificationdismiss", { "id": attr(li, "data-id"), "reason": "swipe" });
          } else {
            set_var_BANG_(li, "--swipe-amount-x", "0px");
            set_var_BANG_(li, "--swipe-amount-y", "0px");
            li.setAttribute("data-swiped", "false");
            li.setAttribute("data-swiping", "false");
            return drag1["axis"] = null;
          }
          ;
        }
        ;
      }));
      return li.addEventListener("dragend", (function(_) {
        li.setAttribute("data-swiping", "false");
        drag1["start"] = null;
        return drag1["axis"] = null;
      }));
    }
    ;
  };
  var not_dismissible_target_QMARK_ = function(e) {
    const G__5341 = e.target;
    if (G__5341 == null) {
      return null;
    } else {
      return G__5341.closest("[data-dismissible='false']");
    }
    ;
  };
  var bind_list_BANG_ = function(toaster, ol) {
    if (truth_(ol["__uiNBound"])) {
      return null;
    } else {
      ol["__uiNBound"] = true;
      const s1 = state2(toaster);
      ol.addEventListener("mouseenter", (function(_) {
        return set_expanded_BANG_(toaster, true);
      }));
      ol.addEventListener("mousemove", (function(_) {
        return set_expanded_BANG_(toaster, true);
      }));
      ol.addEventListener("mouseleave", (function(_) {
        if (truth_(s1["interacting"])) {
          return null;
        } else {
          return set_expanded_BANG_(toaster, false);
        }
        ;
      }));
      ol.addEventListener("dragend", (function(_) {
        return set_expanded_BANG_(toaster, false);
      }));
      ol.addEventListener("pointerdown", (function(e) {
        if (truth_(not_dismissible_target_QMARK_(e))) {
          return null;
        } else {
          s1["interacting"] = true;
          return update_pause_BANG_(toaster);
        }
        ;
      }));
      ol.addEventListener("pointerup", (function(_) {
        s1["interacting"] = false;
        return update_pause_BANG_(toaster);
      }));
      ol.addEventListener("focusin", (function(e) {
        if (truth_((() => {
          const or__23426__auto__2 = not_dismissible_target_QMARK_(e);
          if (truth_(or__23426__auto__2)) {
            return or__23426__auto__2;
          } else {
            return s1["focusWithin"];
          }
          ;
        })())) {
          return null;
        } else {
          s1["focusWithin"] = true;
          return s1["lastFocus"] = e.relatedTarget;
        }
        ;
      }));
      return ol.addEventListener("focusout", (function(e) {
        if (truth_((() => {
          const and__23442__auto__3 = s1["focusWithin"];
          if (truth_(and__23442__auto__3)) {
            return not(ol.contains(e.relatedTarget));
          } else {
            return and__23442__auto__3;
          }
          ;
        })())) {
          s1["focusWithin"] = false;
          const temp__23062__auto__4 = s1["lastFocus"];
          if (truth_(temp__23062__auto__4)) {
            const prev5 = temp__23062__auto__4;
            prev5.focus({ "preventScroll": true });
            return s1["lastFocus"] = null;
          }
          ;
        }
        ;
      }));
    }
    ;
  };
  var sync_toaster_BANG_ = function(toaster) {
    for (let G__1 of iterable(lists(toaster))) {
      const ol2 = G__1;
      bind_list_BANG_(toaster, ol2);
      for (let G__3 of iterable(toasts(ol2))) {
        const li4 = G__3;
        bind_toast_BANG_(li4);
      }
      ;
      sync_list_BANG_(toaster, ol2);
    }
    ;
    if (reduce(_PLUS_, 0, map((function(ol) {
      return count(filter(live_QMARK_, toasts(ol)));
    }), lists(toaster))) <= 1) {
      if (truth_(state2(toaster)["expanded"])) {
        state2(toaster)["expanded"] = false;
        return update_pause_BANG_(toaster);
      }
      ;
    }
    ;
  };
  var sync_all_BANG_ = function() {
    for (let G__1 of iterable(Array.from(document.querySelectorAll("[data-ui-notification-toaster]")))) {
      const t2 = G__1;
      sync_toaster_BANG_(t2);
    }
    return null;
  };
  var scheduled = atom(false);
  var schedule_BANG_2 = function() {
    if (truth_(deref(scheduled))) {
      return null;
    } else {
      reset_BANG_(scheduled, true);
      return requestAnimationFrame((function() {
        reset_BANG_(scheduled, false);
        return sync_all_BANG_();
      }));
    }
    ;
  };
  var on_click3 = function(e) {
    if (truth_(e.defaultPrevented)) {
      return null;
    } else {
      const temp__23062__auto__1 = (() => {
        const G__5352 = e.target;
        if (G__5352 == null) {
          return null;
        } else {
          return G__5352.closest("[data-ui-notification]");
        }
        ;
      })();
      if (truth_(temp__23062__auto__1)) {
        const li3 = temp__23062__auto__1;
        const dismissible_QMARK_4 = "true" === attr(li3, "data-dismissible");
        const target5 = e.target;
        const reason6 = truth_(target5.closest("[data-close-button]")) ? dismissible_QMARK_4 ? "close" : null : truth_(target5.closest("[data-action]")) ? "action" : truth_(target5.closest("[data-cancel]")) ? dismissible_QMARK_4 ? "cancel" : null : null;
        if (truth_(reason6)) {
          return emit_BANG_2(li3, "notificationdismiss", { "id": attr(li3, "data-id"), "reason": reason6 });
        }
        ;
      }
      ;
    }
    ;
  };
  var hotkey_pressed_QMARK_ = function(e, keys2) {
    return count(keys2) > 0 && every_QMARK_((function(k) {
      const or__23426__auto__1 = e[k];
      if (truth_(or__23426__auto__1)) {
        return or__23426__auto__1;
      } else {
        return _EQ_(e.code, k);
      }
      ;
    }), keys2);
  };
  var on_keydown3 = function(e) {
    for (let G__1 of iterable(Array.from(document.querySelectorAll("[data-ui-notification-toaster]")))) {
      const t2 = G__1;
      const keys3 = (() => {
        const or__23426__auto__4 = attr(t2, "data-hotkey");
        if (truth_(or__23426__auto__4)) {
          return or__23426__auto__4;
        } else {
          return "";
        }
        ;
      })().split("+");
      if (truth_(hotkey_pressed_QMARK_(e, keys3))) {
        set_expanded_BANG_(t2, true);
        const G__5365 = first(lists(t2));
        if (G__5365 == null) {
        } else {
          G__5365.focus();
        }
      }
      ;
      if (truth_("Escape" === e.code && t2.contains(document.activeElement))) {
        set_expanded_BANG_(t2, false);
      }
    }
    return null;
  };
  var svg_ns = "http://www.w3.org/2000/svg";
  var element_QMARK_ = function(h) {
    const and__23442__auto__1 = vector_QMARK_(h);
    if (truth_(and__23442__auto__1)) {
      return string_QMARK_(first(h));
    } else {
      return and__23442__auto__1;
    }
    ;
  };
  var plain_map_QMARK_ = function(x) {
    return !(x == null) && (not(vector_QMARK_(x)) && _EQ_(x.constructor, Object));
  };
  var set_attrs_BANG_ = function(el2, attrs) {
    for (let G__1 of iterable(Object.entries((() => {
      const or__23426__auto__2 = attrs;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return {};
      }
      ;
    })()))) {
      const vec__36 = G__1;
      const k7 = nth(vec__36, 0, null);
      const v8 = nth(vec__36, 1, null);
      if (truth_((() => {
        const and__23442__auto__9 = k7.startsWith("on-");
        if (truth_(and__23442__auto__9)) {
          return fn_QMARK_(v8);
        } else {
          return and__23442__auto__9;
        }
        ;
      })())) {
        el2.addEventListener(k7.slice(3), v8);
      } else {
        if (k7 === "style") {
          for (let G__10 of iterable(Object.entries(v8))) {
            const vec__1114 = G__10;
            const sk15 = nth(vec__1114, 0, null);
            const sv16 = nth(vec__1114, 1, null);
            el2.style.setProperty(sk15, `${sv16 ?? ""}`);
          }
        } else {
          if (truth_((() => {
            const or__23426__auto__17 = v8 == null;
            if (or__23426__auto__17) {
              return or__23426__auto__17;
            } else {
              return false_QMARK_(v8);
            }
            ;
          })())) {
          } else {
            if (truth_(true_QMARK_(v8))) {
              el2.setAttribute(k7, "");
            } else {
              if ("else") {
                el2.setAttribute(k7, `${v8 ?? ""}`);
              } else {
              }
            }
          }
        }
      }
    }
    return null;
  };
  var dom = (() => {
    const f537 = (function(...args538) {
      const G__5391 = args538.length;
      switch (G__5391) {
        case 1:
          return f537.cljs$core$IFn$_invoke$arity$1(args538[0]);
          break;
        case 2:
          return f537.cljs$core$IFn$_invoke$arity$2(args538[0], args538[1]);
          break;
        default:
          throw new Error(`${"Invalid arity: "}${args538.length ?? ""}`);
      }
      ;
    });
    f537.cljs$core$IFn$_invoke$arity$1 = (function(h) {
      return dom(h, false);
    });
    f537.cljs$core$IFn$_invoke$arity$2 = (function(h, svg_QMARK_) {
      if (h == null) {
        return null;
      } else {
        if (truth_((() => {
          const or__23426__auto__3 = string_QMARK_(h);
          if (truth_(or__23426__auto__3)) {
            return or__23426__auto__3;
          } else {
            return number_QMARK_(h);
          }
          ;
        })())) {
          return document.createTextNode(`${h ?? ""}`);
        } else {
          if (truth_(element_QMARK_(h))) {
            const tag7 = first(h);
            const svg_QMARK_8 = (() => {
              const or__23426__auto__9 = svg_QMARK_;
              if (truth_(or__23426__auto__9)) {
                return or__23426__auto__9;
              } else {
                return tag7 === "svg";
              }
              ;
            })();
            const el10 = truth_(svg_QMARK_8) ? document.createElementNS(svg_ns, tag7) : document.createElement(tag7);
            const vec__411 = truth_(plain_map_QMARK_(second(h))) ? [second(h), drop(2, h)] : [null, rest(h)];
            const attrs12 = nth(vec__411, 0, null);
            const kids13 = nth(vec__411, 1, null);
            set_attrs_BANG_(el10, attrs12);
            for (let G__14 of iterable(mapcat((function(c) {
              if (truth_((() => {
                const or__23426__auto__15 = c == null;
                if (or__23426__auto__15) {
                  return or__23426__auto__15;
                } else {
                  const or__23426__auto__16 = string_QMARK_(c);
                  if (truth_(or__23426__auto__16)) {
                    return or__23426__auto__16;
                  } else {
                    const or__23426__auto__17 = number_QMARK_(c);
                    if (truth_(or__23426__auto__17)) {
                      return or__23426__auto__17;
                    } else {
                      return element_QMARK_(c);
                    }
                    ;
                  }
                  ;
                }
                ;
              })())) {
                return [c];
              } else {
                return c;
              }
              ;
            }), kids13))) {
              const c18 = G__14;
              const temp__23062__auto__19 = dom(c18, svg_QMARK_8);
              if (truth_(temp__23062__auto__19)) {
                const node20 = temp__23062__auto__19;
                el10.appendChild(node20);
              }
            }
            ;
            return el10;
          } else {
            if ("else") {
              return null;
            } else {
              return null;
            }
          }
        }
      }
      ;
    });
    f537.cljs$lang$maxFixedArity = 2;
    return f537;
  })();
  var patch_attrs_BANG_ = function(el2, attrs) {
    const keep12 = /* @__PURE__ */ new Set();
    for (let G__2 of iterable(Object.entries((() => {
      const or__23426__auto__3 = attrs;
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return {};
      }
      ;
    })()))) {
      const vec__47 = G__2;
      const k8 = nth(vec__47, 0, null);
      const v9 = nth(vec__47, 1, null);
      if (truth_(!(v9 == null) && (not(false_QMARK_(v9)) && not((() => {
        const and__23442__auto__10 = k8.startsWith("on-");
        if (truth_(and__23442__auto__10)) {
          return fn_QMARK_(v9);
        } else {
          return and__23442__auto__10;
        }
        ;
      })())))) {
        keep12.add(k8);
      }
    }
    ;
    for (let G__11 of iterable(Array.from(el2.attributes))) {
      const a12 = G__11;
      const k13 = a12.name;
      if (truth_((() => {
        const or__23426__auto__14 = keep12.has(k13);
        if (truth_(or__23426__auto__14)) {
          return or__23426__auto__14;
        } else {
          return contains_QMARK_(own_attrs, k13);
        }
        ;
      })())) {
      } else {
        el2.removeAttribute(k13);
      }
    }
    ;
    for (let G__15 of iterable(Object.entries((() => {
      const or__23426__auto__16 = attrs;
      if (truth_(or__23426__auto__16)) {
        return or__23426__auto__16;
      } else {
        return {};
      }
      ;
    })()))) {
      const vec__1720 = G__15;
      const k21 = nth(vec__1720, 0, null);
      const v22 = nth(vec__1720, 1, null);
      if (truth_((() => {
        const or__23426__auto__23 = v22 == null;
        if (or__23426__auto__23) {
          return or__23426__auto__23;
        } else {
          const or__23426__auto__24 = false_QMARK_(v22);
          if (truth_(or__23426__auto__24)) {
            return or__23426__auto__24;
          } else {
            return k21.startsWith("on-");
          }
          ;
        }
        ;
      })())) {
      } else {
        if (truth_(true_QMARK_(v22))) {
          set_attr_BANG_(el2, k21, "");
        } else {
          if ("else") {
            set_attr_BANG_(el2, k21, `${v22 ?? ""}`);
          } else {
          }
        }
      }
    }
    return null;
  };
  var patch_list_BANG_ = function(ol, h) {
    patch_attrs_BANG_(ol, second(h));
    const items1 = vec(drop(2, h));
    const wanted2 = new Set(map((function(i) {
      return second(i)["data-id"];
    }), items1));
    for (let G__3 of iterable(toasts(ol))) {
      const li4 = G__3;
      if (truth_(wanted2.has(attr(li4, "data-id")))) {
      } else {
        li4.remove();
      }
    }
    ;
    for (let G__5 of iterable(map_indexed(vector, items1))) {
      const vec__69 = G__5;
      const idx10 = nth(vec__69, 0, null);
      const item11 = nth(vec__69, 1, null);
      const id12 = second(item11)["data-id"];
      const existing13 = ol.querySelector(`${':scope > [data-id="'}${CSS.escape(id12) ?? ""}${'"]'}`);
      const fresh14 = dom(item11);
      if (truth_(existing13)) {
        patch_attrs_BANG_(existing13, second(item11));
        existing13.replaceChildren.apply(existing13, Array.from(fresh14.childNodes));
      } else {
        ol.insertBefore(fresh14, toasts(ol)[idx10]);
      }
    }
    return null;
  };
  var patch_toaster_BANG_ = function(root, h) {
    patch_attrs_BANG_(root, second(h));
    const ols1 = vec(drop(2, h));
    const key2 = (function(a) {
      return `${a["data-y-position"] ?? ""}-${a["data-x-position"] ?? ""}`;
    });
    const wanted3 = new Set(map((function(o) {
      return key2(second(o));
    }), ols1));
    for (let G__4 of iterable(lists(root))) {
      const ol5 = G__4;
      if (truth_(wanted3.has(`${attr(ol5, "data-y-position") ?? ""}-${attr(ol5, "data-x-position") ?? ""}`))) {
      } else {
        ol5.remove();
      }
    }
    ;
    for (let G__6 of iterable(ols1)) {
      const o7 = G__6;
      const k8 = key2(second(o7));
      const ol9 = (() => {
        const or__23426__auto__10 = root.querySelector(`${'[data-ui-notification-list][data-y-position="'}${second(o7)["data-y-position"] ?? ""}${'"][data-x-position="'}${second(o7)["data-x-position"] ?? ""}${'"]'}`);
        if (truth_(or__23426__auto__10)) {
          return or__23426__auto__10;
        } else {
          const el11 = dom([first(o7), second(o7)]);
          root.appendChild(el11);
          return el11;
        }
        ;
      })();
      patch_list_BANG_(ol9, o7);
      k8;
    }
    return null;
  };
  var kebab = function(s) {
    return s.replace(new RegExp("[A-Z]", "g"), (function(c) {
      return `-${c.toLowerCase() ?? ""}`;
    }));
  };
  var props = function(o) {
    const m1 = Object.fromEntries(map((function(p__540) {
      const vec__25 = p__540;
      const k6 = nth(vec__25, 0, null);
      const v7 = nth(vec__25, 1, null);
      return [kebab(k6), v7];
    }), Object.entries((() => {
      const or__23426__auto__8 = o;
      if (truth_(or__23426__auto__8)) {
        return or__23426__auto__8;
      } else {
        return {};
      }
      ;
    })())));
    for (let G__9 of iterable(["action", "cancel"])) {
      const k10 = G__9;
      if (truth_(plain_map_QMARK_(m1[k10]))) {
        m1[k10] = props(m1[k10]);
      }
    }
    ;
    return m1;
  };
  var api = { "queue": queue(), "options": {}, "root": null, "timer": null };
  var now = function() {
    return Date.now();
  };
  var fire_closed_BANG_ = function(before, after) {
    for (let G__1 of iterable(newly_closed(before, after))) {
      const i2 = G__1;
      if ("auto" === get(i2, "closed")) {
        const temp__23062__auto__3 = get(i2, "on-auto-close");
        if (truth_(temp__23062__auto__3)) {
          const f4 = temp__23062__auto__3;
          f4(clj__GT_js(i2));
        }
      } else {
        const temp__23062__auto__5 = get(i2, "on-dismiss");
        if (truth_(temp__23062__auto__5)) {
          const f6 = temp__23062__auto__5;
          f6(clj__GT_js(i2));
        }
      }
    }
    return null;
  };
  var update_queue_BANG_ = (() => {
    const f541 = (function(var_args) {
      const args5421 = [];
      const len__23321__auto__2 = arguments.length;
      let i5433 = 0;
      while (true) {
        if (i5433 < len__23321__auto__2) {
          args5421.push(arguments[i5433]);
          let G__4 = i5433 + 1;
          i5433 = G__4;
          continue;
        }
        ;
        break;
      }
      ;
      const argseq__23513__auto__5 = 1 < args5421.length ? args5421.slice(1) : null;
      return f541.cljs$core$IFn$_invoke$arity$variadic(arguments[0], argseq__23513__auto__5);
    });
    f541.cljs$core$IFn$_invoke$arity$variadic = (function(f, args) {
      const before6 = api["queue"];
      const after7 = apply(f, before6, args);
      api["queue"] = after7;
      fire_closed_BANG_(before6, after7);
      return render_BANG_();
    });
    f541.cljs$lang$maxFixedArity = 1;
    return f541;
  })();
  var schedule_expire_BANG_ = function() {
    const temp__23062__auto__1 = api["timer"];
    if (truth_(temp__23062__auto__1)) {
      const t2 = temp__23062__auto__1;
      clearTimeout(t2);
    }
    ;
    return api["timer"] = (() => {
      const temp__23062__auto__3 = next_due(api["queue"]);
      if (truth_(temp__23062__auto__3)) {
        const due4 = temp__23062__auto__3;
        return setTimeout((function() {
          return update_queue_BANG_(expire, now());
        }), max(0, due4 - now()));
      }
      ;
    })();
  };
  var ensure_root_BANG_2 = function() {
    const or__23426__auto__1 = api["root"];
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const el2 = dom(notification_toaster(props(api["options"])));
      el2.addEventListener("notificationdismiss", (function(e) {
        return update_queue_BANG_(dismiss, e.detail.id, now(), e.detail.reason);
      }));
      el2.addEventListener("notificationpause", (function(e) {
        return update_queue_BANG_(truth_(e.detail.paused) ? pause : resume, now());
      }));
      document.body.appendChild(el2);
      api["root"] = el2;
      return el2;
    }
    ;
  };
  var render_BANG_ = function() {
    const root1 = ensure_root_BANG_2();
    const h2 = notification_toaster(assoc(props(api["options"]), "items", visible(api["queue"])));
    patch_toaster_BANG_(root1, h2);
    schedule_expire_BANG_();
    return schedule_BANG_2();
  };
  var create_BANG_ = function(message, type, opts) {
    const item1 = assoc(props(opts), "title", message, "type", type);
    update_queue_BANG_(post, item1, now(), { "duration": api["options"]["duration"] });
    const or__23426__auto__2 = get(item1, "id");
    if (truth_(or__23426__auto__2)) {
      return or__23426__auto__2;
    } else {
      return `notification-${get(api["queue"], "seq") ?? ""}`;
    }
    ;
  };
  var notify = function(message, opts) {
    return create_BANG_(message, null, opts);
  };
  var promise_BANG_ = function(p, data) {
    const d1 = props(data);
    const id2 = !(get(d1, "loading") == null) ? create_BANG_(get(d1, "loading"), "loading", assoc((() => {
      const or__23426__auto__3 = data;
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return {};
      }
      ;
    })(), "id", (() => {
      const or__23426__auto__4 = get(d1, "id");
      if (truth_(or__23426__auto__4)) {
        return or__23426__auto__4;
      } else {
        return `promise-${now() ?? ""}`;
      }
      ;
    })())) : null;
    const settle5 = (function(kind, v) {
      const spec6 = get(d1, kind);
      const spec7 = truth_(fn_QMARK_(spec6)) ? spec6(v) : spec6;
      const base8 = truth_(plain_map_QMARK_(spec7)) ? spec7 : { "message": spec7 };
      const desc9 = get(d1, "description");
      if (!(spec7 == null)) {
        return create_BANG_((() => {
          const or__23426__auto__10 = base8["message"];
          if (truth_(or__23426__auto__10)) {
            return or__23426__auto__10;
          } else {
            return get(base8, "title");
          }
          ;
        })(), kind === "success" ? "success" : "error", (() => {
          const G__54511 = assoc(base8, "id", id2);
          if (!(desc9 == null)) {
            return assoc(G__54511, "description", truth_(fn_QMARK_(desc9)) ? desc9(v) : desc9);
          } else {
            return G__54511;
          }
          ;
        })());
      }
      ;
    });
    Promise.resolve(truth_(fn_QMARK_(p)) ? p() : p).then((function(v) {
      if (!(get(d1, "success") == null)) {
        return settle5("success", v);
      } else {
        if (truth_(id2)) {
          return update_queue_BANG_(dismiss, id2, now());
        }
      }
      ;
    })).catch((function(err) {
      if (!(get(d1, "error") == null)) {
        return settle5("error", err);
      } else {
        if (truth_(id2)) {
          return update_queue_BANG_(dismiss, id2, now());
        }
      }
      ;
    })).finally((function() {
      const temp__23062__auto__12 = get(d1, "finally");
      if (truth_(temp__23062__auto__12)) {
        const f13 = temp__23062__auto__12;
        return f13();
      }
      ;
    }));
    return id2;
  };
  var consume_markers_BANG_ = function() {
    for (let G__1 of iterable(Array.from(document.querySelectorAll("[data-ui-notify]")))) {
      const el2 = G__1;
      const ds3 = el2.dataset;
      el2.remove();
      create_BANG_(ds3["uiNotify"], ds3["type"], { "id": ds3["id"], "description": ds3["description"], "duration": (() => {
        const G__5464 = ds3["duration"];
        if (G__5464 == null) {
          return null;
        } else {
          return parseFloat(G__5464);
        }
        ;
      })() });
    }
    return null;
  };
  var notify_api = Object.assign(notify, { "promise": promise_BANG_, "dismiss": (function(id) {
    if (!(id == null)) {
      return update_queue_BANG_(dismiss, id, now());
    } else {
      return update_queue_BANG_(dismiss_all, now());
    }
    ;
  }), "toaster": (function(o) {
    api["options"] = o;
    if (truth_(api["root"])) {
      return render_BANG_();
    }
    ;
  }), "warning": (function(m, o) {
    return create_BANG_(m, "warning", o);
  }), "success": (function(m, o) {
    return create_BANG_(m, "success", o);
  }), "loading": (function(m, o) {
    return create_BANG_(m, "loading", o);
  }), "info": (function(m, o) {
    return create_BANG_(m, "info", o);
  }), "error": (function(m, o) {
    return create_BANG_(m, "error", o);
  }), "getToasts": (function() {
    return clj__GT_js(active(api["queue"]));
  }), "message": (function(m, o) {
    return create_BANG_(m, null, o);
  }) });
  var watched = "[data-ui-notification-toaster],[data-ui-notify]";
  var relevant_QMARK_ = function(r) {
    const t1 = r.target;
    const el2 = 1 === t1.nodeType ? t1 : t1.parentElement;
    const or__23426__auto__3 = (() => {
      const G__5474 = el2;
      if (G__5474 == null) {
        return null;
      } else {
        return G__5474.closest("[data-ui-notification-toaster]");
      }
      ;
    })();
    if (truth_(or__23426__auto__3)) {
      return or__23426__auto__3;
    } else {
      return some((function(node) {
        return 1 === node.nodeType && (() => {
          const or__23426__auto__5 = node.matches(watched);
          if (truth_(or__23426__auto__5)) {
            return or__23426__auto__5;
          } else {
            return node.querySelector(watched);
          }
          ;
        })();
      }), Array.from(r.addedNodes));
    }
    ;
  };
  var init_BANG_5 = function() {
    sync_all_BANG_();
    consume_markers_BANG_();
    document.addEventListener("click", on_click3);
    document.addEventListener("keydown", on_keydown3);
    document.addEventListener("visibilitychange", (function() {
      for (let G__1 of iterable(Array.from(document.querySelectorAll("[data-ui-notification-toaster]")))) {
        const t2 = G__1;
        update_pause_BANG_(t2);
      }
      return null;
    }));
    window.addEventListener("resize", schedule_BANG_2);
    const obs3 = new MutationObserver((function(records) {
      if (truth_(some(relevant_QMARK_, records))) {
        consume_markers_BANG_();
        return schedule_BANG_2();
      }
      ;
    }));
    return obs3.observe(document.documentElement, { "childList": true, "subtree": true, "characterData": true, "attributes": true, "attributeFilter": ["data-removed", "data-index", "data-type"] });
  };
  window["__uiNotify"] = notify_api;
  if ("loading" === document.readyState) {
    document.addEventListener("DOMContentLoaded", init_BANG_5);
  } else {
    init_BANG_5();
  }

  // .compiled/number_field.mjs
  var num = function(v, fallback) {
    const n1 = parseFloat(v);
    if (truth_(isNaN(n1))) {
      return fallback;
    } else {
      return n1;
    }
    ;
  };
  var closest_field = function(el2) {
    if (truth_((() => {
      const and__23442__auto__1 = el2;
      if (truth_(and__23442__auto__1)) {
        return el2.closest;
      } else {
        return and__23442__auto__1;
      }
      ;
    })())) {
      return el2.closest("[data-ui-number-field]");
    }
    ;
  };
  var disabled_QMARK_ = function(field) {
    return field.hasAttribute("data-ui-number-disabled");
  };
  var field_input = function(field) {
    return field.querySelector("input");
  };
  var round6 = function(n) {
    return Math.round(n * 1e6) / 1e6;
  };
  var step_input_BANG_ = function(input, dir) {
    if (truth_((() => {
      const and__23442__auto__1 = input;
      if (truth_(and__23442__auto__1)) {
        return not(input.disabled);
      } else {
        return and__23442__auto__1;
      }
      ;
    })())) {
      const raw_step2 = num(input.step, 1);
      const step3 = raw_step2 > 0 ? raw_step2 : 1;
      const cur4 = num(input.value, 0);
      const min_STAR_5 = !_EQ_("", input.min) ? num(input.min, null) : null;
      const max_STAR_6 = !_EQ_("", input.max) ? num(input.max, null) : null;
      const next7 = round6(cur4 + dir * step3);
      const next8 = (() => {
        const G__5489 = next7;
        const G__54810 = !(min_STAR_5 == null) ? Math.max(G__5489, min_STAR_5) : G__5489;
        if (!(max_STAR_6 == null)) {
          return Math.min(G__54810, max_STAR_6);
        } else {
          return G__54810;
        }
        ;
      })();
      input.value = `${next8 ?? ""}`;
      input.dispatchEvent(new Event("input", { "bubbles": true }));
      return input.dispatchEvent(new Event("change", { "bubbles": true }));
    }
    ;
  };
  var on_wheel2 = function(e) {
    const temp__23062__auto__1 = closest_field(e.target);
    if (truth_(temp__23062__auto__1)) {
      const field2 = temp__23062__auto__1;
      if (truth_(disabled_QMARK_(field2))) {
        return null;
      } else {
        const temp__23062__auto__3 = field_input(field2);
        if (truth_(temp__23062__auto__3)) {
          const input4 = temp__23062__auto__3;
          e.preventDefault();
          return step_input_BANG_(input4, e.deltaY < 0 ? 1 : -1);
        }
        ;
      }
      ;
    }
    ;
  };
  var on_click4 = function(e) {
    const temp__23062__auto__1 = (() => {
      const and__23442__auto__2 = e.target;
      if (truth_(and__23442__auto__2)) {
        const and__23442__auto__3 = e.target.closest;
        if (truth_(and__23442__auto__3)) {
          return e.target.closest("[data-ui-number-step]");
        } else {
          return and__23442__auto__3;
        }
        ;
      } else {
        return and__23442__auto__2;
      }
      ;
    })();
    if (truth_(temp__23062__auto__1)) {
      const btn4 = temp__23062__auto__1;
      const temp__23062__auto__5 = closest_field(btn4);
      if (truth_(temp__23062__auto__5)) {
        const field6 = temp__23062__auto__5;
        if (truth_(disabled_QMARK_(field6))) {
          return null;
        } else {
          return step_input_BANG_(field_input(field6), num(btn4.getAttribute("data-ui-number-step"), 0));
        }
        ;
      }
      ;
    }
    ;
  };
  var init_BANG_6 = function() {
    document.addEventListener("wheel", on_wheel2, { "passive": false });
    return document.addEventListener("click", on_click4);
  };
  init_BANG_6();

  // .compiled/panels.mjs
  var easing = "cubic-bezier(0.32, 0.72, 0, 1)";
  var duration = 250;
  var key_step = 10;
  var key_step_fast = 50;
  var pan_threshold = 3;
  var axes_config = { "horizontal": { "client": "clientWidth", "extent": "width", "cursor": "col-resize", "grow": "ArrowRight", "shrink": "ArrowLeft", "sepOrient": "vertical" }, "vertical": { "client": "clientHeight", "extent": "height", "cursor": "row-resize", "grow": "ArrowDown", "shrink": "ArrowUp", "sepOrient": "horizontal" } };
  var clamp4 = function(v, lo, hi) {
    return Math.min(Math.max(v, lo), hi);
  };
  var round2 = function(v) {
    return Math.round(v * 100) / 100;
  };
  var reduced_motion_QMARK_3 = function() {
    return matchMedia("(prefers-reduced-motion: reduce)").matches;
  };
  var has_attr_QMARK_ = function(el2, attr2) {
    return !(el2 == null) && (!(el2.hasAttribute == null) && el2.hasAttribute(attr2));
  };
  var fill_node_QMARK_ = function(el2) {
    return has_attr_QMARK_(el2, "data-ui-panels-fill");
  };
  var separator_node_QMARK_ = function(el2) {
    return has_attr_QMARK_(el2, "data-ui-panels-separator");
  };
  var fill_after_QMARK_ = function(el2) {
    let n1 = el2.nextElementSibling;
    while (true) {
      if (n1 == null) {
        return false;
      } else {
        if (truth_(fill_node_QMARK_(n1))) {
          return true;
        } else {
          if ("else") {
            let G__2 = n1.nextElementSibling;
            n1 = G__2;
            continue;
          } else {
            return null;
          }
        }
      }
      ;
      ;
      break;
    }
    ;
  };
  var extent_key = function(group) {
    return group.axes["extent"];
  };
  var group_extent = function(group) {
    return group.el[group.axes["client"]];
  };
  var parse_size = function(s) {
    if (truth_(!(s == null) && !_EQ_(s, ""))) {
      const pct1 = s.endsWith("%");
      const v2 = parseFloat(s);
      if (truth_(isNaN(v2))) {
        return null;
      } else {
        return { "pct": pct1, "value": v2 };
      }
      ;
    }
    ;
  };
  var to_pixels = function(parsed, total) {
    if (truth_(parsed.pct)) {
      return parsed.value / 100 * total;
    } else {
      return parsed.value;
    }
    ;
  };
  var measure = function(panel) {
    const total1 = group_extent(panel.group);
    if (truth_(panel.pct)) {
      return Math.round(panel.value / 100 * total1);
    } else {
      return panel.value;
    }
    ;
  };
  var current_extent = function(panel) {
    return panel.el.getBoundingClientRect()[extent_key(panel.group)];
  };
  var size_report = function(panel) {
    if (truth_(panel.pct)) {
      return `${panel.value ?? ""}${"%"}`;
    } else {
      return panel.value;
    }
    ;
  };
  var lock_state = { "count": 0, "saved": null };
  var lock_body_BANG_ = function(cursor, on_escape) {
    const style1 = document.body.style;
    const onkey2 = (function(e) {
      if (e.key === "Escape") {
        e.preventDefault();
        return on_escape();
      }
      ;
    });
    if (lock_state.count === 0) {
      lock_state["saved"] = { "cursor": style1.cursor, "userSelect": style1.userSelect, "webkitUserSelect": style1.webkitUserSelect };
    }
    ;
    lock_state["count"] = lock_state.count + 1;
    style1.cursor = cursor;
    style1.userSelect = "none";
    style1.webkitUserSelect = "none";
    addEventListener("keydown", onkey2, true);
    return function() {
      lock_state["count"] = lock_state.count - 1;
      if (lock_state.count === 0) {
        const saved3 = lock_state.saved;
        style1.cursor = saved3.cursor;
        style1.userSelect = saved3.userSelect;
        style1.webkitUserSelect = saved3.webkitUserSelect;
      }
      ;
      return removeEventListener("keydown", onkey2, true);
    };
  };
  var persist_BANG_ = function(panel) {
    if (truth_(panel.persist)) {
      return (() => {
        try {
          return localStorage.setItem(`${"ui-panels:"}${panel.persist ?? ""}`, JSON.stringify({ "size": size_report(panel), "collapsed": truth_(panel.collapsed) ? true : false }));
        } catch (_1) {
          return null;
        }
      })();
    }
    ;
  };
  var restore_persisted = function(key) {
    return (() => {
      try {
        const temp__23062__auto__1 = localStorage.getItem(`${"ui-panels:"}${key ?? ""}`);
        if (truth_(temp__23062__auto__1)) {
          const raw2 = temp__23062__auto__1;
          return JSON.parse(raw2);
        }
        ;
      } catch (_3) {
        return null;
      }
    })();
  };
  var emit_BANG_3 = function(panel, type, detail) {
    return panel.el.dispatchEvent(new CustomEvent(type, { "bubbles": true, "detail": detail }));
  };
  var emit_resize_BANG_ = function(panel) {
    return emit_BANG_3(panel, "ui-panels-resize", { "size": size_report(panel), "pixels": measure(panel) });
  };
  var reflect_collapsed_BANG_ = function(panel) {
    if (truth_(panel.collapsed)) {
      return panel.el.setAttribute("data-collapsed", "true");
    } else {
      return panel.el.removeAttribute("data-collapsed");
    }
    ;
  };
  var jump_size_BANG_ = function(panel, px2) {
    return panel.el.style[extent_key(panel.group)] = `${px2 ?? ""}px`;
  };
  var jump_content_BANG_ = function(panel, px2) {
    const temp__23062__auto__1 = panel.content;
    if (truth_(temp__23062__auto__1)) {
      const content2 = temp__23062__auto__1;
      return content2.style[extent_key(panel.group)] = `${px2 ?? ""}px`;
    }
    ;
  };
  var available = function(panel) {
    const group1 = panel.group;
    const k2 = extent_key(group1);
    const fill3 = group1.fill;
    const base4 = !(fill3 == null) ? parseFloat(getComputedStyle(fill3)[k2]) : 0;
    let i5 = 0;
    let room6 = base4;
    while (true) {
      if (i5 < group1.panels.length) {
        const p7 = group1.panels[i5];
        let G__8 = i5 + 1;
        let G__9 = room6 + (current_extent(p7) - (p7 === panel ? 0 : p7.target));
        i5 = G__8;
        room6 = G__9;
        continue;
      } else {
        return Math.max(0, room6);
      }
      ;
      ;
      break;
    }
    ;
  };
  var bounds = function(panel) {
    const room1 = available(panel);
    const total2 = group_extent(panel.group);
    const minv3 = Math.min(!(panel.minSize == null) ? to_pixels(panel.minSize, total2) : 0, room1);
    const maxv4 = !(panel.maxSize == null) ? to_pixels(panel.maxSize, total2) : room1;
    return { "min": minv3, "max": Math.max(minv3, Math.min(maxv4, room1)) };
  };
  var grow_sign = function(panel) {
    if (truth_(panel.end)) {
      return 1;
    } else {
      return -1;
    }
    ;
  };
  var sync_aria_BANG_ = function(panel) {
    const b1 = bounds(panel);
    const t2 = Math.round(panel.target);
    return panel.grips.forEach((function(grip) {
      grip.setAttribute("aria-valuenow", `${t2 ?? ""}`);
      grip.setAttribute("aria-valuetext", `${t2 ?? ""}${" pixels"}`);
      grip.setAttribute("aria-valuemin", `${Math.round(b1.min) ?? ""}`);
      if (!(panel.maxSize == null)) {
        return grip.setAttribute("aria-valuemax", `${Math.round(b1.max) ?? ""}`);
      } else {
        return grip.removeAttribute("aria-valuemax");
      }
      ;
    }));
  };
  var any_folding_QMARK_ = function(group) {
    return group.panels.some((function(p) {
      if (truth_(p.folding)) {
        return true;
      } else {
        return false;
      }
      ;
    }));
  };
  var freeze_fill_BANG_ = function(panel) {
    const group1 = panel.group;
    const fill2 = group1.fill;
    const inner3 = group1.fillInner;
    if (!(inner3 == null)) {
      fill2.style.justifyContent = truth_(panel.end) ? "flex-end" : "flex-start";
      fill2.style.overflow = "clip";
      return inner3.style[extent_key(group1)] = `${Math.max(0, available(panel) - panel.target) ?? ""}px`;
    }
    ;
  };
  var set_folding_BANG_ = function(panel, folding) {
    if (!_EQ_(truth_(panel.folding) ? true : false, truth_(folding) ? true : false)) {
      panel["folding"] = folding;
      if (truth_(folding)) {
        panel.el.setAttribute("data-folding", "true");
      } else {
        panel.el.removeAttribute("data-folding");
      }
      ;
      if (truth_(folding)) {
        return null;
      } else {
        const group1 = panel.group;
        const fill2 = group1.fill;
        const inner3 = group1.fillInner;
        if (truth_(!(inner3 == null) && not(any_folding_QMARK_(group1)))) {
          inner3.style[extent_key(group1)] = "100%";
          return fill2.style.overflow = "";
        }
        ;
      }
      ;
    }
    ;
  };
  var fold_BANG_ = function(panel, to) {
    const el1 = panel.el;
    const k2 = extent_key(panel.group);
    const from3 = current_extent(panel);
    set_folding_BANG_(panel, true);
    freeze_fill_BANG_(panel);
    const temp__23062__auto__4 = panel.anim;
    if (truth_(temp__23062__auto__4)) {
      const anim5 = temp__23062__auto__4;
      anim5.cancel();
    }
    ;
    jump_size_BANG_(panel, to);
    const content_from6 = truth_(to > 0 && from3 > 0) ? (() => {
      const temp__23062__auto__7 = panel.content;
      if (truth_(temp__23062__auto__7)) {
        const c8 = temp__23062__auto__7;
        return c8.getBoundingClientRect()[k2];
      }
      ;
    })() : null;
    if (to > 0) {
      jump_content_BANG_(panel, to);
    }
    ;
    if (truth_((() => {
      const or__23426__auto__9 = reduced_motion_QMARK_3();
      if (truth_(or__23426__auto__9)) {
        return or__23426__auto__9;
      } else {
        return _EQ_(from3, to);
      }
      ;
    })())) {
      return set_folding_BANG_(panel, false);
    } else {
      const kf10 = {};
      const _11 = kf10[k2] = [`${from3 ?? ""}px`, `${to ?? ""}px`];
      const anim12 = el1.animate(kf10, { "duration": duration, "easing": easing });
      panel["anim"] = anim12;
      if (truth_(!(content_from6 == null) && (!(panel.content == null) && !_EQ_(content_from6, to)))) {
        const ckf13 = {};
        ckf13[k2] = [`${content_from6 ?? ""}px`, `${to ?? ""}px`];
        panel.content.animate(ckf13, { "duration": duration, "easing": easing });
      }
      ;
      anim12.onfinish = (function() {
        panel["anim"] = null;
        return set_folding_BANG_(panel, false);
      });
      return anim12.oncancel = (function() {
        return panel["anim"] = null;
      });
    }
    ;
  };
  var retarget_BANG_ = function(panel) {
    const m1 = measure(panel);
    const value2 = truth_(panel.collapsed) ? 0 : m1;
    if (truth_(current_extent(panel) === 0 && not(panel.dragging))) {
      jump_content_BANG_(panel, m1);
    }
    ;
    if (!_EQ_(value2, panel.target)) {
      panel["target"] = value2;
      if (truth_(panel.dragging)) {
      } else {
        if (_EQ_(current_extent(panel), value2)) {
          set_folding_BANG_(panel, false);
          jump_size_BANG_(panel, value2);
          if (value2 > 0) {
            jump_content_BANG_(panel, m1);
          }
        } else {
          fold_BANG_(panel, value2);
        }
      }
    }
    ;
    return sync_aria_BANG_(panel);
  };
  var set_collapsed_BANG_ = function(panel, collapsed) {
    if (!_EQ_(truth_(collapsed) ? true : false, truth_(panel.collapsed) ? true : false)) {
      panel["collapsed"] = collapsed;
      reflect_collapsed_BANG_(panel);
      retarget_BANG_(panel);
      emit_BANG_3(panel, "ui-panels-collapse", { "collapsed": truth_(collapsed) ? true : false });
      return persist_BANG_(panel);
    }
    ;
  };
  var apply_size_BANG_ = function(panel, px2) {
    const b1 = bounds(panel);
    const v2 = clamp4(px2, b1.min, b1.max);
    const total3 = group_extent(panel.group);
    if (truth_((() => {
      const and__23442__auto__4 = panel.collapsed;
      if (truth_(and__23442__auto__4)) {
        return v2 > 0;
      } else {
        return and__23442__auto__4;
      }
      ;
    })())) {
      panel["collapsed"] = false;
      reflect_collapsed_BANG_(panel);
      emit_BANG_3(panel, "ui-panels-collapse", { "collapsed": false });
    }
    ;
    if (truth_(panel.pct)) {
      panel["value"] = total3 > 0 ? round2(100 * (v2 / total3)) : 0;
    } else {
      panel["value"] = v2;
    }
    ;
    retarget_BANG_(panel);
    emit_resize_BANG_(panel);
    return persist_BANG_(panel);
  };
  var release_BANG_ = function(panel) {
    const temp__23062__auto__1 = panel.unlock;
    if (truth_(temp__23062__auto__1)) {
      const unlock2 = temp__23062__auto__1;
      unlock2();
      return panel["unlock"] = null;
    }
    ;
  };
  var set_resizing_attrs_BANG_ = function(panel, on) {
    const f1 = (function(el2) {
      if (truth_(on)) {
        return el2.setAttribute("data-resizing", "true");
      } else {
        return el2.removeAttribute("data-resizing");
      }
      ;
    });
    f1(panel.el);
    return panel.grips.forEach(f1);
  };
  var stop_dragging_BANG_ = function(panel) {
    release_BANG_(panel);
    panel["dragging"] = false;
    return set_resizing_attrs_BANG_(panel, false);
  };
  var drag_collapse_BANG_ = function(panel, collapsed) {
    panel["collapsed"] = collapsed;
    reflect_collapsed_BANG_(panel);
    panel["target"] = truth_(collapsed) ? 0 : measure(panel);
    return emit_BANG_3(panel, "ui-panels-collapse", { "collapsed": truth_(collapsed) ? true : false });
  };
  var drag_cancel_BANG_ = function(panel) {
    if (truth_(panel.dragging)) {
      const s1 = panel.session;
      jump_size_BANG_(panel, s1.start);
      jump_content_BANG_(panel, s1.start > 0 ? s1.start : measure(panel));
      if (!_EQ_(truth_(s1.sessionCollapsed) ? true : false, truth_(s1.wasCollapsed) ? true : false)) {
        drag_collapse_BANG_(panel, s1.wasCollapsed);
      }
      ;
      return stop_dragging_BANG_(panel);
    }
    ;
  };
  var drag_start_BANG_ = function(panel, cursor) {
    const b1 = bounds(panel);
    const s2 = panel.session;
    const group3 = panel.group;
    const cur4 = current_extent(panel);
    const temp__23062__auto__5 = panel.anim;
    if (truth_(temp__23062__auto__5)) {
      const anim6 = temp__23062__auto__5;
      anim6.cancel();
      jump_size_BANG_(panel, cur4);
    }
    ;
    s2["min"] = b1.min;
    s2["max"] = b1.max;
    s2["sign"] = grow_sign(panel);
    s2["start"] = cur4;
    s2["sessionCollapsed"] = truth_(panel.collapsed) ? true : false;
    s2["wasCollapsed"] = truth_(panel.collapsed) ? true : false;
    release_BANG_(panel);
    panel["unlock"] = lock_body_BANG_(!(cursor == null) ? cursor : group3.axes["cursor"], (function() {
      return drag_cancel_BANG_(panel);
    }));
    set_folding_BANG_(panel, false);
    panel["dragging"] = true;
    return set_resizing_attrs_BANG_(panel, true);
  };
  var drag_move_BANG_ = function(panel, dx, dy) {
    if (truth_(panel.dragging)) {
      const s1 = panel.session;
      const group2 = panel.group;
      const offset3 = extent_key(group2) === "width" ? dx : dy;
      const pixels4 = s1.start + offset3 * s1.sign;
      const collapse5 = (() => {
        const and__23442__auto__6 = panel.collapsible;
        if (truth_(and__23442__auto__6)) {
          return pixels4 < s1.min / 2;
        } else {
          return and__23442__auto__6;
        }
        ;
      })();
      const next7 = truth_(collapse5) ? 0 : Math.round(clamp4(pixels4, s1.min, s1.max));
      if (!_EQ_(truth_(collapse5) ? true : false, truth_(s1.sessionCollapsed) ? true : false)) {
        drag_collapse_BANG_(panel, collapse5);
      }
      ;
      jump_size_BANG_(panel, next7);
      if (truth_(collapse5)) {
      } else {
        jump_content_BANG_(panel, next7);
      }
      ;
      return s1["sessionCollapsed"] = collapse5;
    }
    ;
  };
  var drag_end_BANG_ = function(panel) {
    if (truth_(panel.dragging)) {
      const s1 = panel.session;
      stop_dragging_BANG_(panel);
      if (truth_(s1.sessionCollapsed)) {
        persist_BANG_(panel);
      } else {
        const px2 = current_extent(panel);
        const total3 = group_extent(panel.group);
        if (truth_(panel.pct)) {
          panel["value"] = total3 > 0 ? round2(100 * (px2 / total3)) : 0;
        } else {
          panel["value"] = px2;
        }
        ;
        panel["target"] = measure(panel);
        emit_resize_BANG_(panel);
        persist_BANG_(panel);
      }
      ;
      const cur4 = current_extent(panel);
      if (!_EQ_(cur4, panel.target)) {
        fold_BANG_(panel, panel.target);
      }
      ;
      return sync_aria_BANG_(panel);
    }
    ;
  };
  var resize_by_key_BANG_ = function(panel, e) {
    const key1 = e.key;
    const axes2 = panel.group.axes;
    if (key1 === "Enter") {
      if (truth_(panel.collapsible)) {
        e.preventDefault();
        return set_collapsed_BANG_(panel, not(panel.collapsed));
      }
    } else {
      const b3 = bounds(panel);
      const fast4 = (() => {
        const or__23426__auto__5 = e.shiftKey;
        if (truth_(or__23426__auto__5)) {
          return or__23426__auto__5;
        } else {
          const or__23426__auto__6 = key1 === "PageUp";
          if (or__23426__auto__6) {
            return or__23426__auto__6;
          } else {
            return key1 === "PageDown";
          }
          ;
        }
        ;
      })();
      const step7 = (truth_(fast4) ? key_step_fast : key_step) * grow_sign(panel);
      const t8 = panel.target;
      const next9 = key1 === "End" ? b3.max : key1 === "Home" ? b3.min : key1 === "PageDown" ? t8 + step7 : key1 === "PageUp" ? t8 - step7 : _EQ_(key1, axes2["grow"]) ? t8 + step7 : _EQ_(key1, axes2["shrink"]) ? t8 - step7 : "else" ? null : null;
      if (!(next9 == null)) {
        e.preventDefault();
        return apply_size_BANG_(panel, clamp4(next9, b3.min, b3.max));
      }
      ;
    }
    ;
  };
  var reset_size_BANG_ = function(panel) {
    if (truth_(panel.collapsed)) {
      set_collapsed_BANG_(panel, false);
    }
    ;
    const total1 = group_extent(panel.group);
    const parsed2 = !(panel.defaultSize == null) ? panel.defaultSize : panel.initial;
    if (!(parsed2 == null)) {
      return apply_size_BANG_(panel, to_pixels(parsed2, total1));
    }
    ;
  };
  var attach_grip_BANG_ = function(grip, get_panel) {
    const state1 = { "pressed": null, "dragging": false, "dragged": false };
    const settle2 = (function() {
      state1["pressed"] = null;
      return state1["dragging"] = false;
    });
    grip.addEventListener("pointerdown", (function(e) {
      if (!(get_panel() == null)) {
        state1["dragged"] = false;
        state1["pressed"] = { "x": e.clientX, "y": e.clientY };
        return grip.setPointerCapture(e.pointerId);
      }
      ;
    }));
    grip.addEventListener("pointermove", (function(e) {
      const temp__23062__auto__3 = state1.pressed;
      if (truth_(temp__23062__auto__3)) {
        const pr4 = temp__23062__auto__3;
        const temp__23062__auto__5 = get_panel();
        if (truth_(temp__23062__auto__5)) {
          const p6 = temp__23062__auto__5;
          const dx7 = e.clientX - pr4.x;
          const dy8 = e.clientY - pr4.y;
          if (truth_(not(state1.dragging) && Math.hypot(dx7, dy8) >= pan_threshold)) {
            state1["dragging"] = true;
            state1["dragged"] = true;
            drag_start_BANG_(p6, null);
          }
          ;
          if (truth_(state1.dragging)) {
            return drag_move_BANG_(p6, dx7, dy8);
          }
          ;
        }
        ;
      }
      ;
    }));
    grip.addEventListener("pointerup", (function(_) {
      if (truth_(state1.dragging)) {
        const temp__23062__auto__9 = get_panel();
        if (truth_(temp__23062__auto__9)) {
          const p10 = temp__23062__auto__9;
          drag_end_BANG_(p10);
        }
      }
      ;
      return settle2();
    }));
    grip.addEventListener("pointercancel", (function(_) {
      if (truth_(state1.dragging)) {
        const temp__23062__auto__11 = get_panel();
        if (truth_(temp__23062__auto__11)) {
          const p12 = temp__23062__auto__11;
          drag_cancel_BANG_(p12);
        }
      }
      ;
      return settle2();
    }));
    grip.addEventListener("dblclick", (function(_) {
      if (truth_(state1.dragged)) {
        return null;
      } else {
        const temp__23062__auto__13 = get_panel();
        if (truth_(temp__23062__auto__13)) {
          const p14 = temp__23062__auto__13;
          return reset_size_BANG_(p14);
        }
        ;
      }
      ;
    }));
    return grip.addEventListener("keydown", (function(e) {
      const temp__23062__auto__15 = get_panel();
      if (truth_(temp__23062__auto__15)) {
        const p16 = temp__23062__auto__15;
        return resize_by_key_BANG_(p16, e);
      }
      ;
    }));
  };
  var separator_panel = function(group, slot) {
    return group.bySide[truth_(fill_after_QMARK_(slot)) ? "start" : "end"];
  };
  var place_all_BANG_ = function(group) {
    const gel1 = group.el;
    const children2 = Array.from(gel1.children);
    const fill3 = children2.find((function(c) {
      return fill_node_QMARK_(c);
    }));
    const by_side4 = {};
    group["fill"] = !(fill3 == null) ? fill3 : null;
    group["fillInner"] = truth_(!(fill3 == null) && fill3.hasAttribute("data-pin")) ? fill3.querySelector(":scope > .ui-panels-fill-inner") : null;
    group.panels.forEach((function(p) {
      const el5 = p.el;
      const end6 = fill_after_QMARK_(el5);
      const neighbour7 = truth_(end6) ? el5.nextElementSibling : el5.previousElementSibling;
      const bare8 = not(separator_node_QMARK_(neighbour7));
      p["end"] = end6;
      el5.setAttribute("data-edge", truth_(end6) ? "end" : "start");
      if (bare8) {
        el5.setAttribute("data-bare", "true");
      } else {
        el5.removeAttribute("data-bare");
      }
      ;
      by_side4[truth_(end6) ? "start" : "end"] = p;
      return p["grips"] = !(p.edge == null) ? [p.edge] : [];
    }));
    group["bySide"] = by_side4;
    children2.forEach((function(c) {
      if (truth_(separator_node_QMARK_(c))) {
        const temp__23062__auto__9 = c["__uiSepGrip"];
        if (truth_(temp__23062__auto__9)) {
          const grip10 = temp__23062__auto__9;
          const temp__23062__auto__11 = separator_panel(group, c);
          if (truth_(temp__23062__auto__11)) {
            const p12 = temp__23062__auto__11;
            return p12.grips.push(grip10);
          }
          ;
        }
        ;
      }
      ;
    }));
    return group.panels.forEach((function(p) {
      return sync_aria_BANG_(p);
    }));
  };
  var observe_panel_attrs_BANG_ = function(panel) {
    const el1 = panel.el;
    const mo2 = new MutationObserver((function(muts) {
      return muts.forEach((function(m) {
        const attr3 = m.attributeName;
        if (attr3 === "data-collapsed") {
          const want4 = el1.hasAttribute("data-collapsed");
          if (!_EQ_(truth_(want4) ? true : false, truth_(panel.collapsed) ? true : false)) {
            return set_collapsed_BANG_(panel, want4);
          }
          ;
        } else {
          if (attr3 === "data-size") {
            const temp__23062__auto__5 = parse_size(el1.getAttribute("data-size"));
            if (truth_(temp__23062__auto__5)) {
              const parsed6 = temp__23062__auto__5;
              if (truth_((() => {
                const or__23426__auto__7 = !_EQ_(parsed6.value, panel.value);
                if (or__23426__auto__7) {
                  return or__23426__auto__7;
                } else {
                  return !_EQ_(truth_(parsed6.pct) ? true : false, truth_(panel.pct) ? true : false);
                }
                ;
              })())) {
                panel["pct"] = parsed6.pct;
                panel["value"] = parsed6.value;
                return retarget_BANG_(panel);
              }
              ;
            }
            ;
          } else {
            return null;
          }
        }
        ;
      }));
    }));
    mo2.observe(el1, { "attributes": true, "attributeFilter": ["data-collapsed", "data-size"] });
    return panel["attrObserver"] = mo2;
  };
  var create_panel_BANG_ = function(group, el2) {
    const content1 = el2.querySelector(":scope > .ui-panels-content");
    const edge22 = el2.querySelector(":scope > .ui-panels-edge");
    const parsed3 = parse_size(el2.getAttribute("data-size"));
    const persist_key4 = el2.getAttribute("data-persist-key");
    const saved5 = !(persist_key4 == null) ? restore_persisted(persist_key4) : null;
    const saved_size6 = !(saved5 == null) ? parse_size(`${saved5.size ?? ""}`) : null;
    const active7 = !(saved_size6 == null) ? saved_size6 : parsed3;
    const collapsed8 = !(saved5 == null) ? truth_(saved5.collapsed) ? true : false : el2.hasAttribute("data-collapsed");
    const panel9 = { "folding": false, "edge": edge22, "el": el2, "group": group, "collapsed": collapsed8, "content": content1, "collapsible": el2.hasAttribute("data-collapsible"), "value": !(active7 == null) ? active7.value : 0, "grips": [], "persist": persist_key4, "dragging": false, "minSize": parse_size(el2.getAttribute("data-min-size")), "pct": !(active7 == null) ? active7.pct : false, "anim": null, "initial": parsed3, "unlock": null, "defaultSize": parse_size(el2.getAttribute("data-default-size")), "target": 0, "end": false, "maxSize": parse_size(el2.getAttribute("data-max-size")), "session": {} };
    el2["__uiPanel"] = panel9;
    group.panels.push(panel9);
    const m10 = measure(panel9);
    const t11 = truth_(collapsed8) ? 0 : m10;
    panel9["target"] = t11;
    reflect_collapsed_BANG_(panel9);
    jump_size_BANG_(panel9, t11);
    jump_content_BANG_(panel9, m10);
    if (!(edge22 == null)) {
      edge22.setAttribute("aria-orientation", group.axes["sepOrient"]);
      attach_grip_BANG_(edge22, (function() {
        return panel9;
      }));
    }
    ;
    observe_panel_attrs_BANG_(panel9);
    return panel9;
  };
  var attach_separator_BANG_ = function(group, slot) {
    const grip1 = slot.querySelector(":scope > .ui-panels-grip");
    const grip2 = !(grip1 == null) ? grip1 : slot;
    slot["__uiSepGrip"] = grip2;
    grip2.setAttribute("aria-orientation", group.axes["sepOrient"]);
    return attach_grip_BANG_(grip2, (function() {
      return separator_panel(group, slot);
    }));
  };
  var bind_children_BANG_ = function(group) {
    return Array.from(group.el.children).forEach((function(c) {
      if (truth_((() => {
        const and__23442__auto__1 = has_attr_QMARK_(c, "data-ui-panels-panel");
        if (truth_(and__23442__auto__1)) {
          return c["__uiPanel"] == null;
        } else {
          return and__23442__auto__1;
        }
        ;
      })())) {
        return create_panel_BANG_(group, c);
      } else {
        if (truth_((() => {
          const and__23442__auto__2 = separator_node_QMARK_(c);
          if (truth_(and__23442__auto__2)) {
            return c["__uiSepGrip"] == null;
          } else {
            return and__23442__auto__2;
          }
          ;
        })())) {
          return attach_separator_BANG_(group, c);
        } else {
          return null;
        }
      }
      ;
    }));
  };
  var prune_panels_BANG_ = function(group) {
    const gel1 = group.el;
    const kept2 = group.panels.filter((function(p) {
      return p.el.parentElement === gel1;
    }));
    group.panels.forEach((function(p) {
      if (p.el.parentElement === gel1) {
        return null;
      } else {
        release_BANG_(p);
        const temp__23062__auto__3 = p.attrObserver;
        if (truth_(temp__23062__auto__3)) {
          const mo4 = temp__23062__auto__3;
          return mo4.disconnect();
        }
        ;
      }
      ;
    }));
    return group["panels"] = kept2;
  };
  var refit_BANG_ = function(panel) {
    if (truth_((() => {
      const and__23442__auto__1 = panel.pct;
      if (truth_(and__23442__auto__1)) {
        return not(panel.dragging) && not(panel.folding);
      } else {
        return and__23442__auto__1;
      }
      ;
    })())) {
      const m2 = measure(panel);
      const v3 = truth_(panel.collapsed) ? 0 : m2;
      jump_content_BANG_(panel, m2);
      if (!_EQ_(v3, panel.target)) {
        panel["target"] = v3;
        jump_size_BANG_(panel, v3);
        return sync_aria_BANG_(panel);
      }
      ;
    }
    ;
  };
  var init_group_BANG_ = function(gel) {
    if (gel["__uiPanelsGroup"] == null) {
      const orientation1 = (() => {
        const o2 = gel.getAttribute("data-orientation");
        if (o2 === "vertical") {
          return "vertical";
        } else {
          return "horizontal";
        }
        ;
      })();
      const group3 = { "el": gel, "axes": axes_config[orientation1], "panels": [], "bySide": {}, "fill": null, "fillInner": null };
      gel["__uiPanelsGroup"] = group3;
      bind_children_BANG_(group3);
      place_all_BANG_(group3);
      const mo4 = new MutationObserver((function(_) {
        prune_panels_BANG_(group3);
        bind_children_BANG_(group3);
        return place_all_BANG_(group3);
      }));
      mo4.observe(gel, { "childList": true });
      const ro5 = new ResizeObserver((function(_) {
        return group3.panels.forEach((function(p) {
          return refit_BANG_(p);
        }));
      }));
      ro5.observe(gel);
      return group3;
    }
    ;
  };
  var scan_BANG_2 = function() {
    return Array.from(document.querySelectorAll("[data-ui-panels-group]")).forEach((function(gel) {
      return init_group_BANG_(gel);
    }));
  };
  var scan_scheduled = atom(null);
  var schedule_scan_BANG_ = function() {
    if (deref(scan_scheduled) == null) {
      return reset_BANG_(scan_scheduled, requestAnimationFrame((function() {
        reset_BANG_(scan_scheduled, null);
        return scan_BANG_2();
      })));
    }
    ;
  };
  window["__uiPanels"] = scan_BANG_2;
  var start_BANG_ = function() {
    scan_BANG_2();
    const mo1 = new MutationObserver((function(_) {
      return schedule_scan_BANG_();
    }));
    return mo1.observe(document.body, { "childList": true, "subtree": true });
  };
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", (function() {
      return start_BANG_();
    }));
  } else {
    start_BANG_();
  }

  // .compiled/popover.mjs
  var gap = 8;
  var edge = 8;
  var clamp5 = function(v, lo, hi) {
    return Math.max(lo, Math.min(v, hi));
  };
  var align_h = function(tr, cw, align) {
    if (align === "start") {
      return tr.left;
    } else {
      if (align === "end") {
        return tr.right - cw;
      } else {
        if ("else") {
          return tr.left + (tr.width - cw) / 2;
        } else {
          return null;
        }
      }
    }
    ;
  };
  var align_v = function(tr, ch, align) {
    if (align === "start") {
      return tr.top;
    } else {
      if (align === "end") {
        return tr.bottom - ch;
      } else {
        if ("else") {
          return tr.top + (tr.height - ch) / 2;
        } else {
          return null;
        }
      }
    }
    ;
  };
  var position_BANG_ = function(content, trigger) {
    const side1 = (() => {
      const or__23426__auto__2 = content.dataset.popoverSide;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return "bottom";
      }
      ;
    })();
    const align3 = (() => {
      const or__23426__auto__4 = content.dataset.popoverAlign;
      if (truth_(or__23426__auto__4)) {
        return or__23426__auto__4;
      } else {
        return "center";
      }
      ;
    })();
    const tr5 = trigger.getBoundingClientRect();
    const cr6 = content.getBoundingClientRect();
    const cw7 = cr6.width;
    const ch8 = cr6.height;
    const vw9 = window.innerWidth;
    const vh10 = window.innerHeight;
    const left11 = side1 === "left" ? tr5.left - cw7 - gap : side1 === "right" ? tr5.right + gap : "else" ? align_h(tr5, cw7, align3) : null;
    const top12 = side1 === "top" ? tr5.top - ch8 - gap : side1 === "bottom" ? tr5.bottom + gap : "else" ? align_v(tr5, ch8, align3) : null;
    content.style.left = `${clamp5(left11, edge, vw9 - cw7 - edge) ?? ""}px`;
    return content.style.top = `${clamp5(top12, edge, vh10 - ch8 - edge) ?? ""}px`;
  };
  var current = { "content": null, "trigger": null };
  var reposition_BANG_ = function() {
    if (truth_(current.content)) {
      return position_BANG_(current.content, current.trigger);
    }
    ;
  };
  var on_toggle = function(e) {
    const content1 = e.target;
    if (truth_((() => {
      const and__23442__auto__2 = content1;
      if (truth_(and__23442__auto__2)) {
        const and__23442__auto__3 = content1.matches;
        if (truth_(and__23442__auto__3)) {
          return content1.matches("[popover].popover-content");
        } else {
          return and__23442__auto__3;
        }
        ;
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      if (e.newState === "open") {
        const id4 = content1.id;
        const trigger5 = truth_(id4) ? document.querySelector(`${'[popovertarget="'}${id4 ?? ""}${'"]'}`) : null;
        if (truth_(trigger5)) {
          current.content = content1;
          current.trigger = trigger5;
          return position_BANG_(content1, trigger5);
        }
        ;
      } else {
        if (current.content === content1) {
          current.content = null;
          return current.trigger = null;
        }
      }
      ;
    }
    ;
  };
  var init_BANG_7 = function() {
    document.addEventListener("toggle", on_toggle, true);
    window.addEventListener("scroll", reposition_BANG_, true);
    return window.addEventListener("resize", reposition_BANG_);
  };
  init_BANG_7();
  window["__uiPopover"] = { "reposition": reposition_BANG_ };

  // .compiled/rating.mjs
  var pressed = atom(null);
  var closest2 = function(el2, sel) {
    if (truth_((() => {
      const and__23442__auto__1 = el2;
      if (truth_(and__23442__auto__1)) {
        return el2.closest;
      } else {
        return and__23442__auto__1;
      }
      ;
    })())) {
      return el2.closest(sel);
    }
    ;
  };
  var root_of = function(el2) {
    return closest2(el2, "[data-ui-rating]");
  };
  var disabled_QMARK_2 = function(root) {
    return root.classList.contains("rating--disabled");
  };
  var commit_BANG_3 = function(root, value) {
    root.setAttribute("data-value", value);
    root.classList.remove("rating--committed");
    if (!(value === "0")) {
      root.offsetWidth;
      return root.classList.add("rating--committed");
    }
    ;
  };
  var clear_BANG_2 = function(root) {
    const temp__23062__auto__1 = root.querySelector(".rating-input-none");
    if (truth_(temp__23062__auto__1)) {
      const none2 = temp__23062__auto__1;
      none2.checked = true;
      none2.dispatchEvent(new Event("input", { "bubbles": true }));
      return none2.dispatchEvent(new Event("change", { "bubbles": true }));
    }
    ;
  };
  var on_pointerdown4 = function(e) {
    return reset_BANG_(pressed, (() => {
      const temp__23062__auto__1 = closest2(e.target, ".rating-hit");
      if (truth_(temp__23062__auto__1)) {
        const hit2 = temp__23062__auto__1;
        const input3 = hit2.querySelector(".rating-input");
        if (truth_((() => {
          const and__23442__auto__4 = input3;
          if (truth_(and__23442__auto__4)) {
            return input3.checked;
          } else {
            return and__23442__auto__4;
          }
          ;
        })())) {
          return input3;
        }
        ;
      }
      ;
    })());
  };
  var on_click5 = function(e) {
    const temp__23062__auto__1 = closest2(e.target, ".rating-hit");
    if (truth_(temp__23062__auto__1)) {
      const hit2 = temp__23062__auto__1;
      const root3 = root_of(hit2);
      const input4 = hit2.querySelector(".rating-input");
      if (truth_((() => {
        const and__23442__auto__5 = root3;
        if (truth_(and__23442__auto__5)) {
          const and__23442__auto__6 = input4;
          if (truth_(and__23442__auto__6)) {
            return not(disabled_QMARK_2(root3)) && _EQ_(input4, deref(pressed));
          } else {
            return and__23442__auto__6;
          }
          ;
        } else {
          return and__23442__auto__5;
        }
        ;
      })())) {
        e.preventDefault();
        reset_BANG_(pressed, null);
        return clear_BANG_2(root3);
      }
      ;
    }
    ;
  };
  var on_change3 = function(e) {
    const t1 = e.target;
    if (truth_((() => {
      const and__23442__auto__2 = t1;
      if (truth_(and__23442__auto__2)) {
        const and__23442__auto__3 = t1.classList;
        if (truth_(and__23442__auto__3)) {
          return t1.classList.contains("rating-input");
        } else {
          return and__23442__auto__3;
        }
        ;
      } else {
        return and__23442__auto__2;
      }
      ;
    })())) {
      const temp__23062__auto__4 = root_of(t1);
      if (truth_(temp__23062__auto__4)) {
        const root5 = temp__23062__auto__4;
        return commit_BANG_3(root5, t1.value);
      }
      ;
    }
    ;
  };
  var on_keydown4 = function(e) {
    const t1 = e.target;
    const k2 = e.key;
    if (truth_((() => {
      const and__23442__auto__3 = t1;
      if (truth_(and__23442__auto__3)) {
        const and__23442__auto__4 = t1.classList;
        if (truth_(and__23442__auto__4)) {
          const and__23442__auto__5 = t1.classList.contains("rating-input");
          if (truth_(and__23442__auto__5)) {
            const or__23426__auto__6 = k2 === "Backspace";
            if (or__23426__auto__6) {
              return or__23426__auto__6;
            } else {
              return k2 === "Delete";
            }
            ;
          } else {
            return and__23442__auto__5;
          }
          ;
        } else {
          return and__23442__auto__4;
        }
        ;
      } else {
        return and__23442__auto__3;
      }
      ;
    })())) {
      const temp__23062__auto__7 = root_of(t1);
      if (truth_(temp__23062__auto__7)) {
        const root8 = temp__23062__auto__7;
        if (truth_(disabled_QMARK_2(root8))) {
          return null;
        } else {
          e.preventDefault();
          return clear_BANG_2(root8);
        }
        ;
      }
      ;
    }
    ;
  };
  var on_animationend = function(e) {
    const t1 = e.target;
    const temp__23062__auto__2 = root_of(t1);
    if (truth_(temp__23062__auto__2)) {
      const root3 = temp__23062__auto__2;
      if (_EQ_(t1, (() => {
        const G__5494 = root3.querySelector(".rating-stars");
        if (G__5494 == null) {
          return null;
        } else {
          return G__5494.lastElementChild;
        }
        ;
      })())) {
        return root3.classList.remove("rating--committed");
      }
      ;
    }
    ;
  };
  var init_BANG_8 = function() {
    document.addEventListener("pointerdown", on_pointerdown4, true);
    document.addEventListener("click", on_click5, true);
    document.addEventListener("change", on_change3);
    document.addEventListener("keydown", on_keydown4);
    return document.addEventListener("animationend", on_animationend);
  };
  init_BANG_8();

  // .compiled/select.mjs
  var get_state2 = function() {
    const or__23426__auto__1 = window["__uiSelectState"];
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const s2 = { "menu": null, "trigger": null, "cleanup": null };
      window["__uiSelectState"] = s2;
      return s2;
    }
    ;
  };
  var dismiss_BANG_2 = function() {
    const state1 = get_state2();
    if (truth_(state1.menu)) {
      state1.menu.remove();
      state1.menu = null;
    }
    ;
    if (truth_(state1.trigger)) {
      state1.trigger.setAttribute("aria-expanded", "false");
      state1.trigger = null;
    }
    ;
    if (truth_(state1.cleanup)) {
      state1.cleanup();
      return state1.cleanup = null;
    }
    ;
  };
  var update_trigger_BANG_ = function(trigger, value, label) {
    trigger.dataset.selectValue = value;
    const span1 = trigger.querySelector(".select-value");
    if (truth_(span1)) {
      span1.textContent = label;
      span1.classList.remove("select-value--placeholder");
    }
    ;
    const wrap2 = trigger.parentElement;
    const input3 = truth_(wrap2) ? wrap2.querySelector("input[type=hidden]") : null;
    if (truth_(input3)) {
      input3.value = value;
      return input3.dispatchEvent(new Event("change", { "bubbles": true }));
    }
    ;
  };
  var create_menu2 = function(options, current_value, on_change4, trigger) {
    const menu1 = document.createElement("div");
    menu1.className = "select-menu";
    menu1.setAttribute("role", "listbox");
    options.forEach((function(opt) {
      const value2 = opt["value"];
      const label3 = opt["label"];
      const selected4 = _EQ_(value2, current_value);
      const el5 = document.createElement("button");
      el5.className = selected4 ? "select-option select-option--selected" : "select-option";
      el5.setAttribute("type", "button");
      el5.setAttribute("role", "option");
      el5.setAttribute("tabindex", "-1");
      el5.setAttribute("aria-selected", selected4 ? "true" : "false");
      el5.textContent = label3;
      el5.addEventListener("click", (function(e) {
        e.preventDefault();
        e.stopPropagation();
        if (truth_(on_change4)) {
          dismiss_BANG_2();
          return on_change4(value2);
        } else {
          update_trigger_BANG_(trigger, value2, label3);
          return dismiss_BANG_2();
        }
        ;
      }));
      return menu1.appendChild(el5);
    }));
    return menu1;
  };
  var position_menu_BANG_2 = function(menu, trigger) {
    const r1 = trigger.getBoundingClientRect();
    menu.style.minWidth = `${r1.width ?? ""}px`;
    document.body.appendChild(menu);
    const mr2 = menu.getBoundingClientRect();
    const vw3 = window.innerWidth;
    const vh4 = window.innerHeight;
    const left5 = Math.max(8, Math.min(r1.left, vw3 - mr2.width - 8));
    const below6 = r1.bottom + 4;
    const top7 = below6 + mr2.height > vh4 ? Math.max(8, r1.top - mr2.height - 4) : below6;
    menu.style.left = `${left5 ?? ""}px`;
    return menu.style.top = `${top7 ?? ""}px`;
  };
  var focus_selected_or_first_BANG_ = function(menu) {
    const sel1 = menu.querySelector(".select-option--selected");
    if (truth_(sel1)) {
      return sel1.focus();
    } else {
      const first_opt2 = menu.querySelector(".select-option");
      if (truth_(first_opt2)) {
        return first_opt2.focus();
      }
      ;
    }
    ;
  };
  var focus_next_BANG_2 = function(menu, direction) {
    const items1 = menu.querySelectorAll(".select-option");
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
  var open_select = (() => {
    const f550 = (function(var_args) {
      const args5511 = [];
      const len__23321__auto__2 = arguments.length;
      let i5523 = 0;
      while (true) {
        if (i5523 < len__23321__auto__2) {
          args5511.push(arguments[i5523]);
          let G__4 = i5523 + 1;
          i5523 = G__4;
          continue;
        }
        ;
        break;
      }
      ;
      const argseq__23513__auto__5 = 1 < args5511.length ? args5511.slice(1) : null;
      return f550.cljs$core$IFn$_invoke$arity$variadic(arguments[0], argseq__23513__auto__5);
    });
    f550.cljs$core$IFn$_invoke$arity$variadic = (function(trigger, args) {
      dismiss_BANG_2();
      const options6 = (() => {
        const passed7 = first(args);
        if (truth_(passed7)) {
          return passed7;
        } else {
          const json8 = trigger.dataset.selectOptions;
          if (truth_(json8)) {
            return JSON.parse(json8);
          }
          ;
        }
        ;
      })();
      const on_change9 = second(args);
      const current10 = trigger.dataset.selectValue;
      if (truth_(options6)) {
        const menu11 = create_menu2(options6, current10, on_change9, trigger);
        const state12 = get_state2();
        state12.menu = menu11;
        state12.trigger = trigger;
        trigger.setAttribute("aria-expanded", "true");
        position_menu_BANG_2(menu11, trigger);
        focus_selected_or_first_BANG_(menu11);
        const on_click13 = (function(e) {
          if (not(menu11.contains(e.target))) {
            e.preventDefault();
            e.stopPropagation();
            return dismiss_BANG_2();
          }
          ;
        });
        const on_key14 = (function(e) {
          const key15 = e.key;
          if (key15 === "Escape") {
            e.preventDefault();
            dismiss_BANG_2();
            return trigger.focus();
          } else {
            if (key15 === "ArrowDown") {
              e.preventDefault();
              return focus_next_BANG_2(menu11, "down");
            } else {
              if (key15 === "ArrowUp") {
                e.preventDefault();
                return focus_next_BANG_2(menu11, "up");
              } else {
                if (key15 === "Enter") {
                  const active16 = document.activeElement;
                  if (truth_((() => {
                    const and__23442__auto__17 = active16;
                    if (truth_(and__23442__auto__17)) {
                      return menu11.contains(active16);
                    } else {
                      return and__23442__auto__17;
                    }
                    ;
                  })())) {
                    e.preventDefault();
                    return active16.click();
                  }
                  ;
                } else {
                  if (key15 === "Tab") {
                    return dismiss_BANG_2();
                  } else {
                    return null;
                  }
                }
              }
            }
          }
          ;
        });
        const on_scroll18 = (function(_) {
          return dismiss_BANG_2();
        });
        const on_resize19 = (function(_) {
          return dismiss_BANG_2();
        });
        const cleanup20 = (function() {
          document.removeEventListener("click", on_click13, true);
          document.removeEventListener("keydown", on_key14, true);
          window.removeEventListener("scroll", on_scroll18, true);
          return window.removeEventListener("resize", on_resize19);
        });
        document.addEventListener("click", on_click13, true);
        document.addEventListener("keydown", on_key14, true);
        window.addEventListener("scroll", on_scroll18, true);
        window.addEventListener("resize", on_resize19);
        return state12.cleanup = cleanup20;
      }
      ;
    });
    f550.cljs$lang$maxFixedArity = 1;
    return f550;
  })();
  window["__uiSelect"] = open_select;

  // .compiled/theme.mjs
  var storage_key2 = "ui-theme";
  var get_stored = function() {
    return (() => {
      try {
        return localStorage.getItem(storage_key2);
      } catch (_e1) {
        return null;
      }
    })();
  };
  var store_BANG_ = function(mode) {
    return (() => {
      try {
        if (mode === "auto") {
          return localStorage.removeItem(storage_key2);
        } else {
          return localStorage.setItem(storage_key2, mode);
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
    const G__5541 = mode;
    switch (G__5541) {
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
    const G__5552 = mode;
    switch (G__5552) {
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
  var notify_BANG_2 = function(mode, effective) {
    const subs1 = deref(subscribers);
    return subs1.forEach((function(f) {
      return f({ "mode": mode, "effective": effective });
    }));
  };
  var url_mode = function() {
    const m1 = new URLSearchParams(location.search).get("theme");
    if (truth_((() => {
      const or__23426__auto__2 = m1 === "light";
      if (or__23426__auto__2) {
        return or__23426__auto__2;
      } else {
        return m1 === "dark";
      }
      ;
    })())) {
      return m1;
    }
    ;
  };
  var get_mode = function() {
    const or__23426__auto__1 = url_mode();
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const or__23426__auto__2 = get_stored();
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return "auto";
      }
      ;
    }
    ;
  };
  var get_effective = function() {
    return resolve_effective(get_mode());
  };
  var sync_url_mode_BANG_ = function(mode) {
    if (truth_(url_mode())) {
      const url1 = new URL(location.href);
      if (mode === "auto") {
        url1.searchParams.delete("theme");
      } else {
        url1.searchParams.set("theme", mode);
      }
      ;
      return history.replaceState(null, "", url1);
    }
    ;
  };
  var set_mode_BANG_ = function(mode) {
    const m1 = truth_(get(/* @__PURE__ */ new Set(["auto", "light", "dark"]), mode)) ? mode : "auto";
    store_BANG_(m1);
    sync_url_mode_BANG_(m1);
    apply_theme_BANG_(m1);
    return notify_BANG_2(m1, resolve_effective(m1));
  };
  var toggle_BANG_ = function() {
    const current1 = get_mode();
    const next_mode2 = (() => {
      const G__5563 = current1;
      switch (G__5563) {
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
    swap_BANG_(subscribers, (function(subs2) {
      return subs2.concat([f]);
    }));
    return function() {
      return swap_BANG_(subscribers, (function(subs2) {
        return subs2.filter((function(s) {
          return !_EQ_(s, f);
        }));
      }));
    };
  };
  var init_BANG_9 = function() {
    const mode1 = get_mode();
    apply_theme_BANG_(mode1);
    const mql2 = window.matchMedia("(prefers-color-scheme: dark)");
    return mql2.addEventListener("change", (function(_e) {
      if (get_mode() === "auto") {
        apply_theme_BANG_("auto");
        return notify_BANG_2("auto", resolve_effective("auto"));
      }
      ;
    }));
  };
  window["__uiTheme"] = { "init": init_BANG_9, "set": set_mode_BANG_, "get": get_mode, "effective": get_effective, "toggle": toggle_BANG_, "subscribe": subscribe_BANG_ };

  // .compiled/toast.mjs
  var container_id = "ui-toast-container";
  var ensure_container_BANG_ = function() {
    const or__23426__auto__1 = document.getElementById(container_id);
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const el2 = document.createElement("div");
      el2.id = container_id;
      el2.className = "toast-container";
      document.body.appendChild(el2);
      return el2;
    }
    ;
  };
  var dismiss_BANG_3 = function(el2) {
    if (truth_(el2["__uiToastDismissed"])) {
      return null;
    } else {
      el2["__uiToastDismissed"] = true;
      el2.classList.add("toast-leaving");
      return setTimeout((function() {
        return el2.remove();
      }), 300);
    }
    ;
  };
  var show_BANG_ = function(message, opts) {
    const opts1 = (() => {
      const or__23426__auto__2 = opts;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return {};
      }
      ;
    })();
    const variant3 = (() => {
      const or__23426__auto__4 = opts1["variant"];
      if (truth_(or__23426__auto__4)) {
        return or__23426__auto__4;
      } else {
        return "info";
      }
      ;
    })();
    const raw_dur5 = opts1["duration"];
    const duration6 = truth_(number_QMARK_(raw_dur5)) ? raw_dur5 : 5e3;
    const container7 = ensure_container_BANG_();
    const el8 = document.createElement("div");
    el8.className = `${"toast toast-"}${variant3 ?? ""}`;
    el8.textContent = `${message ?? ""}`;
    el8.setAttribute("role", "status");
    el8.addEventListener("click", (function(_) {
      return dismiss_BANG_3(el8);
    }));
    container7.appendChild(el8);
    if (duration6 > 0) {
      setTimeout((function() {
        return dismiss_BANG_3(el8);
      }), duration6);
    }
    ;
    return el8;
  };
  var consume_BANG_ = function(el2) {
    const msg1 = el2.getAttribute("data-ui-toast");
    const dur2 = el2.getAttribute("data-duration");
    el2.remove();
    return show_BANG_(msg1, { "variant": (() => {
      const or__23426__auto__3 = el2.getAttribute("data-variant");
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return "info";
      }
      ;
    })(), "duration": truth_(dur2) ? parseInt(dur2, 10) : null });
  };
  var scan_BANG_3 = function() {
    for (let G__1 of iterable(Array.from(document.querySelectorAll("[data-ui-toast]")))) {
      const el2 = G__1;
      consume_BANG_(el2);
    }
    return null;
  };
  var init_BANG_10 = function() {
    scan_BANG_3();
    const obs1 = new MutationObserver((function(_, _2) {
      return scan_BANG_3();
    }));
    return obs1.observe(document.body, { "childList": true, "subtree": true });
  };
  window["__uiToast"] = show_BANG_;
  if ("loading" === document.readyState) {
    document.addEventListener("DOMContentLoaded", init_BANG_10);
  } else {
    init_BANG_10();
  }

  // .compiled/tooltip.mjs
  var default_delay = 500;
  var skip_ms = 400;
  var close_ms = 80;
  var card_close_ms = 160;
  var gap2 = 6;
  var edge2 = 8;
  var focusable2 = "a[href],button,input,select,textarea,summary,[tabindex]";
  var interactive = "a[href],button,input,select,textarea,summary,label";
  var get_state3 = function() {
    const or__23426__auto__1 = window["__uiTooltipState"];
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      const s2 = { "open": null, "lastClose": -1e5, "pending": null, "bubble": null, "layer": null, "pointer": "mouse", "hideTimer": null, "showTimer": null, "suppressed": null, "observer": null };
      window["__uiTooltipState"] = s2;
      return s2;
    }
    ;
  };
  var now2 = function() {
    return performance.now();
  };
  var trigger_of = function(node) {
    if (truth_((() => {
      const and__23442__auto__1 = node;
      if (truth_(and__23442__auto__1)) {
        return node.closest;
      } else {
        return and__23442__auto__1;
      }
      ;
    })())) {
      const t2 = node.closest(".tooltip, [data-tooltip]");
      if (t2 == null) {
        return null;
      } else {
        if (truth_(t2.matches(".tooltip"))) {
          return t2;
        } else {
          if (_EQ_("", (() => {
            const or__23426__auto__3 = t2.getAttribute("data-tooltip");
            if (truth_(or__23426__auto__3)) {
              return or__23426__auto__3;
            } else {
              return "";
            }
            ;
          })().trim())) {
            return trigger_of(t2.parentElement);
          } else {
            if ("else") {
              return t2;
            } else {
              return null;
            }
          }
        }
      }
      ;
    }
    ;
  };
  var el = function(tag, cls, text) {
    const e1 = document.createElement(tag);
    if (truth_(cls)) {
      e1.className = cls;
    }
    ;
    if (truth_(text)) {
      e1.textContent = text;
    }
    ;
    return e1;
  };
  var attr_label = function(trigger) {
    const label1 = el("span", "tooltip-label", null);
    const caps2 = (() => {
      const or__23426__auto__3 = trigger.getAttribute("data-tooltip-shortcut");
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return "";
      }
      ;
    })().split(" ").filter((function(k) {
      return !_EQ_(k, "");
    }));
    label1.appendChild(el("span", null, trigger.getAttribute("data-tooltip")));
    if (caps2.length > 0) {
      const row4 = el("span", "tooltip-keys", null);
      caps2.forEach((function(k) {
        return row4.appendChild(el("kbd", "tooltip-key", k));
      }));
      label1.appendChild(row4);
    }
    ;
    return label1;
  };
  var content_of = function(trigger) {
    const frag1 = document.createDocumentFragment();
    const tpl2 = trigger.querySelector(":scope > template.tooltip-content");
    if (truth_(tpl2)) {
      const src3 = truth_((() => {
        const and__23442__auto__4 = tpl2.content;
        if (truth_(and__23442__auto__4)) {
          return tpl2.content.firstChild;
        } else {
          return and__23442__auto__4;
        }
        ;
      })()) ? tpl2.content : tpl2;
      src3.childNodes.forEach((function(n) {
        return frag1.appendChild(n.cloneNode(true));
      }));
    } else {
      frag1.appendChild(attr_label(trigger));
    }
    ;
    return frag1;
  };
  var page_dark_QMARK_ = function(trigger) {
    const host1 = trigger.closest("[data-theme]");
    const theme2 = truth_(host1) ? host1.getAttribute("data-theme") : null;
    if (theme2 === "dark") {
      return true;
    } else {
      if (theme2 === "light") {
        return false;
      } else {
        if ("else") {
          return matchMedia("(prefers-color-scheme: dark)").matches;
        } else {
          return null;
        }
      }
    }
    ;
  };
  var bubble_theme = function(trigger) {
    const host1 = trigger.closest("[data-tooltip-theme]");
    const explicit2 = truth_(host1) ? host1.dataset.tooltipTheme : null;
    const dark_QMARK_3 = page_dark_QMARK_(trigger);
    if (truth_((() => {
      const or__23426__auto__4 = explicit2 === "light";
      if (or__23426__auto__4) {
        return or__23426__auto__4;
      } else {
        return explicit2 === "dark";
      }
      ;
    })())) {
      return explicit2;
    } else {
      if (truth_(trigger.dataset.tooltipTone)) {
        if (truth_(dark_QMARK_3)) {
          return "dark";
        } else {
          return "light";
        }
      } else {
        if ("else") {
          if (truth_(dark_QMARK_3)) {
            return "light";
          } else {
            return "dark";
          }
        } else {
          return null;
        }
      }
    }
    ;
  };
  var opposite = function(side) {
    const G__5571 = side;
    switch (G__5571) {
      case "top":
        return "bottom";
        break;
      case "bottom":
        return "top";
        break;
      case "left":
        return "right";
        break;
      case "right":
        return "left";
        break;
      default:
        return "top";
    }
    ;
  };
  var perpendicular = function(side) {
    if (truth_((() => {
      const or__23426__auto__1 = side === "top";
      if (or__23426__auto__1) {
        return or__23426__auto__1;
      } else {
        return side === "bottom";
      }
      ;
    })())) {
      return ["right", "left"];
    } else {
      return ["top", "bottom"];
    }
    ;
  };
  var room = function(side, tr, vw, vh) {
    const G__5581 = side;
    switch (G__5581) {
      case "bottom":
        return vh - tr.bottom - gap2 - edge2;
        break;
      case "left":
        return tr.left - gap2 - edge2;
        break;
      case "right":
        return vw - tr.right - gap2 - edge2;
        break;
      default:
        return tr.top - gap2 - edge2;
    }
    ;
  };
  var need = function(side, w, h) {
    if (truth_((() => {
      const or__23426__auto__1 = side === "top";
      if (or__23426__auto__1) {
        return or__23426__auto__1;
      } else {
        return side === "bottom";
      }
      ;
    })())) {
      return h;
    } else {
      return w;
    }
    ;
  };
  var pick_side = function(pref, tr, w, h, vw, vh) {
    const order1 = into([pref, opposite(pref)], perpendicular(pref));
    const spare2 = (function(side) {
      return room(side, tr, vw, vh) - need(side, w, h);
    });
    const or__23426__auto__3 = some((function(side) {
      if (spare2(side) >= 0) {
        return side;
      }
      ;
    }), order1);
    if (truth_(or__23426__auto__3)) {
      return or__23426__auto__3;
    } else {
      return reduce((function(best, side) {
        if (spare2(side) > spare2(best)) {
          return side;
        } else {
          return best;
        }
        ;
      }), order1);
    }
    ;
  };
  var clamp6 = function(v, lo, hi) {
    return Math.max(lo, Math.min(v, hi));
  };
  var position_BANG_2 = function(layer, trigger) {
    const vw1 = document.documentElement.clientWidth;
    const vh2 = document.documentElement.clientHeight;
    layer.style.left = "0px";
    layer.style.top = "0px";
    layer.style.setProperty("--tooltip-max-width", `${vw1 - 2 * edge2}px`);
    const tr3 = trigger.getBoundingClientRect();
    const w4 = layer.offsetWidth;
    const h5 = layer.offsetHeight;
    const side6 = pick_side((() => {
      const or__23426__auto__7 = trigger.dataset.tooltipSide;
      if (truth_(or__23426__auto__7)) {
        return or__23426__auto__7;
      } else {
        return "top";
      }
      ;
    })(), tr3, w4, h5, vw1, vh2);
    const cx8 = tr3.left + tr3.width / 2;
    const cy9 = tr3.top + tr3.height / 2;
    const left10 = clamp6((() => {
      const G__55911 = side6;
      switch (G__55911) {
        case "left":
          return tr3.left - gap2 - w4;
          break;
        case "right":
          return tr3.right + gap2;
          break;
        default:
          return cx8 - w4 / 2;
      }
      ;
    })(), edge2, vw1 - w4 - edge2);
    const top13 = clamp6((() => {
      const G__56014 = side6;
      switch (G__56014) {
        case "top":
          return tr3.top - gap2 - h5;
          break;
        case "bottom":
          return tr3.bottom + gap2;
          break;
        default:
          return cy9 - h5 / 2;
      }
      ;
    })(), edge2, vh2 - h5 - edge2);
    layer.style.left = `${left10 ?? ""}px`;
    layer.style.top = `${top13 ?? ""}px`;
    layer.setAttribute("data-side", side6);
    layer.style.setProperty("--tooltip-origin-x", `${clamp6(cx8 - left10, 0, w4) ?? ""}px`);
    return layer.style.setProperty("--tooltip-origin-y", `${clamp6(cy9 - top13, 0, h5) ?? ""}px`);
  };
  var describe_BANG_ = function(trigger, on_QMARK_) {
    const target1 = (() => {
      const or__23426__auto__2 = trigger.querySelector(focusable2);
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return trigger;
      }
      ;
    })();
    if (truth_(on_QMARK_)) {
      return target1.setAttribute("aria-describedby", "ui-tooltip");
    } else {
      if (target1.getAttribute("aria-describedby") === "ui-tooltip") {
        return target1.removeAttribute("aria-describedby");
      }
    }
    ;
  };
  var clear_pending_BANG_ = function() {
    const s1 = get_state3();
    clearTimeout(s1.showTimer);
    return s1.pending = null;
  };
  var unwatch_BANG_ = function() {
    const G__5611 = get_state3().observer;
    if (G__5611 == null) {
      return null;
    } else {
      return G__5611.disconnect();
    }
    ;
  };
  var hide_BANG_ = function() {
    const s1 = get_state3();
    clear_pending_BANG_();
    clearTimeout(s1.hideTimer);
    const temp__23062__auto__2 = s1.open;
    if (truth_(temp__23062__auto__2)) {
      const t3 = temp__23062__auto__2;
      unwatch_BANG_();
      describe_BANG_(t3, false);
      s1.open = null;
      s1.lastClose = now2();
      return s1.layer.classList.remove("tooltip-layer--open", "tooltip-layer--instant");
    }
    ;
  };
  var schedule_hide_BANG_ = function() {
    const s1 = get_state3();
    clearTimeout(s1.hideTimer);
    if (truth_(s1.open)) {
      return s1.hideTimer = setTimeout(hide_BANG_, s1.layer.getAttribute("data-kind") === "card" ? card_close_ms : close_ms);
    }
    ;
  };
  var layer_BANG_ = function() {
    const s1 = get_state3();
    const or__23426__auto__2 = s1.layer;
    if (truth_(or__23426__auto__2)) {
      return or__23426__auto__2;
    } else {
      const layer3 = document.createElement("div");
      const bubble4 = document.createElement("div");
      layer3.id = "ui-tooltip";
      layer3.className = "tooltip-layer";
      layer3.setAttribute("role", "tooltip");
      bubble4.className = "tooltip-bubble";
      layer3.appendChild(bubble4);
      document.body.appendChild(layer3);
      layer3.addEventListener("pointerenter", (function(_) {
        return clearTimeout(get_state3().hideTimer);
      }));
      layer3.addEventListener("pointerleave", (function(e) {
        if (!(e.pointerType === "touch")) {
          return schedule_hide_BANG_();
        }
        ;
      }));
      s1.layer = layer3;
      s1.bubble = bubble4;
      return layer3;
    }
    ;
  };
  var render_BANG_2 = function(layer, trigger) {
    const tone1 = trigger.dataset.tooltipTone;
    get_state3().bubble.replaceChildren(content_of(trigger));
    layer.setAttribute("data-kind", trigger.dataset.tooltipKind === "card" ? "card" : "label");
    layer.setAttribute("data-theme", bubble_theme(trigger));
    if (truth_(tone1)) {
      return layer.setAttribute("data-tone", tone1);
    } else {
      return layer.removeAttribute("data-tone");
    }
    ;
  };
  var refresh_BANG_ = function() {
    const s1 = get_state3();
    const t2 = s1.open;
    if (truth_(t2)) {
      if (truth_(t2.isConnected)) {
        render_BANG_2(s1.layer, t2);
        return position_BANG_2(s1.layer, t2);
      } else {
        return hide_BANG_();
      }
      ;
    }
    ;
  };
  var watch_BANG_ = function(trigger) {
    const s1 = get_state3();
    if (truth_(s1.observer)) {
    } else {
      s1.observer = new MutationObserver((function(_) {
        return refresh_BANG_();
      }));
    }
    ;
    return s1.observer.observe(trigger, { "childList": true, "subtree": true, "characterData": true, "attributes": true });
  };
  var host_BANG_ = function(layer) {
    const host1 = (() => {
      const or__23426__auto__2 = document.fullscreenElement;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return document.body;
      }
      ;
    })();
    if (layer.parentNode === host1) {
      return null;
    } else {
      return host1.appendChild(layer);
    }
    ;
  };
  var show_BANG_2 = function(trigger, instant_QMARK_) {
    const s1 = get_state3();
    const layer2 = layer_BANG_();
    const cl3 = layer2.classList;
    const prev4 = s1.open;
    host_BANG_(layer2);
    clear_pending_BANG_();
    clearTimeout(s1.hideTimer);
    unwatch_BANG_();
    if (truth_((() => {
      const and__23442__auto__5 = prev4;
      if (truth_(and__23442__auto__5)) {
        return !(prev4 === trigger);
      } else {
        return and__23442__auto__5;
      }
      ;
    })())) {
      describe_BANG_(prev4, false);
    }
    ;
    s1.open = trigger;
    describe_BANG_(trigger, true);
    render_BANG_2(layer2, trigger);
    watch_BANG_(trigger);
    cl3.toggle("tooltip-layer--instant", instant_QMARK_);
    position_BANG_2(layer2, trigger);
    if (truth_(instant_QMARK_)) {
    } else {
      cl3.remove("tooltip-layer--open");
      layer2.getBoundingClientRect();
    }
    ;
    cl3.add("tooltip-layer--open");
    if (truth_(instant_QMARK_)) {
      return requestAnimationFrame((function() {
        return requestAnimationFrame((function() {
          return cl3.remove("tooltip-layer--instant");
        }));
      }));
    }
    ;
  };
  var delay_of = function(trigger) {
    const host1 = trigger.closest("[data-tooltip-delay]");
    const v2 = truth_(host1) ? parseFloat(host1.dataset.tooltipDelay) : null;
    if (truth_(!(v2 == null) && not(isNaN(v2)))) {
      return v2;
    } else {
      return default_delay;
    }
    ;
  };
  var warm_QMARK_ = function(s) {
    const or__23426__auto__1 = s.open;
    if (truth_(or__23426__auto__1)) {
      return or__23426__auto__1;
    } else {
      return now2() - s.lastClose < skip_ms;
    }
    ;
  };
  var enter_BANG_ = function(trigger, immediate_QMARK_) {
    const s1 = get_state3();
    clearTimeout(s1.hideTimer);
    if (truth_((() => {
      const or__23426__auto__2 = s1.open === trigger;
      if (or__23426__auto__2) {
        return or__23426__auto__2;
      } else {
        const or__23426__auto__3 = s1.pending === trigger;
        if (or__23426__auto__3) {
          return or__23426__auto__3;
        } else {
          return s1.suppressed === trigger;
        }
        ;
      }
      ;
    })())) {
      return null;
    } else {
      clear_pending_BANG_();
      const d4 = delay_of(trigger);
      if (truth_(warm_QMARK_(s1))) {
        return show_BANG_2(trigger, true);
      } else {
        if (truth_((() => {
          const or__23426__auto__5 = immediate_QMARK_;
          if (truth_(or__23426__auto__5)) {
            return or__23426__auto__5;
          } else {
            return d4 <= 0;
          }
          ;
        })())) {
          return show_BANG_2(trigger, false);
        } else {
          if ("else") {
            s1.pending = trigger;
            return s1.showTimer = setTimeout((function() {
              return show_BANG_2(trigger, false);
            }), d4);
          } else {
            return null;
          }
        }
      }
      ;
    }
    ;
  };
  var leave_BANG_ = function(trigger) {
    const s1 = get_state3();
    if (s1.suppressed === trigger) {
      s1.suppressed = null;
    }
    ;
    if (s1.pending === trigger) {
      clear_pending_BANG_();
    }
    ;
    if (s1.open === trigger) {
      return schedule_hide_BANG_();
    }
    ;
  };
  var reposition_BANG_2 = function() {
    const s1 = get_state3();
    const t2 = s1.open;
    if (truth_(t2)) {
      if (truth_(t2.isConnected)) {
        return position_BANG_2(s1.layer, t2);
      } else {
        return hide_BANG_();
      }
      ;
    }
    ;
  };
  var left_trigger_QMARK_ = function(trigger, e) {
    const to1 = e.relatedTarget;
    return not((() => {
      const and__23442__auto__2 = to1;
      if (truth_(and__23442__auto__2)) {
        return trigger.contains(to1);
      } else {
        return and__23442__auto__2;
      }
      ;
    })());
  };
  var on_pointer_over = function(e) {
    if (!(e.pointerType === "touch")) {
      const temp__23062__auto__1 = trigger_of(e.target);
      if (truth_(temp__23062__auto__1)) {
        const t2 = temp__23062__auto__1;
        return enter_BANG_(t2, false);
      }
      ;
    }
    ;
  };
  var on_pointer_out = function(e) {
    if (!(e.pointerType === "touch")) {
      const temp__23062__auto__1 = trigger_of(e.target);
      if (truth_(temp__23062__auto__1)) {
        const t2 = temp__23062__auto__1;
        if (truth_(left_trigger_QMARK_(t2, e))) {
          return leave_BANG_(t2);
        }
        ;
      }
      ;
    }
    ;
  };
  var on_pointer_down = function(e) {
    const s1 = get_state3();
    const t2 = trigger_of(e.target);
    s1.pointer = e.pointerType;
    if (truth_((() => {
      const and__23442__auto__3 = t2;
      if (truth_(and__23442__auto__3)) {
        return !(e.pointerType === "touch") && (() => {
          const or__23426__auto__4 = t2 === s1.open;
          if (or__23426__auto__4) {
            return or__23426__auto__4;
          } else {
            return t2 === s1.pending;
          }
          ;
        })();
      } else {
        return and__23442__auto__3;
      }
      ;
    })())) {
      hide_BANG_();
      return s1.suppressed = t2;
    } else {
      if (truth_(e.pointerType === "touch" && (not(t2) && not((() => {
        const and__23442__auto__5 = s1.layer;
        if (truth_(and__23442__auto__5)) {
          return s1.layer.contains(e.target);
        } else {
          return and__23442__auto__5;
        }
        ;
      })())))) {
        return hide_BANG_();
      } else {
        return null;
      }
    }
    ;
  };
  var on_click6 = function(e) {
    const s1 = get_state3();
    if (s1.pointer === "touch") {
      const temp__23062__auto__2 = trigger_of(e.target);
      if (truth_(temp__23062__auto__2)) {
        const t3 = temp__23062__auto__2;
        if (truth_((() => {
          const or__23426__auto__4 = t3.matches(interactive);
          if (truth_(or__23426__auto__4)) {
            return or__23426__auto__4;
          } else {
            return t3.querySelector(interactive);
          }
          ;
        })())) {
          return null;
        } else {
          if (s1.open === t3) {
            return hide_BANG_();
          } else {
            return enter_BANG_(t3, true);
          }
          ;
        }
        ;
      }
      ;
    }
    ;
  };
  var on_focus_in = function(e) {
    const temp__23062__auto__1 = trigger_of(e.target);
    if (truth_(temp__23062__auto__1)) {
      const t2 = temp__23062__auto__1;
      if (truth_(!(get_state3().pointer === "touch") && e.target.matches(":focus-visible"))) {
        return enter_BANG_(t2, true);
      }
      ;
    }
    ;
  };
  var on_focus_out = function(e) {
    const temp__23062__auto__1 = trigger_of(e.target);
    if (truth_(temp__23062__auto__1)) {
      const t2 = temp__23062__auto__1;
      if (truth_(left_trigger_QMARK_(t2, e))) {
        return leave_BANG_(t2);
      }
      ;
    }
    ;
  };
  var on_key_down = function(e) {
    const s1 = get_state3();
    if (truth_(e.key === "Escape" && s1.open)) {
      const t2 = s1.open;
      hide_BANG_();
      return s1.suppressed = t2;
    }
    ;
  };
  var init_BANG_11 = function() {
    if (truth_(window["__uiTooltipInit"])) {
      return null;
    } else {
      window["__uiTooltipInit"] = true;
      document.addEventListener("pointerover", on_pointer_over);
      document.addEventListener("pointerout", on_pointer_out);
      document.addEventListener("pointerdown", on_pointer_down, true);
      document.addEventListener("click", on_click6);
      document.addEventListener("focusin", on_focus_in);
      document.addEventListener("focusout", on_focus_out);
      document.addEventListener("keydown", on_key_down);
      window.addEventListener("scroll", reposition_BANG_2, { "capture": true, "passive": true });
      return window.addEventListener("resize", reposition_BANG_2);
    }
    ;
  };
  init_BANG_11();
  window["__uiTooltip"] = { "hide": hide_BANG_, "reposition": reposition_BANG_2 };

  // .compiled/touch.mjs
  var mq = window.matchMedia("(hover: none)");
  var viewport_overrides = [["width", "device-width"], ["initial-scale", "1.0"], ["maximum-scale", "1.0"], ["user-scalable", "no"]];
  var merge_viewport = function(existing) {
    const entries1 = map((function(s) {
      const i2 = s.indexOf("=");
      if (i2 < 0) {
        return [s, null];
      } else {
        return [s.slice(0, i2).trim(), s.slice(i2 + 1).trim()];
      }
      ;
    }), remove((function(s) {
      return _EQ_("", s);
    }), map((function(s) {
      return s.trim();
    }), (() => {
      const or__23426__auto__3 = existing;
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return "";
      }
      ;
    })().split(","))));
    const override_keys4 = set(map(first, viewport_overrides));
    const kept5 = remove((function(p__562) {
      const vec__69 = p__562;
      const k10 = nth(vec__69, 0, null);
      const _11 = nth(vec__69, 1, null);
      return contains_QMARK_(override_keys4, k10);
    }), entries1);
    return join(", ", map((function(p__563) {
      const vec__1215 = p__563;
      const k16 = nth(vec__1215, 0, null);
      const v17 = nth(vec__1215, 1, null);
      if (v17 == null) {
        return k16;
      } else {
        return `${k16 ?? ""}${"="}${v17 ?? ""}`;
      }
      ;
    }), concat(kept5, viewport_overrides)));
  };
  var harden_viewport_BANG_ = function() {
    const temp__23007__auto__1 = document.querySelector("meta[name=viewport]");
    if (truth_(temp__23007__auto__1)) {
      const meta_el2 = temp__23007__auto__1;
      return meta_el2.setAttribute("content", merge_viewport(meta_el2.getAttribute("content")));
    } else {
      const m3 = document.createElement("meta");
      m3.setAttribute("name", "viewport");
      m3.setAttribute("content", merge_viewport(null));
      return document.head.appendChild(m3);
    }
    ;
  };
  var sync_BANG_2 = function() {
    const touch_QMARK_1 = mq.matches;
    const cl2 = document.documentElement.classList;
    if (truth_(touch_QMARK_1)) {
      cl2.add("clj-ui-touch");
    } else {
      cl2.remove("clj-ui-touch");
    }
    ;
    return touch_QMARK_1;
  };
  window["__uiTouch"] = sync_BANG_2;
  if (truth_(sync_BANG_2())) {
    harden_viewport_BANG_();
    document.addEventListener("gesturestart", (function(e) {
      return e.preventDefault();
    }));
  }
  mq.addEventListener("change", sync_BANG_2);

  // .compiled/video_player.mjs
  var players = /* @__PURE__ */ new Map();
  var idle_ms = 2600;
  var upnext_secs = 10;
  var check_svg = '<svg class="icon video-player-icon video-player-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>';
  var volume_key = "ui-video-player:volume";
  var q2 = function(el2, sel) {
    return el2.querySelector(sel);
  };
  var qa = function(el2, sel) {
    return Array.from(el2.querySelectorAll(sel));
  };
  var flag_BANG_ = function(el2, k, on_QMARK_) {
    if (truth_(on_QMARK_)) {
      return el2.setAttribute(k, "");
    } else {
      return el2.removeAttribute(k);
    }
    ;
  };
  var esc = function(s) {
    return `${s ?? ""}`.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
  };
  var pad2 = function(n) {
    if (n < 10) {
      return `0${n ?? ""}`;
    } else {
      return `${n ?? ""}`;
    }
    ;
  };
  var fmt = function(t) {
    if (not(isFinite(t))) {
      return "0:00";
    } else {
      const t1 = Math.max(0, Math.floor(t));
      const h2 = Math.floor(t1 / 3600);
      const m3 = Math.floor(mod(t1, 3600) / 60);
      const s4 = mod(t1, 60);
      if (h2 > 0) {
        return `${h2 ?? ""}${":"}${pad2(m3) ?? ""}${":"}${pad2(s4) ?? ""}`;
      } else {
        return `${m3 ?? ""}${":"}${pad2(s4) ?? ""}`;
      }
      ;
    }
    ;
  };
  var clamp7 = function(v, lo, hi) {
    return Math.max(lo, Math.min(hi, v));
  };
  var emit_BANG_4 = function(p, detail) {
    return p.root.dispatchEvent(new CustomEvent("change", { "detail": detail }));
  };
  var text_tracks = function(p) {
    return Array.from(p.video.textTracks).filter((function(t) {
      const or__23426__auto__1 = t.kind === "subtitles";
      if (or__23426__auto__1) {
        return or__23426__auto__1;
      } else {
        return t.kind === "captions";
      }
      ;
    }));
  };
  var levels = function(p) {
    const temp__23007__auto__1 = p.hls;
    if (truth_(temp__23007__auto__1)) {
      const h2 = temp__23007__auto__1;
      const or__23426__auto__3 = h2.levels;
      if (truth_(or__23426__auto__3)) {
        return or__23426__auto__3;
      } else {
        return [];
      }
      ;
    } else {
      return [];
    }
    ;
  };
  var toast_BANG_ = function(p, msg) {
    const temp__23062__auto__1 = q2(p.root, "[data-vp=toast]");
    if (truth_(temp__23062__auto__1)) {
      const n2 = temp__23062__auto__1;
      n2.textContent = msg;
      n2.classList.remove("is-on");
      n2.offsetWidth;
      n2.classList.add("is-on");
      clearTimeout(p.toastT);
      return p.toastT = setTimeout((function() {
        return n2.classList.remove("is-on");
      }), 1600);
    }
    ;
  };
  var rates = [0.5, 0.75, 1, 1.25, 1.5, 2];
  var rate_label = function(r) {
    if (r === 1) {
      return "Normal";
    } else {
      return `${r ?? ""}${"\xD7"}`;
    }
    ;
  };
  var builtin_options = function(p, id) {
    const G__5641 = id;
    switch (G__5641) {
      case "quality":
        const lv3 = levels(p);
        const h4 = p.hls;
        const cur5 = truth_(h4) ? lv3[h4.currentLevel] : null;
        const auto_QMARK_6 = (() => {
          const or__23426__auto__7 = not(h4);
          if (or__23426__auto__7) {
            return or__23426__auto__7;
          } else {
            return h4.autoLevelEnabled;
          }
          ;
        })();
        const rows8 = lv3.map((function(l, i) {
          return { "value": i, "height": l.height };
        })).sort((function(a, b) {
          return b.height - a.height;
        }));
        return [{ "value": -1, "label": "Auto", "default": true, "hint": truth_((() => {
          const and__23442__auto__9 = auto_QMARK_6;
          if (truth_(and__23442__auto__9)) {
            return cur5;
          } else {
            return and__23442__auto__9;
          }
          ;
        })()) ? `${cur5.height ?? ""}p` : null }].concat(rows8.map((function(r) {
          return { "value": r.value, "label": `${r.height ?? ""}p`, "hint": r.height >= 720 ? "HD" : null };
        })));
        break;
      case "subtitles":
        return [{ "value": -1, "label": "Off", "default": true }].concat(text_tracks(p).map((function(t, i) {
          return { "value": i, "label": (() => {
            const or__23426__auto__10 = not_empty(t.label);
            if (truth_(or__23426__auto__10)) {
              return or__23426__auto__10;
            } else {
              const or__23426__auto__11 = not_empty(t.language);
              if (truth_(or__23426__auto__11)) {
                return or__23426__auto__11;
              } else {
                return `${"Track "}${i + 1}`;
              }
              ;
            }
            ;
          })() };
        })));
        break;
      case "speed":
        return into_array(rates).map((function(r) {
          return { "value": r, "label": rate_label(r), "default": r === 1 };
        }));
        break;
      default:
        return [];
    }
    ;
  };
  var builtin_value = function(p, id) {
    const G__5651 = id;
    switch (G__5651) {
      case "quality":
        const h3 = p.hls;
        if (truth_((() => {
          const or__23426__auto__4 = not(h3);
          if (or__23426__auto__4) {
            return or__23426__auto__4;
          } else {
            return h3.autoLevelEnabled;
          }
          ;
        })())) {
          return -1;
        } else {
          return h3.currentLevel;
        }
        ;
        break;
      case "subtitles":
        return p.sub;
        break;
      case "speed":
        return p.video.playbackRate;
        break;
    }
    ;
  };
  var builtin_summary = function(p, id) {
    const G__5661 = id;
    switch (G__5661) {
      case "quality":
        const h3 = p.hls;
        const cur4 = truth_(h3) ? levels(p)[h3.currentLevel] : null;
        if (not(cur4)) {
          return "Auto";
        } else {
          if (truth_(h3.autoLevelEnabled)) {
            return `${"Auto \xB7 "}${cur4.height ?? ""}p`;
          } else {
            if ("else") {
              return `${cur4.height ?? ""}p`;
            } else {
              return null;
            }
          }
        }
        ;
        break;
    }
    ;
  };
  var set_sub_BANG_ = function(p, i, say_QMARK_) {
    const ts1 = text_tracks(p);
    ts1.forEach((function(t, j) {
      return t.mode = _EQ_(j, i) ? "hidden" : "disabled";
    }));
    p.sub = i;
    if (i >= 0) {
      p.lastSub = i;
    }
    ;
    draw_cue_BANG_(p);
    if (truth_(say_QMARK_)) {
      return toast_BANG_(p, i < 0 ? "Subtitles off" : `${"Subtitles: "}${ts1[i].label ?? ""}`);
    }
    ;
  };
  var builtin_select_BANG_ = function(p, id, v) {
    const G__5671 = id;
    switch (G__5671) {
      case "quality":
        const temp__23062__auto__3 = p.hls;
        if (truth_(temp__23062__auto__3)) {
          const h4 = temp__23062__auto__3;
          h4.currentLevel = v;
          return toast_BANG_(p, v < 0 ? "Quality: Auto" : `${"Quality: "}${levels(p)[v].height ?? ""}p`);
        }
        ;
        break;
      case "subtitles":
        return set_sub_BANG_(p, v, true);
        break;
      case "speed":
        p.video.playbackRate = v;
        return toast_BANG_(p, `${"Speed: "}${rate_label(v) ?? ""}`);
        break;
    }
    ;
  };
  var fill_panel_BANG_ = function(p, panel, id) {
    const cur1 = builtin_value(p, id);
    const html2 = builtin_options(p, id).map((function(o) {
      return `${'<button type="button" class="video-player-option" role="menuitemradio"'}${' aria-checked="'}${_EQ_(o.value, cur1)}${'"'}${' data-vp-value="'}${esc(o.value) ?? ""}${'"'}${(truth_(o.default) ? ' data-vp-default="true"' : null) ?? ""}${">"}${'<span class="video-player-option-label">'}${esc(o.label) ?? ""}${"</span>"}${(truth_(o.hint) ? `${'<span class="video-player-option-hint">'}${esc(o.hint) ?? ""}${"</span>"}` : null) ?? ""}${check_svg ?? ""}${"</button>"}`;
    })).join("");
    const title3 = q2(panel, ".video-player-panel-title");
    panel.innerHTML = "";
    if (truth_(title3)) {
      panel.appendChild(title3);
    }
    ;
    return panel.insertAdjacentHTML("beforeend", html2);
  };
  var place_BANG_2 = function(p, panel, trigger) {
    const r1 = p.root.getBoundingClientRect();
    const b2 = trigger.getBoundingClientRect();
    const w3 = panel.offsetWidth;
    const left4 = b2.left + b2.width / 2 - w3 / 2 - r1.left;
    panel.style.bottom = `${r1.bottom - b2.top + 12}px`;
    return panel.style.left = `${clamp7(left4, 8, r1.width - w3 - 8) ?? ""}px`;
  };
  var arm_hide_BANG_ = function(p) {
    clearTimeout(p.hideT);
    return p.hideT = setTimeout((function() {
      if (truth_(not(p.video.paused) && not(p.open))) {
        return flag_BANG_(p.root, "data-idle", true);
      }
      ;
    }), idle_ms);
  };
  var show_ui_BANG_ = function(p) {
    flag_BANG_(p.root, "data-idle", false);
    return arm_hide_BANG_(p);
  };
  var close_panel_BANG_ = function(p) {
    const temp__23062__auto__1 = p.open;
    if (truth_(temp__23062__auto__1)) {
      const o2 = temp__23062__auto__1;
      o2.panel.hidden = true;
      o2.trigger.setAttribute("aria-expanded", "false");
      p.open = null;
      flag_BANG_(p.root, "data-panel-open", false);
      return arm_hide_BANG_(p);
    }
    ;
  };
  var open_panel_BANG_ = function(p, id, trigger) {
    const panel1 = q2(p.root, `${'[data-vp-panel="'}${id ?? ""}${'"]'}`);
    const same_QMARK_2 = (() => {
      const and__23442__auto__3 = p.open;
      if (truth_(and__23442__auto__3)) {
        return p.open.panel === panel1;
      } else {
        return and__23442__auto__3;
      }
      ;
    })();
    close_panel_BANG_(p);
    if (truth_((() => {
      const and__23442__auto__4 = panel1;
      if (truth_(and__23442__auto__4)) {
        return not(same_QMARK_2);
      } else {
        return and__23442__auto__4;
      }
      ;
    })())) {
      if (truth_(trigger.hasAttribute("data-vp-builtin"))) {
        fill_panel_BANG_(p, panel1, id);
      }
      ;
      panel1.hidden = false;
      place_BANG_2(p, panel1, trigger);
      trigger.setAttribute("aria-expanded", "true");
      p.open = { "panel": panel1, "trigger": trigger, "id": id };
      flag_BANG_(p.root, "data-panel-open", true);
      show_ui_BANG_(p);
      const temp__23062__auto__5 = (() => {
        const or__23426__auto__6 = q2(panel1, "[aria-checked=true]");
        if (truth_(or__23426__auto__6)) {
          return or__23426__auto__6;
        } else {
          return q2(panel1, ".video-player-option");
        }
        ;
      })();
      if (truth_(temp__23062__auto__5)) {
        const c7 = temp__23062__auto__5;
        return c7.focus({ "preventScroll": true });
      }
      ;
    }
    ;
  };
  var refresh_trigger_BANG_ = function(p, id) {
    const root1 = p.root;
    const trigger2 = q2(root1, `${'[data-vp-open="'}${id ?? ""}${'"]'}`);
    const panel3 = q2(root1, `${'[data-vp-panel="'}${id ?? ""}${'"]'}`);
    if (truth_((() => {
      const and__23442__auto__4 = trigger2;
      if (truth_(and__23442__auto__4)) {
        return panel3;
      } else {
        return and__23442__auto__4;
      }
      ;
    })())) {
      if (truth_(trigger2.hasAttribute("data-vp-builtin"))) {
        const opts5 = builtin_options(p, id);
        const cur6 = builtin_value(p, id);
        const picked7 = opts5.find((function(o) {
          return _EQ_(o.value, cur6);
        }));
        trigger2.setAttribute("aria-pressed", `${boolean$((() => {
          const and__23442__auto__8 = picked7;
          if (truth_(and__23442__auto__8)) {
            return not(picked7.default);
          } else {
            return and__23442__auto__8;
          }
          ;
        })()) ?? ""}`);
        const temp__23062__auto__9 = q2(trigger2, "[data-vp-summary]");
        if (truth_(temp__23062__auto__9)) {
          const s10 = temp__23062__auto__9;
          return s10.textContent = (() => {
            const or__23426__auto__11 = builtin_summary(p, id);
            if (truth_(or__23426__auto__11)) {
              return or__23426__auto__11;
            } else {
              const or__23426__auto__12 = truth_(picked7) ? picked7.label : null;
              if (truth_(or__23426__auto__12)) {
                return or__23426__auto__12;
              } else {
                return "";
              }
              ;
            }
            ;
          })();
        }
        ;
      } else {
        const checked13 = q2(panel3, "[aria-checked=true]");
        const dflt14 = (() => {
          const or__23426__auto__15 = q2(panel3, "[data-vp-default]");
          if (truth_(or__23426__auto__15)) {
            return or__23426__auto__15;
          } else {
            return q2(panel3, ".video-player-option");
          }
          ;
        })();
        trigger2.setAttribute("aria-pressed", `${boolean$((() => {
          const and__23442__auto__16 = checked13;
          if (truth_(and__23442__auto__16)) {
            return !(checked13 === dflt14);
          } else {
            return and__23442__auto__16;
          }
          ;
        })()) ?? ""}`);
        const temp__23062__auto__17 = q2(trigger2, "[data-vp-summary]");
        if (truth_(temp__23062__auto__17)) {
          const s18 = temp__23062__auto__17;
          if (truth_(checked13)) {
            return s18.textContent = q2(checked13, ".video-player-option-label").textContent;
          }
          ;
        }
        ;
      }
      ;
    }
    ;
  };
  var pick_BANG_ = function(p, panel, opt) {
    const id1 = p.open.id;
    const trigger2 = p.open.trigger;
    const raw3 = opt.getAttribute("data-vp-value");
    if (truth_(trigger2.hasAttribute("data-vp-builtin"))) {
      const v4 = parseFloat(raw3);
      builtin_select_BANG_(p, id1, v4);
      emit_BANG_4(p, { "kind": "select", "id": id1, "value": v4 });
    } else {
      qa(panel, ".video-player-option").forEach((function(o) {
        return o.setAttribute("aria-checked", `${o === opt}`);
      }));
      emit_BANG_4(p, { "kind": "select", "id": id1, "value": raw3 });
    }
    ;
    close_panel_BANG_(p);
    trigger2.focus({ "preventScroll": true });
    return sync_BANG_3(p);
  };
  var draw_cue_BANG_ = function(p) {
    const temp__23062__auto__1 = q2(p.root, "[data-vp=caption]");
    if (truth_(temp__23062__auto__1)) {
      const box2 = temp__23062__auto__1;
      const t3 = text_tracks(p)[p.sub];
      const cues4 = truth_(t3) ? t3.activeCues : null;
      const text5 = truth_((() => {
        const and__23442__auto__6 = cues4;
        if (truth_(and__23442__auto__6)) {
          return cues4.length > 0;
        } else {
          return and__23442__auto__6;
        }
        ;
      })()) ? Array.from(cues4).map((function(c) {
        return c.text;
      })).join("\n") : null;
      return box2.innerHTML = truth_(text5) ? `${"<span>"}${esc(text5.replace(new RegExp("<[^>]+>", "g"), "")).replaceAll("\n", "<br>") ?? ""}${"</span>"}` : "";
    }
    ;
  };
  var wire_track_BANG_ = function(p, t) {
    if (truth_(t.__vp)) {
      return null;
    } else {
      t.__vp = true;
      return t.addEventListener("cuechange", (function() {
        return draw_cue_BANG_(p);
      }));
    }
    ;
  };
  var init_tracks_BANG_ = function(p) {
    const ts1 = text_tracks(p);
    ts1.forEach((function(t) {
      return wire_track_BANG_(p, t);
    }));
    if (p.sub < 0) {
      const i2 = ts1.findIndex((function(t) {
        return t.mode === "showing";
      }));
      if (i2 >= 0) {
        set_sub_BANG_(p, i2, false);
      } else {
        ts1.forEach((function(t) {
          return t.mode = "disabled";
        }));
      }
    }
    ;
    const temp__23062__auto__3 = q2(p.root, "[data-vp-builtin=subtitles]");
    if (truth_(temp__23062__auto__3)) {
      const b4 = temp__23062__auto__3;
      return b4.hidden = ts1.length === 0;
    }
    ;
  };
  var has_next_QMARK_ = function(p) {
    return !(q2(p.root, "[data-vp=upnext]") == null);
  };
  var go_next_BANG_ = function(p) {
    p.dismissed = false;
    emit_BANG_4(p, { "kind": "next" });
    const temp__23062__auto__1 = p.root.dataset.nextHref;
    if (truth_(temp__23062__auto__1)) {
      const href2 = temp__23062__auto__1;
      return location.href = href2;
    }
    ;
  };
  var upnext_BANG_ = function(p) {
    const temp__23062__auto__1 = q2(p.root, "[data-vp=upnext]");
    if (truth_(temp__23062__auto__1)) {
      const card2 = temp__23062__auto__1;
      const v3 = p.video;
      const d4 = (() => {
        const or__23426__auto__5 = v3.duration;
        if (truth_(or__23426__auto__5)) {
          return or__23426__auto__5;
        } else {
          return 0;
        }
        ;
      })();
      const left6 = d4 - v3.currentTime;
      const show_QMARK_7 = (() => {
        const and__23442__auto__8 = p.auto;
        if (truth_(and__23442__auto__8)) {
          return not(p.dismissed) && (() => {
            const and__23442__auto__9 = isFinite(d4);
            if (truth_(and__23442__auto__9)) {
              return d4 > 0 && left6 <= upnext_secs;
            } else {
              return and__23442__auto__9;
            }
            ;
          })();
        } else {
          return and__23442__auto__8;
        }
        ;
      })();
      card2.hidden = not(show_QMARK_7);
      flag_BANG_(p.root, "data-upnext", show_QMARK_7);
      if (truth_(show_QMARK_7)) {
        card2.style.setProperty("--vp-countdown", 1 - left6 / upnext_secs);
        const temp__23062__auto__10 = q2(card2, "[data-vp=upnext-secs]");
        if (truth_(temp__23062__auto__10)) {
          const s11 = temp__23062__auto__10;
          return s11.textContent = Math.ceil(left6);
        }
        ;
      }
      ;
    }
    ;
  };
  var sync_BANG_3 = function(p) {
    const root1 = p.root;
    const v2 = p.video;
    const muted_QMARK_3 = (() => {
      const or__23426__auto__4 = v2.muted;
      if (truth_(or__23426__auto__4)) {
        return or__23426__auto__4;
      } else {
        return v2.volume === 0;
      }
      ;
    })();
    flag_BANG_(root1, "data-paused", v2.paused);
    flag_BANG_(root1, "data-muted", muted_QMARK_3);
    if (truth_(muted_QMARK_3)) {
    } else {
      flag_BANG_(root1, "data-autoplay-muted", false);
    }
    ;
    flag_BANG_(root1, "data-fullscreen", document.fullscreenElement === root1);
    qa(root1, "[data-vp-act=play]").forEach((function(b) {
      const l5 = truth_(v2.paused) ? "Play" : "Pause";
      b.setAttribute("aria-label", l5);
      return b.setAttribute("data-tooltip", `${l5 ?? ""}${" (space)"}`);
    }));
    const temp__23062__auto__6 = q2(root1, "[data-vp-toggle=autonext]");
    if (truth_(temp__23062__auto__6)) {
      const b7 = temp__23062__auto__6;
      b7.setAttribute("aria-pressed", `${p.auto ?? ""}`);
    }
    ;
    const temp__23062__auto__8 = q2(root1, "[data-vp=volume]");
    if (truth_(temp__23062__auto__8)) {
      const vol9 = temp__23062__auto__8;
      const x10 = truth_(muted_QMARK_3) ? 0 : v2.volume;
      vol9.style.setProperty("--vp-volume", x10);
      vol9.setAttribute("aria-valuenow", Math.round(100 * x10));
    }
    ;
    qa(root1, "[data-vp-open]").forEach((function(b) {
      return refresh_trigger_BANG_(p, b.getAttribute("data-vp-open"));
    }));
    const temp__23062__auto__11 = p.open;
    if (truth_(temp__23062__auto__11)) {
      const o12 = temp__23062__auto__11;
      if (truth_(o12.trigger.hasAttribute("data-vp-builtin"))) {
        fill_panel_BANG_(p, o12.panel, o12.id);
      }
    }
    ;
    return upnext_BANG_(p);
  };
  var tick_BANG_ = function(p) {
    const root1 = p.root;
    const v2 = p.video;
    const d3 = truth_(isFinite(v2.duration)) ? v2.duration : 0;
    const t4 = v2.currentTime;
    const scrub5 = q2(root1, "[data-vp=scrub]");
    const temp__23062__auto__6 = q2(root1, "[data-vp=time]");
    if (truth_(temp__23062__auto__6)) {
      const n7 = temp__23062__auto__6;
      n7.textContent = fmt(t4);
    }
    ;
    const temp__23062__auto__8 = q2(root1, "[data-vp=left]");
    if (truth_(temp__23062__auto__8)) {
      const n9 = temp__23062__auto__8;
      n9.textContent = `-${fmt(d3 - t4) ?? ""}`;
    }
    ;
    if (truth_((() => {
      const and__23442__auto__10 = scrub5;
      if (truth_(and__23442__auto__10)) {
        return d3 > 0;
      } else {
        return and__23442__auto__10;
      }
      ;
    })())) {
      if (truth_(p.dragging)) {
      } else {
        scrub5.style.setProperty("--vp-played", t4 / d3);
      }
      ;
      scrub5.setAttribute("aria-valuemax", Math.round(d3));
      scrub5.setAttribute("aria-valuenow", Math.round(t4));
      scrub5.setAttribute("aria-valuetext", `${fmt(t4) ?? ""}${" of "}${fmt(d3) ?? ""}`);
      const b11 = v2.buffered;
      const n56812 = b11.length;
      let i13 = 0;
      for (; i13 < n56812; i13++) {
        b11.start(i13) <= t4 + 0.5 ? scrub5.style.setProperty("--vp-buffered", b11.end(i13) / d3) : null;
      }
      ;
    }
    ;
    return upnext_BANG_(p);
  };
  var toggle_play_BANG_ = function(p) {
    const v1 = p.video;
    if (truth_(v1.paused)) {
      const r2 = v1.play();
      if (truth_((() => {
        const and__23442__auto__3 = r2;
        if (truth_(and__23442__auto__3)) {
          return r2.catch;
        } else {
          return and__23442__auto__3;
        }
        ;
      })())) {
        return r2.catch((function(_) {
          return null;
        }));
      }
      ;
    } else {
      return v1.pause();
    }
    ;
  };
  var autoplay_BANG_ = function(p) {
    const v1 = p.video;
    const r2 = v1.play();
    if (truth_((() => {
      const and__23442__auto__3 = r2;
      if (truth_(and__23442__auto__3)) {
        return r2.catch;
      } else {
        return and__23442__auto__3;
      }
      ;
    })())) {
      return r2.catch((function(e) {
        if ("NotAllowedError" === e.name) {
          v1.muted = true;
          flag_BANG_(p.root, "data-autoplay-muted", true);
          const r24 = v1.play();
          if (truth_((() => {
            const and__23442__auto__5 = r24;
            if (truth_(and__23442__auto__5)) {
              return r24.catch;
            } else {
              return and__23442__auto__5;
            }
            ;
          })())) {
            return r24.catch((function(_) {
              return null;
            }));
          }
          ;
        }
        ;
      }));
    }
    ;
  };
  var unmute_BANG_ = function(p) {
    const v1 = p.video;
    v1.muted = false;
    if (v1.volume === 0) {
      v1.volume = 1;
    }
    ;
    return flag_BANG_(p.root, "data-autoplay-muted", false);
  };
  var auto_muted_QMARK_ = function(p) {
    return p.root.hasAttribute("data-autoplay-muted");
  };
  var seek_by_BANG_ = function(p, s) {
    const v1 = p.video;
    return v1.currentTime = clamp7(v1.currentTime + s, 0, (() => {
      const or__23426__auto__2 = v1.duration;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        return 0;
      }
      ;
    })());
  };
  var set_volume_BANG_ = function(p, x) {
    const v1 = p.video;
    const x2 = clamp7(x, 0, 1);
    v1.volume = x2;
    v1.muted = x2 === 0;
    return (() => {
      try {
        return localStorage.setItem(volume_key, `${x2 ?? ""}`);
      } catch (_3) {
        return null;
      }
    })();
  };
  var fullscreen_BANG_ = function(p) {
    const root1 = p.root;
    const v2 = p.video;
    if (truth_(document.fullscreenElement)) {
      return document.exitFullscreen();
    } else {
      if (truth_(root1.requestFullscreen)) {
        return root1.requestFullscreen();
      } else {
        if (truth_(v2.webkitEnterFullscreen)) {
          return v2.webkitEnterFullscreen();
        } else {
          return null;
        }
      }
    }
    ;
  };
  var act_BANG_ = function(p, a) {
    const v1 = p.video;
    const G__5692 = a;
    switch (G__5692) {
      case "play":
        toggle_play_BANG_(p);
        break;
      case "back":
        seek_by_BANG_(p, -10);
        toast_BANG_(p, "\u221210 s");
        break;
      case "forward":
        seek_by_BANG_(p, 10);
        toast_BANG_(p, "+10 s");
        break;
      case "mute":
        if (truth_((() => {
          const or__23426__auto__4 = v1.muted;
          if (truth_(or__23426__auto__4)) {
            return or__23426__auto__4;
          } else {
            return v1.volume === 0;
          }
          ;
        })())) {
          unmute_BANG_(p);
        } else {
          v1.muted = true;
        }
        ;
        break;
      case "unmute":
        unmute_BANG_(p);
        break;
      case "next":
        go_next_BANG_(p);
        break;
      case "upnext-cancel":
        p.dismissed = true;
        upnext_BANG_(p);
        break;
      case "pip":
        if (truth_(document.pictureInPictureElement)) {
          document.exitPictureInPicture();
        } else {
          if (truth_(v1.requestPictureInPicture)) {
            v1.requestPictureInPicture();
          }
        }
        ;
        break;
      case "fullscreen":
        fullscreen_BANG_(p);
        break;
    }
    ;
    return sync_BANG_3(p);
  };
  var toggle_BANG_2 = function(p, btn, id) {
    const on_QMARK_1 = !("true" === btn.getAttribute("aria-pressed"));
    if (id === "autonext") {
      p.auto = on_QMARK_1;
      p.dismissed = false;
      toast_BANG_(p, on_QMARK_1 ? "Autoplay is on" : "Autoplay is off");
    } else {
      btn.setAttribute("aria-pressed", `${on_QMARK_1}`);
    }
    ;
    emit_BANG_4(p, { "kind": "toggle", "id": id, "on": on_QMARK_1 });
    return sync_BANG_3(p);
  };
  var frac = function(el2, e) {
    const r1 = el2.getBoundingClientRect();
    return clamp7((e.clientX - r1.left) / r1.width, 0, 1);
  };
  var wire_scrub_BANG_ = function(p, scrub) {
    const v1 = p.video;
    const tip2 = q2(scrub, "[data-vp=tip]");
    scrub.addEventListener("pointermove", (function(e) {
      const f3 = frac(scrub, e);
      scrub.style.setProperty("--vp-pointer", f3);
      if (truth_(tip2)) {
        tip2.textContent = fmt(f3 * (() => {
          const or__23426__auto__4 = v1.duration;
          if (truth_(or__23426__auto__4)) {
            return or__23426__auto__4;
          } else {
            return 0;
          }
          ;
        })());
      }
      ;
      if (truth_(p.dragging)) {
        return scrub.style.setProperty("--vp-played", f3);
      }
      ;
    }));
    scrub.addEventListener("pointerdown", (function(e) {
      p.dragging = true;
      scrub.setPointerCapture(e.pointerId);
      scrub.classList.add("is-dragging");
      return scrub.style.setProperty("--vp-played", frac(scrub, e));
    }));
    const end5 = (function(e) {
      if (truth_(p.dragging)) {
        p.dragging = false;
        scrub.classList.remove("is-dragging");
        return v1.currentTime = frac(scrub, e) * (() => {
          const or__23426__auto__6 = v1.duration;
          if (truth_(or__23426__auto__6)) {
            return or__23426__auto__6;
          } else {
            return 0;
          }
          ;
        })();
      }
      ;
    });
    scrub.addEventListener("pointerup", end5);
    scrub.addEventListener("pointercancel", end5);
    return scrub.addEventListener("keydown", (function(e) {
      const G__5707 = e.key;
      switch (G__5707) {
        case "ArrowLeft":
          e.preventDefault();
          e.stopPropagation();
          return seek_by_BANG_(p, -5);
          break;
        case "ArrowRight":
          e.preventDefault();
          e.stopPropagation();
          return seek_by_BANG_(p, 5);
          break;
        case "Home":
          e.preventDefault();
          return v1.currentTime = 0;
          break;
        case "End":
          e.preventDefault();
          return v1.currentTime = (() => {
            const or__23426__auto__9 = v1.duration;
            if (truth_(or__23426__auto__9)) {
              return or__23426__auto__9;
            } else {
              return 0;
            }
            ;
          })();
          break;
      }
      ;
    }));
  };
  var wire_volume_BANG_ = function(p, vol) {
    const drag1 = (function(e) {
      return set_volume_BANG_(p, frac(vol, e));
    });
    vol.addEventListener("pointerdown", (function(e) {
      p.volDrag = true;
      vol.setPointerCapture(e.pointerId);
      return drag1(e);
    }));
    vol.addEventListener("pointermove", (function(e) {
      if (truth_(p.volDrag)) {
        return drag1(e);
      }
      ;
    }));
    vol.addEventListener("pointerup", (function(_) {
      return p.volDrag = false;
    }));
    return vol.addEventListener("keydown", (function(e) {
      const x2 = p.video.volume;
      const k3 = e.key;
      const step4 = truth_((() => {
        const or__23426__auto__5 = k3 === "ArrowLeft";
        if (or__23426__auto__5) {
          return or__23426__auto__5;
        } else {
          return k3 === "ArrowDown";
        }
        ;
      })()) ? -0.1 : truth_((() => {
        const or__23426__auto__6 = k3 === "ArrowRight";
        if (or__23426__auto__6) {
          return or__23426__auto__6;
        } else {
          return k3 === "ArrowUp";
        }
        ;
      })()) ? 0.1 : null;
      if (truth_(step4)) {
        e.preventDefault();
        e.stopPropagation();
        return set_volume_BANG_(p, x2 + step4);
      }
      ;
    }));
  };
  var on_key_BANG_ = function(p, e) {
    const k1 = e.key;
    const handled2 = truth_(k1 === "Escape" && p.open) ? (() => {
      close_panel_BANG_(p);
      return true;
    })() : truth_(p.open) ? false : truth_((() => {
      const or__23426__auto__3 = k1 === " ";
      if (or__23426__auto__3) {
        return or__23426__auto__3;
      } else {
        return k1 === "k";
      }
      ;
    })()) ? (() => {
      toggle_play_BANG_(p);
      return true;
    })() : k1 === "f" ? (() => {
      fullscreen_BANG_(p);
      return true;
    })() : k1 === "m" ? (() => {
      act_BANG_(p, "mute");
      return true;
    })() : k1 === "c" ? (() => {
      if (text_tracks(p).length > 0) {
        set_sub_BANG_(p, p.sub < 0 ? p.lastSub : -1, true);
        sync_BANG_3(p);
      }
      ;
      return true;
    })() : truth_(k1 === "n" && has_next_QMARK_(p)) ? (() => {
      go_next_BANG_(p);
      return true;
    })() : k1 === "ArrowRight" ? (() => {
      seek_by_BANG_(p, 5);
      toast_BANG_(p, "+5 s");
      return true;
    })() : k1 === "ArrowLeft" ? (() => {
      seek_by_BANG_(p, -5);
      toast_BANG_(p, "\u22125 s");
      return true;
    })() : k1 === "ArrowUp" ? (() => {
      set_volume_BANG_(p, p.video.volume + 0.1);
      return true;
    })() : k1 === "ArrowDown" ? (() => {
      set_volume_BANG_(p, p.video.volume - 0.1);
      return true;
    })() : "else" ? false : null;
    if (truth_(handled2)) {
      e.preventDefault();
      return show_ui_BANG_(p);
    }
    ;
  };
  var on_document_key = function(e) {
    const t1 = e.target;
    if (truth_((() => {
      const or__23426__auto__2 = e.defaultPrevented;
      if (truth_(or__23426__auto__2)) {
        return or__23426__auto__2;
      } else {
        const or__23426__auto__3 = e.ctrlKey;
        if (truth_(or__23426__auto__3)) {
          return or__23426__auto__3;
        } else {
          const or__23426__auto__4 = e.metaKey;
          if (truth_(or__23426__auto__4)) {
            return or__23426__auto__4;
          } else {
            const or__23426__auto__5 = e.altKey;
            if (truth_(or__23426__auto__5)) {
              return or__23426__auto__5;
            } else {
              const and__23442__auto__6 = t1.closest;
              if (truth_(and__23442__auto__6)) {
                return t1.closest("input,textarea,select,[contenteditable]");
              } else {
                return and__23442__auto__6;
              }
              ;
            }
            ;
          }
          ;
        }
        ;
      }
      ;
    })())) {
      return null;
    } else {
      const root7 = (() => {
        const or__23426__auto__8 = (() => {
          const and__23442__auto__9 = t1.closest;
          if (truth_(and__23442__auto__9)) {
            return t1.closest("[data-video-player]");
          } else {
            return and__23442__auto__9;
          }
          ;
        })();
        if (truth_(or__23426__auto__8)) {
          return or__23426__auto__8;
        } else {
          if (truth_((() => {
            const and__23442__auto__11 = (() => {
              const or__23426__auto__10 = t1 === document.body;
              if (or__23426__auto__10) {
                return or__23426__auto__10;
              } else {
                return t1 === document.documentElement;
              }
              ;
            })();
            if (truth_(and__23442__auto__11)) {
              return 1 === players.size;
            } else {
              return and__23442__auto__11;
            }
            ;
          })())) {
            return players.values().next().value.root;
          }
        }
        ;
      })();
      const temp__23062__auto__12 = (() => {
        const and__23442__auto__13 = root7;
        if (truth_(and__23442__auto__13)) {
          return players.get(root7);
        } else {
          return and__23442__auto__13;
        }
        ;
      })();
      if (truth_(temp__23062__auto__12)) {
        const p14 = temp__23062__auto__12;
        if (truth_(e.key === " " && t1.closest("button,a"))) {
          return null;
        } else {
          return on_key_BANG_(p14, e);
        }
        ;
      }
      ;
    }
    ;
  };
  var destroy_hls_BANG_ = function(p) {
    const temp__23062__auto__1 = p.hls;
    if (truth_(temp__23062__auto__1)) {
      const h2 = temp__23062__auto__1;
      h2.destroy();
      return p.hls = null;
    }
    ;
  };
  var load_BANG_ = function(p) {
    destroy_hls_BANG_(p);
    const root1 = p.root;
    const v2 = p.video;
    const src3 = (() => {
      const or__23426__auto__4 = root1.dataset.src;
      if (truth_(or__23426__auto__4)) {
        return or__23426__auto__4;
      } else {
        return "";
      }
      ;
    })();
    const start5 = parseFloat((() => {
      const or__23426__auto__6 = root1.dataset.startAt;
      if (truth_(or__23426__auto__6)) {
        return or__23426__auto__6;
      } else {
        return "0";
      }
      ;
    })());
    const start7 = truth_(isNaN(start5)) ? 0 : start5;
    const hls_QMARK_8 = (() => {
      const or__23426__auto__9 = "true" === root1.dataset.hls;
      if (or__23426__auto__9) {
        return or__23426__auto__9;
      } else {
        return new RegExp("\\.m3u8($|[?#])", "i").test(src3);
      }
      ;
    })();
    const Hls10 = window.Hls;
    const autoplay_QMARK_11 = "true" === root1.dataset.autoplay;
    const play_BANG_12 = (function() {
      if (autoplay_QMARK_11) {
        return autoplay_BANG_(p);
      }
      ;
    });
    p.src = src3;
    p.dismissed = false;
    p.sub = -1;
    flag_BANG_(root1, "data-error", false);
    flag_BANG_(root1, "data-loading", autoplay_QMARK_11 && !_EQ_("", src3));
    if (_EQ_("", src3)) {
      const temp__23062__auto__13 = q2(root1, "[data-vp-builtin=quality]");
      if (truth_(temp__23062__auto__13)) {
        const b14 = temp__23062__auto__13;
        b14.hidden = true;
      }
      ;
      v2.removeAttribute("src");
      return v2.load();
    } else {
      if (truth_((() => {
        const and__23442__auto__15 = hls_QMARK_8;
        if (truth_(and__23442__auto__15)) {
          const and__23442__auto__16 = Hls10;
          if (truth_(and__23442__auto__16)) {
            return Hls10.isSupported();
          } else {
            return and__23442__auto__16;
          }
          ;
        } else {
          return and__23442__auto__15;
        }
        ;
      })())) {
        const h17 = new Hls10({ "startPosition": start7 > 0 ? start7 : -1, "maxBufferLength": 30 });
        p.hls = h17;
        p.fails = 0;
        h17.loadSource(src3);
        h17.attachMedia(v2);
        h17.on(Hls10.Events.MANIFEST_PARSED, (function() {
          const temp__23062__auto__18 = q2(root1, "[data-vp-builtin=quality]");
          if (truth_(temp__23062__auto__18)) {
            const b19 = temp__23062__auto__18;
            b19.hidden = levels(p).length < 2;
          }
          ;
          sync_BANG_3(p);
          return play_BANG_12();
        }));
        h17.on(Hls10.Events.LEVEL_SWITCHED, (function() {
          return sync_BANG_3(p);
        }));
        h17.on(Hls10.Events.FRAG_LOADED, (function() {
          return p.fails = 0;
        }));
        return h17.on(Hls10.Events.ERROR, (function(_, d) {
          if (truth_(d.fatal)) {
            p.fails = p.fails + 1;
            if (p.fails > 8) {
              flag_BANG_(root1, "data-error", true);
              return toast_BANG_(p, `${"Playback error: "}${d.details ?? ""}`);
            } else {
              if (_EQ_(d.type, Hls10.ErrorTypes.MEDIA_ERROR)) {
                return h17.recoverMediaError();
              } else {
                if ("else") {
                  toast_BANG_(p, "Reconnecting\u2026");
                  return setTimeout((function() {
                    if (p.hls === h17) {
                      return h17.startLoad(v2.currentTime);
                    }
                    ;
                  }), Math.min(8e3, 500 * p.fails));
                } else {
                  return null;
                }
              }
            }
            ;
          }
          ;
        }));
      } else {
        if ("else") {
          const temp__23062__auto__20 = q2(root1, "[data-vp-builtin=quality]");
          if (truth_(temp__23062__auto__20)) {
            const b21 = temp__23062__auto__20;
            b21.hidden = true;
          }
          ;
          if (truth_((() => {
            const and__23442__auto__22 = hls_QMARK_8;
            if (truth_(and__23442__auto__22)) {
              return _EQ_("", v2.canPlayType("application/vnd.apple.mpegurl"));
            } else {
              return and__23442__auto__22;
            }
            ;
          })())) {
            flag_BANG_(root1, "data-error", true);
            return toast_BANG_(p, "This browser can't play HLS");
          } else {
            v2.src = src3;
            if (start7 > 0) {
              v2.addEventListener("loadedmetadata", (function() {
                return v2.currentTime = start7;
              }), { "once": true });
            }
            ;
            return play_BANG_12();
          }
          ;
        } else {
          return null;
        }
      }
    }
    ;
  };
  var init_BANG_12 = function(root) {
    const v1 = q2(root, "video");
    const p2 = { "open": null, "video": v1, "hls": null, "dismissed": false, "sub": -1, "volDrag": false, "dragging": false, "root": root, "lastSub": 0, "auto": "true" === root.dataset.autonext };
    players.set(root, p2);
    try {
      const temp__23062__auto__3 = localStorage.getItem(volume_key);
      if (truth_(temp__23062__auto__3)) {
        const x4 = temp__23062__auto__3;
        const x5 = parseFloat(x4);
        if (truth_(isNaN(x5))) {
        } else {
          v1.volume = x5;
          v1.muted = x5 === 0;
        }
      }
    } catch (_6) {
    }
    ;
    root.addEventListener("click", (function(e) {
      const t7 = e.target.closest("[data-vp-open],[data-vp-toggle],[data-vp-action],[data-vp-act],.video-player-option");
      if (not(t7)) {
        if (truth_(p2.open)) {
          return close_panel_BANG_(p2);
        } else {
          if (not(e.target.closest("[data-vp=surface]"))) {
            return null;
          } else {
            if (truth_(auto_muted_QMARK_(p2))) {
              return unmute_BANG_(p2);
            } else {
              if ("else") {
                return toggle_play_BANG_(p2);
              } else {
                return null;
              }
            }
          }
        }
      } else {
        if (truth_(t7.hasAttribute("data-vp-open"))) {
          return open_panel_BANG_(p2, t7.getAttribute("data-vp-open"), t7);
        } else {
          if (truth_(t7.classList.contains("video-player-option"))) {
            return pick_BANG_(p2, t7.closest("[data-vp-panel]"), t7);
          } else {
            if (truth_(t7.hasAttribute("data-vp-toggle"))) {
              return toggle_BANG_2(p2, t7, t7.getAttribute("data-vp-toggle"));
            } else {
              if (truth_(t7.hasAttribute("data-vp-action"))) {
                return emit_BANG_4(p2, { "kind": "action", "id": t7.getAttribute("data-vp-action") });
              } else {
                if ("else") {
                  return act_BANG_(p2, t7.getAttribute("data-vp-act"));
                } else {
                  return null;
                }
              }
            }
          }
        }
      }
      ;
    }));
    root.addEventListener("dblclick", (function(e) {
      if (truth_(e.target.closest("[data-vp=surface]"))) {
        return fullscreen_BANG_(p2);
      }
      ;
    }));
    document.addEventListener("pointerdown", (function(e) {
      const temp__23062__auto__8 = p2.open;
      if (truth_(temp__23062__auto__8)) {
        const o9 = temp__23062__auto__8;
        if (truth_((() => {
          const or__23426__auto__10 = o9.panel.contains(e.target);
          if (truth_(or__23426__auto__10)) {
            return or__23426__auto__10;
          } else {
            return o9.trigger.contains(e.target);
          }
          ;
        })())) {
          return null;
        } else {
          return close_panel_BANG_(p2);
        }
        ;
      }
      ;
    }));
    ["pointermove", "pointerdown", "keydown", "focusin"].forEach((function(t) {
      return root.addEventListener(t, (function() {
        return show_ui_BANG_(p2);
      }), { "passive": true });
    }));
    root.addEventListener("pointerleave", (function(e) {
      if (truth_(e.pointerType === "mouse" && (not(v1.paused) && not(p2.open)))) {
        return flag_BANG_(root, "data-idle", true);
      }
      ;
    }));
    const temp__23062__auto__11 = q2(root, "[data-vp=scrub]");
    if (truth_(temp__23062__auto__11)) {
      const s12 = temp__23062__auto__11;
      wire_scrub_BANG_(p2, s12);
    }
    ;
    const temp__23062__auto__13 = q2(root, "[data-vp=volume]");
    if (truth_(temp__23062__auto__13)) {
      const s14 = temp__23062__auto__13;
      wire_volume_BANG_(p2, s14);
    }
    ;
    ["play", "pause", "volumechange", "ratechange"].forEach((function(t) {
      return v1.addEventListener(t, (function() {
        sync_BANG_3(p2);
        return arm_hide_BANG_(p2);
      }));
    }));
    ["timeupdate", "progress", "durationchange", "seeked"].forEach((function(t) {
      return v1.addEventListener(t, (function() {
        return tick_BANG_(p2);
      }));
    }));
    v1.addEventListener("waiting", (function() {
      return flag_BANG_(root, "data-loading", true);
    }));
    ["playing", "canplay", "pause"].forEach((function(t) {
      return v1.addEventListener(t, (function() {
        return flag_BANG_(root, "data-loading", false);
      }));
    }));
    v1.addEventListener("loadedmetadata", (function() {
      init_tracks_BANG_(p2);
      sync_BANG_3(p2);
      return tick_BANG_(p2);
    }));
    v1.textTracks.addEventListener("addtrack", (function() {
      init_tracks_BANG_(p2);
      return sync_BANG_3(p2);
    }));
    v1.addEventListener("ended", (function() {
      if (truth_((() => {
        const and__23442__auto__15 = has_next_QMARK_(p2);
        if (truth_(and__23442__auto__15)) {
          const and__23442__auto__16 = p2.auto;
          if (truth_(and__23442__auto__16)) {
            return not(p2.dismissed);
          } else {
            return and__23442__auto__16;
          }
          ;
        } else {
          return and__23442__auto__15;
        }
        ;
      })())) {
        return go_next_BANG_(p2);
      }
      ;
    }));
    const temp__23062__auto__17 = q2(root, "[data-vp-act=pip]");
    if (truth_(temp__23062__auto__17)) {
      const b18 = temp__23062__auto__17;
      b18.hidden = not(document.pictureInPictureEnabled);
    }
    ;
    init_tracks_BANG_(p2);
    load_BANG_(p2);
    sync_BANG_3(p2);
    return tick_BANG_(p2);
  };
  var teardown_BANG_ = function(root) {
    const temp__23062__auto__1 = players.get(root);
    if (truth_(temp__23062__auto__1)) {
      const p2 = temp__23062__auto__1;
      destroy_hls_BANG_(p2);
      return players.delete(root);
    }
    ;
  };
  var scan_BANG_4 = function() {
    qa(document, "[data-video-player]").forEach((function(root) {
      const temp__23007__auto__1 = players.get(root);
      if (truth_(temp__23007__auto__1)) {
        const p2 = temp__23007__auto__1;
        if (!_EQ_(p2.src, (() => {
          const or__23426__auto__3 = root.dataset.src;
          if (truth_(or__23426__auto__3)) {
            return or__23426__auto__3;
          } else {
            return "";
          }
          ;
        })())) {
          load_BANG_(p2);
          return sync_BANG_3(p2);
        }
        ;
      } else {
        return init_BANG_12(root);
      }
      ;
    }));
    return Array.from(players.keys()).forEach((function(root) {
      if (truth_(root.isConnected)) {
        return null;
      } else {
        return teardown_BANG_(root);
      }
      ;
    }));
  };
  var boot_BANG_ = function() {
    scan_BANG_4();
    document.addEventListener("keydown", on_document_key);
    document.addEventListener("fullscreenchange", (function() {
      return players.forEach((function(p) {
        close_panel_BANG_(p);
        return sync_BANG_3(p);
      }));
    }));
    const raf1 = atom(null);
    const obs2 = new MutationObserver((function(_) {
      if (truth_(deref(raf1))) {
        return null;
      } else {
        return reset_BANG_(raf1, requestAnimationFrame((function() {
          reset_BANG_(raf1, null);
          return scan_BANG_4();
        })));
      }
      ;
    }));
    return obs2.observe(document.body, { "childList": true, "subtree": true, "attributes": true, "attributeFilter": ["data-src"] });
  };
  window["__uiVideoPlayer"] = (function(root) {
    let temp__23062__auto__571 = players.get(root);
    if (truth_(temp__23062__auto__571)) {
      const p1 = temp__23062__auto__571;
      return { "video": p1.video, "play": (function() {
        return autoplay_BANG_(p1);
      }), "pause": (function() {
        return p1.video.pause();
      }), "seek": (function(t) {
        return p1.video.currentTime = t;
      }), "toast": (function(msg) {
        return toast_BANG_(p1, msg);
      }), "load": (function() {
        load_BANG_(p1);
        return sync_BANG_3(p1);
      }) };
    }
    ;
  });
  if ("loading" === document.readyState) {
    document.addEventListener("DOMContentLoaded", boot_BANG_);
  } else {
    boot_BANG_();
  }
})();
