/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // same origin in development: the browser talks to Vite, Vite forwards /api to the Spring Boot API
    proxy: { '/api': 'http://localhost:8080' },
    // the project lives under /mnt/c (OneDrive), which emits no inotify events
    watch: { usePolling: true },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    restoreMocks: true,
  },
});
