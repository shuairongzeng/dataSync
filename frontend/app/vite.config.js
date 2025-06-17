import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path' // Import path module for alias resolution

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src') // Correct alias for Vite
    }
  },
  test: { // Vitest configuration
    globals: true,
    environment: 'jsdom', // Changed to jsdom
    deps: {
      inline: ['element-plus'], // To handle Element Plus components during tests
    },
  },
})
