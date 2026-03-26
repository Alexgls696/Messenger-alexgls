import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    global: 'window',
  },

  server: {
    port: 5173,
    allowedHosts: true,
    proxy: {
      '^/(auth|api)': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },

      '/ws-chat': {
        target: 'http://127.0.0.1:8080',
        ws: true,
        changeOrigin: true,
      },
    }
  }
})