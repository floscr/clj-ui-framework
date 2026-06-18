import { defineConfig } from "vite";

export default defineConfig({
  root: ".",
  publicDir: "public",
  server: {
    port: 3002,
    // Bind all interfaces so the dev server is reachable over LAN/Tailscale,
    // not just localhost. allowedHosts: true disables Vite's Host-header check
    // so MagicDNS hostnames (*.ts.net) also work.
    host: true,
    allowedHosts: true,
  },
});
