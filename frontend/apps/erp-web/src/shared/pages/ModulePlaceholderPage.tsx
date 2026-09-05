import { ArrowLeft, Construction } from 'lucide-react';
import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

export function ModulePlaceholderPage({
  description,
  title
}: {
  description: string;
  title: string;
}) {
  return (
    <div className="mx-auto max-w-7xl">
      <p className="text-sm font-semibold text-teal-700">Módulo ERP</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">{title}</h1>
      <Card className="mt-7 grid min-h-80 place-items-center p-8 text-center">
        <div className="max-w-md">
          <div className="mx-auto grid size-14 place-items-center rounded-2xl bg-teal-50 text-teal-700">
            <Construction className="size-6" />
          </div>
          <h2 className="mt-5 text-xl font-bold text-slate-900">Módulo preparado</h2>
          <p className="mt-2 text-sm leading-6 text-slate-500">{description}</p>
          <Link
            to="/dashboard"
            className="mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 shadow-sm transition-colors hover:border-slate-300 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-500"
          >
            <ArrowLeft className="size-4" />
            Volver al resumen
          </Link>
        </div>
      </Card>
    </div>
  );
}
