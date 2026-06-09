import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  base: './',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/stock': {
        target: 'http://localhost:18888',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/stock/, '')
      },
      '/chat': {
        target: 'http://localhost:5001',
        changeOrigin: true
      }
    }
  }
})