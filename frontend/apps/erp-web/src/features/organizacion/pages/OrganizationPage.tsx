import { useQuery } from '@tanstack/react-query';
import { Building2, Clock3, MapPin, MonitorSmartphone, Warehouse } from 'lucide-react';
import { Badge, Card } from '@boticas/ui-web';
import { corporateStructureQuery } from '../api/organization.api';

function statusLabel(status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED') {
  if (status === 'ACTIVE') return 'Activo';
  if (status === 'SUSPENDED') return 'Suspendido';
  return 'Inactivo';
}

function statusTone(status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED') {
  if (status === 'ACTIVE') return 'success' as const;
  if (status === 'SUSPENDED') return 'warning' as const;
  return 'neutral' as const;
}

function formatCutoff(value: string) {
  return new Intl.DateTimeFormat('es-PE', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'America/Lima'
  }).format(new Date(value));
}

export function OrganizationPage() {
  const { data, isError, isPending } = useQuery(corporateStructureQuery);
  const establishments = data?.companies.flatMap((company) => company.establishments) ?? [];
  const warehouseCount = establishments.reduce(
    (total, establishment) => total + establishment.warehouses.length,
    0
  );
  const cashRegisterCount = establishments.reduce(
    (total, establishment) => total + establishment.cashRegisters.length,
    0
  );

  return (
    <div className="mx-auto max-w-7xl">
      <div>
        <p className="text-sm font-semibold text-teal-700">Foundation / Core maestro</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Organización</h1>
        <p className="mt-2 text-sm text-slate-500">
          Estructura corporativa efectiva dentro del ámbito autorizado de la sesión.
        </p>
      </div>

      {isError ? (
        <Card className="mt-7 border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
          No fue posible obtener la estructura corporativa. Verifica la conexión y tu ámbito de
          acceso.
        </Card>
      ) : null}

      <section
        aria-label="Resumen de estructura"
        className="mt-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
      >
        {[
          { label: 'Empresas', value: data?.companies.length ?? 0, icon: Building2 },
          { label: 'Establecimientos', value: establishments.length, icon: MapPin },
          { label: 'Almacenes', value: warehouseCount, icon: Warehouse },
          { label: 'Cajas', value: cashRegisterCount, icon: MonitorSmartphone }
        ].map(({ icon: Icon, label, value }) => (
          <Card key={label} className="flex items-center justify-between p-5">
            <div>
              <p className="text-sm font-medium text-slate-500">{label}</p>
              <p className="mt-2 text-2xl font-bold text-slate-950">{isPending ? '—' : value}</p>
            </div>
            <div className="grid size-11 place-items-center rounded-xl bg-teal-50 text-teal-700">
              <Icon className="size-5" aria-hidden="true" />
            </div>
          </Card>
        ))}
      </section>

      {data ? (
        <div className="mt-6 space-y-5">
          <p className="flex items-center gap-2 text-xs font-medium text-slate-500">
            <Clock3 className="size-4" aria-hidden="true" />
            Fecha de corte: {formatCutoff(data.asOf)}
          </p>

          {data.companies.map((company) => (
            <Card key={company.id} className="overflow-hidden">
              <div className="flex flex-col gap-3 border-b border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
                <div>
                  <h2 className="font-bold text-slate-950">
                    {company.tradeName ?? company.legalName}
                  </h2>
                  <p className="mt-1 text-xs text-slate-500">{company.legalName}</p>
                </div>
                <Badge tone={statusTone(company.status)}>{statusLabel(company.status)}</Badge>
              </div>

              <div className="divide-y divide-slate-100">
                {company.establishments.map((establishment) => (
                  <article key={establishment.id} className="p-5 sm:p-6">
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <p className="text-xs font-bold tracking-wide text-teal-700 uppercase">
                          {establishment.code}
                        </p>
                        <h3 className="mt-1 font-semibold text-slate-900">{establishment.name}</h3>
                        <p className="mt-1 text-xs text-slate-500">{establishment.timeZone}</p>
                      </div>
                      <Badge tone={statusTone(establishment.status)}>
                        {statusLabel(establishment.status)}
                      </Badge>
                    </div>

                    <div className="mt-4 grid gap-3 lg:grid-cols-2">
                      <div className="rounded-xl bg-slate-50 p-4">
                        <p className="flex items-center gap-2 text-xs font-bold text-slate-600 uppercase">
                          <Warehouse className="size-4" aria-hidden="true" /> Almacenes
                        </p>
                        <p className="mt-2 text-sm text-slate-700">
                          {establishment.warehouses.map((warehouse) => warehouse.name).join(', ') ||
                            'Sin almacenes visibles'}
                        </p>
                      </div>
                      <div className="rounded-xl bg-slate-50 p-4">
                        <p className="flex items-center gap-2 text-xs font-bold text-slate-600 uppercase">
                          <MonitorSmartphone className="size-4" aria-hidden="true" /> Cajas
                        </p>
                        <p className="mt-2 text-sm text-slate-700">
                          {establishment.cashRegisters
                            .map((register) => register.name)
                            .join(', ') || 'Sin cajas visibles'}
                        </p>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </Card>
          ))}
        </div>
      ) : null}
    </div>
  );
}
