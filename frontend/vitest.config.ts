import { readFileSync } from 'node:fs';
import { defineConfig } from 'vitest/config';

const coverageBaseline = readFileSync('./coverage-baseline.txt', 'utf-8')
  .split('\n')
  .map((line) => line.trim())
  .filter(Boolean);

export default defineConfig({
  test: {
    coverage: {
      reporter: ['text', 'html'],
      thresholds: {
        lines: 100,
        branches: 100,
        functions: 100,
        statements: 100
      },
      exclude: [
        '**/main.tsx',
        '**/src/test/mocks/**',
        '**/setup-tests.ts',
        '**/test-setup.ts',
        '**/*.config.ts',
        // Baseline congelado: archivos existentes con cobertura <100% al momento de activar
        // el gate. Esta lista no crece — todo archivo nuevo cae bajo el gate al 100%.
        ...coverageBaseline
      ]
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
