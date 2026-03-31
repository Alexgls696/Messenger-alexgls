import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
   define: {
    global: 'window',
  },
  server: {
    port: 5173,
    host: '0.0.0.0', 
    allowedHosts: true,
    proxy: {
      '^/(auth|api|ws-chat)': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true,
        configure: (proxy, _options) => {
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            proxyReq.setHeader('Connection', 'keep-alive');
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            proxyRes.headers['connection'] = 'keep-alive';
          });
        },
      },
    }
  }
})