import { defineConfig } from "vite";

// Dev only: xi's chrome design mode holds the page reload while its popover is
// open (config/xi/mcp-servers/chrome/vite in the dotfiles; skipped without them).
const xiDesignReload = import(
  `${process.env.HOME}/.config/dotfiles/config/xi/mcp-servers/chrome/vite/xi-design-reload.mjs`
).then((m) => m.default(), () => null);

export default defineConfig({
  plugins: [xiDesignReload],
  root: ".",
  publicDir: "public",
  // Production build is served by the hiccup server under /squint/ —
  // relative asset URLs so the subpath works. Deterministic (unhashed)
  // names keep the committed dist/ diff-friendly; public/ is generated
  // server-side by bb build-theme, so don't duplicate it into dist.
  base: "./",
  build: {
    copyPublicDir: false,
    rollupOptions: {
      output: {
        entryFileNames: "assets/[name].js",
        chunkFileNames: "assets/[name].js",
        assetFileNames: "assets/[name][extname]",
      },
    },
  },
  server: {
    port: 3002,
    // Bind all interfaces so the dev server is reachable over LAN/Tailscale,
    // not just localhost. allowedHosts: true disables Vite's Host-header check
    // so MagicDNS hostnames (*.ts.net) also work.
    host: true,
    allowedHosts: true,
  },
});
