import { onCLS, onINP, onLCP, type Metric } from 'web-vitals';

// Local diagnostics only. No endpoint, personal data or URL is collected.
export function observeWebVitals() {
  const report = ({ name, value, rating }: Metric) => {
    const detail = { name, value, rating };
    performance.mark(`erp:${name}`, { detail });
    window.dispatchEvent(new CustomEvent('erp:web-vital', { detail }));
  };
  onCLS(report, { reportAllChanges: true });
  onINP(report, { reportAllChanges: true });
  onLCP(report, { reportAllChanges: true });
}
