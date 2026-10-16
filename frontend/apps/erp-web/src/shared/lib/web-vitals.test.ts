import { onCLS, onINP, onLCP, type Metric } from 'web-vitals';
import { observeWebVitals } from './web-vitals';

vi.mock('web-vitals', () => ({ onCLS: vi.fn(), onINP: vi.fn(), onLCP: vi.fn() }));

it('registra métricas locales sin datos personales ni peticiones de red', () => {
  const mark = vi.spyOn(performance, 'mark');
  const listener = vi.fn();
  window.addEventListener('erp:web-vital', listener);
  observeWebVitals();
  for (const [hook, name] of [
    [onCLS, 'CLS'],
    [onINP, 'INP'],
    [onLCP, 'LCP']
  ] as const) {
    const report = vi.mocked(hook).mock.calls[0]![0] as (metric: Metric) => void;
    report({ name, value: 1, rating: 'good' } as Metric);
    expect(mark).toHaveBeenCalledWith(`erp:${name}`, {
      detail: { name, value: 1, rating: 'good' }
    });
  }
  expect(listener).toHaveBeenCalledTimes(3);
  window.removeEventListener('erp:web-vital', listener);
});
