import { useQuery } from '@tanstack/react-query';
import {
  AlertTriangle,
  ArrowUpRight,
  Boxes,
  CircleDollarSign,
  Clock3,
  PackageCheck,
  ReceiptText,
  Users,
  type LucideIcon
} from 'lucide-react';
import { Badge, Button, Card } from '@boticas/ui-web';
import { dashboardSummaryQuery } from '../api/dashboard.api';

type StatCardProps = {
  label: string;
  value: string;
  detail: string;
  icon: LucideIcon;
  tone: 'teal' | 'blue' | 'amber' | 'rose';
};

const toneClasses = {
  teal: 'bg-teal-50 text-teal-700',
  blue: 'bg-blue-50 text-blue-700',
  amber: 'bg-amber-50 text-amber-700',
  rose: 'bg-rose-50 text-rose-700'
};

function StatCard({ detail, icon: Icon, label, tone, value }: StatCardProps) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <p className="mt-2 text-2xl font-bold tracking-tight text-slate-950">{value}</p>
        </div>
        <div className={`grid size-11 place-items-center rounded-xl ${toneClasses[tone]}`}>
          <Icon className="size-5" aria-hidden="true" />
        </div>
      </div>
      <p className="mt-4 text-xs font-medium text-slate-500">{detail}</p>
    </Card>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value);
}

const recentSales = [
  { id: 'V-1048', customer: 'Ana Torres', total: 'S/ 184.50', status: 'Completada' },
  { id: 'V-1047', customer: 'Carlos Mendoza', total: 'S/ 72.90', status: 'Completada' },
  { id: 'V-1046', customer: 'Venta mostrador', total: 'S/ 35.00', status: 'Completada' },
  { id: 'V-1045', customer: 'Lucía Salazar', total: 'S/ 246.20', status: 'Pendiente' }
];

export function DashboardPage() {
  const { data, isError, isPending } = useQuery(dashboardSummaryQuery);

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-semibold text-teal-700">Domingo, 9 de agosto</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">
            Buenos días, María
          </h1>
          <p className="mt-2 text-sm text-slate-500">
            Este es el estado operativo de tu botica hoy.
          </p>
        </div>
        <Button>
          Nueva venta
          <ArrowUpRight className="size-4" />
        </Button>
      </div>

      {isError ? (
        <Card className="mt-7 border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
          No fue posible obtener el resumen. Verifica la conexión con la API.
        </Card>
      ) : null}

      <section
        aria-label="Indicadores principales"
        className="mt-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
      >
        <StatCard
          label="Ventas de hoy"
          value={isPending ? '—' : formatCurrency(data?.salesToday ?? 0)}
          detail={
            isPending
              ? 'Actualizando...'
              : `${data?.transactionsToday ?? 0} operaciones registradas`
          }
          icon={CircleDollarSign}
          tone="teal"
        />
        <StatCard
          label="Unidades en stock"
          value={isPending ? '—' : String(data?.stockUnits ?? 0)}
          detail="En todos los almacenes"
          icon={Boxes}
          tone="blue"
        />
        <StatCard
          label="Stock bajo"
          value={isPending ? '—' : String(data?.lowStockProducts ?? 0)}
          detail="Productos que requieren atención"
          icon={AlertTriangle}
          tone="amber"
        />
        <StatCard
          label="Lotes por vencer"
          value={isPending ? '—' : String(data?.expiringLots ?? 0)}
          detail="En los próximos 60 días"
          icon={Clock3}
          tone="rose"
        />
      </section>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.5fr_1fr]">
        <Card className="overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 sm:px-6">
            <div>
              <h2 className="font-bold text-slate-900">Ventas recientes</h2>
              <p className="mt-1 text-xs text-slate-500">Últimas operaciones de la sucursal</p>
            </div>
            <Button variant="ghost" size="sm">
              Ver todas
            </Button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase">
                <tr>
                  <th className="px-6 py-3">Venta</th>
                  <th className="px-6 py-3">Cliente</th>
                  <th className="px-6 py-3">Total</th>
                  <th className="px-6 py-3">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recentSales.map((sale) => (
                  <tr key={sale.id} className="hover:bg-slate-50/70">
                    <td className="px-6 py-4 font-semibold text-slate-800">{sale.id}</td>
                    <td className="px-6 py-4 text-slate-600">{sale.customer}</td>
                    <td className="px-6 py-4 font-medium text-slate-800">{sale.total}</td>
                    <td className="px-6 py-4">
                      <Badge tone={sale.status === 'Completada' ? 'success' : 'warning'}>
                        {sale.status}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        <Card className="p-5 sm:p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-bold text-slate-900">Atención requerida</h2>
              <p className="mt-1 text-xs text-slate-500">Prioridades operativas</p>
            </div>
            <PackageCheck className="size-5 text-teal-700" />
          </div>
          <div className="mt-5 space-y-3">
            <div className="flex gap-3 rounded-xl bg-amber-50 p-4">
              <AlertTriangle className="mt-0.5 size-5 shrink-0 text-amber-700" />
              <div>
                <p className="text-sm font-semibold text-amber-950">Reposición de inventario</p>
                <p className="mt-1 text-xs leading-5 text-amber-800">
                  12 productos alcanzaron su stock mínimo.
                </p>
              </div>
            </div>
            <div className="flex gap-3 rounded-xl bg-blue-50 p-4">
              <ReceiptText className="mt-0.5 size-5 shrink-0 text-blue-700" />
              <div>
                <p className="text-sm font-semibold text-blue-950">Comprobantes pendientes</p>
                <p className="mt-1 text-xs leading-5 text-blue-800">
                  3 documentos esperan confirmación.
                </p>
              </div>
            </div>
            <div className="flex gap-3 rounded-xl bg-emerald-50 p-4">
              <Users className="mt-0.5 size-5 shrink-0 text-emerald-700" />
              <div>
                <p className="text-sm font-semibold text-emerald-950">Clientes activos</p>
                <p className="mt-1 text-xs leading-5 text-emerald-800">
                  {data?.activeCustomers ?? 0} clientes compraron este mes.
                </p>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
