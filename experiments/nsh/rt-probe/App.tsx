import { mount, Text, View } from "nsh";
import { append, element, insert, prop } from "nsh:rt";

// Build a tree without JSX, the way a hiccup interpreter would.
function fromRt() {
  const v = element("view");
  prop(v, "class", "rounded-lg bg-blue-600 px-4 py-2");
  const t = element("text");
  insert(t, () => "built with nsh:rt");
  append(v, t);
  return v;
}

// Call the primitives as plain functions.
function fromCalls() {
  return View({ class: "rounded-lg bg-green-600 px-4 py-2", children: Text({ children: "built by calling View()/Text()" }) });
}

mount(() => {
  const root = element("view");
  prop(root, "style", { flex: 1, padding: 24, gap: 12 });
  append(root, fromRt());
  append(root, fromCalls());
  return root;
});
