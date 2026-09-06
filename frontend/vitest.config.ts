import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    coverage: {
      reporter: ['text', 'html']
    },
    projects: [
      {
        test: {
          name: 'erp-web',
          environment: 'jsdom',
          environmentOptions: {
            jsdom: {
              url: 'http://localhost/'
            }
          },
          globals: true,
          setupFiles: ['./apps/erp-web/src/test/setup-tests.ts'],
          include: ['apps/erp-web/**/*.test.{ts,tsx}']
        }
      },
      {
        test: {
          name: 'ui-web',
          environment: 'jsdom',
          globals: true,
          setupFiles: ['./packages/ui-web/src/test-setup.ts'],
          include: ['packages/ui-web/**/*.test.{ts,tsx}']
        }
      },
      {
        test: {
          name: 'api-client',
          environment: 'jsdom',
          globals: true,
          include: ['packages/api-client/**/*.test.{ts,tsx}']
        }
      }
    ]
  }
});
