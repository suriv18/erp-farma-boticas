import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./apps/erp-web/src/test/setup-tests.ts'],
    include: ['apps/**/*.test.{ts,tsx}', 'packages/**/*.test.{ts,tsx}'],
    coverage: {
      reporter: ['text', 'html']
    }
  }
});
