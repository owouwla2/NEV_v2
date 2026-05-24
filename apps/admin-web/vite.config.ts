import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 8120,
    proxy: {
      // 后端 API 直接代理过去（避开 CORS）
      '/auth':             { target: 'http://localhost:9280', changeOrigin: true },
      '/system':           { target: 'http://localhost:9280', changeOrigin: true },
      '/producer':         { target: 'http://localhost:9280', changeOrigin: true },
      '/distributor':      { target: 'http://localhost:9280', changeOrigin: true },
      '/retailer':         { target: 'http://localhost:9280', changeOrigin: true },
      '/merchant':         { target: 'http://localhost:9280', changeOrigin: true },
      '/consumer':         { target: 'http://localhost:9280', changeOrigin: true },
      '/recycler':         { target: 'http://localhost:9280', changeOrigin: true },
      '/admin':            { target: 'http://localhost:9280', changeOrigin: true },
      '/public':           { target: 'http://localhost:9280', changeOrigin: true },
    },
  },
})
