import { expect, open, test } from "nsh/test";

test("cljs.core runs under the hardened engine and renders", async () => {
  const app = await open("./App.tsx");
  expect(app.texts()).toContain("hi from ClojureScript via clojurific");
});
