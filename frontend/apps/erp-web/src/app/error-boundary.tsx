import { Component, type ErrorInfo, type PropsWithChildren, type ReactNode } from 'react';
import { Button, Card } from '@boticas/ui-web';

type ErrorBoundaryState = { hasError: boolean };

export class AppErrorBoundary extends Component<PropsWithChildren, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Error no controlado en la aplicación', error, info.componentStack);
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return (
        <main className="grid min-h-screen place-items-center bg-neutral-50 p-6 dark:bg-neutral-950">
          <Card className="max-w-lg p-8 text-center">
            <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
              ERP Boticas
            </p>
            <h1 className="mt-2 text-2xl font-bold text-neutral-950 dark:text-white">
              No pudimos cargar esta vista
            </h1>
            <p className="mt-3 text-sm leading-6 text-neutral-600 dark:text-neutral-300">
              Vuelve a intentarlo. Si el problema continúa, comunica el incidente al equipo de
              soporte.
            </p>
            <Button className="mt-6" onClick={() => window.location.reload()}>
              Recargar aplicación
            </Button>
          </Card>
        </main>
      );
    }

    return this.props.children;
  }
}
