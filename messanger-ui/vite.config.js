import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'window',
  },
  server: {
    host: true, // или '0.0.0.0'
    port: 5173,
    allowedHosts: ['ctfgnk-84-42-72-25.ru.tuna.am'],
    proxy: {
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    }
  },
})
