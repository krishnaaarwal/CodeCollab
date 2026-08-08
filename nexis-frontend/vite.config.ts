import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { fileURLToPath, URL } from 'node:url';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Supplementary shim for the production build path (rollup processes
  // node_modules through the same pipeline as app code, unlike dev's
  // separate esbuild dependency pre-bundling step, where this didn't
  // reliably apply). The actual fix for dev is the inline script in
  // index.html, which runs before the module graph loads at all.
  define: {
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
  },
});
