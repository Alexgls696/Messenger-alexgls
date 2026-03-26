import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
   define: {
    global: 'window',
  },
  server: {
    port: 5173,
    host: '127.0.0.1', 
    allowedHosts: true,
    proxy: {
      '^/(auth|api|ws-chat)': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true,
        // ДОБАВЬТЕ ЭТО:
        configure: (proxy, _options) => {
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            // Удаляем заголовок, который может заставлять Gateway закрывать соединение
            proxyReq.setHeader('Connection', 'keep-alive');
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            // Гарантируем, что браузер получит keep-alive от прокси Vite
            proxyRes.headers['connection'] = 'keep-alive';
          });
        },
      },
    }
  }
})