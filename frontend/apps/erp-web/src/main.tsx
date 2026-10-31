import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';
import { environment } from './app/environment';
import './styles.css';
import { observeWebVitals } from './shared/lib/web-vitals';

async function enableApiMocking() {
  if (!import.meta.env.DEV || environment.VITE_API_MODE !== 'mock') return;
  const { worker } = await import('./test/mocks/browser');
  await worker.start({ onUnhandledRequest: 'bypass' });
}

async function bootstrap() {
  observeWebVitals();
  await enableApiMocking();
  const rootElement = document.getElementById('root');
  if (!rootElement) throw new Error('No se encontró el elemento raíz de la aplicación.');

  createRoot(rootElement).render(
    <StrictMode>
      <App />
    </StrictMode>
  );
}

void bootstrap();
