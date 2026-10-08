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
  tone: 'primary' | 'neutral' | 'warning' | 'danger';
};

const toneClasses = {
  primary: 'bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300',
  neutral: 'bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300',
  warning: 'bg-warning-50 text-warning-700 dark:bg-warning-900/40 dark:text-warning-300',
  danger: 'bg-danger-50 text-danger-700 dark:bg-danger-900/40 dark:text-danger-300'
};

function StatCard({ detail, icon: Icon, label, tone, value }: StatCardProps) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-neutral-500 dark:text-neutral-400">{label}</p>
          <p className="mt-2 text-2xl font-bold tracking-tight text-neutral-950 dark:text-white">
            {value}
          </p>
        </div>
        <div className={`grid size-11 place-items-center rounded-xl ${toneClasses[tone]}`}>
          <Icon className="size-5" aria-hidden="true" />
        </div>
      </div>
      <p className="mt-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">{detail}</p>
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
          <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
            Domingo, 9 de agosto
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
            Buenos días, María
          </h1>
          <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
            Este es el estado operativo de tu botica hoy.
          </p>
        </div>
        <Button>
          Nueva venta
          <ArrowUpRight className="size-4" />
        </Button>
      </div>

      {isError ? (
        <Card className="border-danger-200 bg-danger-50 text-danger-800 dark:border-danger-800 dark:bg-danger-900/30 dark:text-danger-300 mt-7 p-4 text-sm">
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
          tone="primary"
        />
        <StatCard
          label="Unidades en stock"
          value={isPending ? '—' : String(data?.stockUnits ?? 0)}
          detail="En todos los almacenes"
          icon={Boxes}
          tone="neutral"
        />
        <StatCard
          label="Stock bajo"
          value={isPending ? '—' : String(data?.lowStockProducts ?? 0)}
          detail="Productos que requieren atención"
          icon={AlertTriangle}
          tone="warning"
        />
        <StatCard
          label="Lotes por vencer"
          value={isPending ? '—' : String(data?.expiringLots ?? 0)}
          detail="En los próximos 60 días"
          icon={Clock3}
          tone="danger"
        />
      </section>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.5fr_1fr]">
        <Card className="overflow-hidden">
          <div className="flex items-center justify-between border-b border-neutral-100 px-5 py-4 sm:px-6 dark:border-neutral-800">
            <div>
              <h2 className="font-bold text-neutral-900 dark:text-neutral-50">Ventas recientes</h2>
              <p className="mt-1 text-xs text-neutral-500 dark:text-neutral-400">
                Últimas operaciones de la sucursal
              </p>
            </div>
            <Button variant="ghost" size="sm">
              Ver todas
            </Button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-neutral-50 text-xs font-semibold text-neutral-500 uppercase dark:bg-neutral-800/60 dark:text-neutral-400">
                <tr>
                  <th className="px-6 py-3">Venta</th>
                  <th className="px-6 py-3">Cliente</th>
                  <th className="px-6 py-3">Total</th>
                  <th className="px-6 py-3">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {recentSales.map((sale) => (
                  <tr key={sale.id} className="hover:bg-neutral-50/70 dark:hover:bg-neutral-800/40">
                    <td className="px-6 py-4 font-semibold text-neutral-800 dark:text-neutral-100">
                      {sale.id}
                    </td>
                    <td className="px-6 py-4 text-neutral-600 dark:text-neutral-300">
                      {sale.customer}
                    </td>
                    <td className="px-6 py-4 font-medium text-neutral-800 dark:text-neutral-100">
                      {sale.total}
                    </td>
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
              <h2 className="font-bold text-neutral-900 dark:text-neutral-50">
                Atención requerida
              </h2>
              <p className="mt-1 text-xs text-neutral-500 dark:text-neutral-400">
                Prioridades operativas
              </p>
            </div>
            <PackageCheck className="text-primary-700 dark:text-primary-400 size-5" />
          </div>
          <div className="mt-5 space-y-3">
            <div className="bg-warning-50 dark:bg-warning-900/30 flex gap-3 rounded-xl p-4">
              <AlertTriangle className="text-warning-700 dark:text-warning-400 mt-0.5 size-5 shrink-0" />
              <div>
                <p className="text-warning-950 dark:text-warning-100 text-sm font-semibold">
                  Reposición de inventario
                </p>
                <p className="text-warning-800 dark:text-warning-300 mt-1 text-xs leading-5">
                  12 productos alcanzaron su stock mínimo.
                </p>
              </div>
            </div>
            <div className="bg-neutral-100 dark:bg-neutral-800/60 flex gap-3 rounded-xl p-4">
              <ReceiptText className="text-neutral-600 dark:text-neutral-400 mt-0.5 size-5 shrink-0" />
              <div>
                <p className="text-neutral-900 dark:text-neutral-100 text-sm font-semibold">
                  Comprobantes pendientes
                </p>
                <p className="text-neutral-700 dark:text-neutral-300 mt-1 text-xs leading-5">
                  3 documentos esperan confirmación.
                </p>
              </div>
            </div>
            <div className="bg-success-50 dark:bg-success-900/30 flex gap-3 rounded-xl p-4">
              <Users className="text-success-700 dark:text-success-400 mt-0.5 size-5 shrink-0" />
              <div>
                <p className="text-success-950 dark:text-success-100 text-sm font-semibold">
                  Clientes activos
                </p>
                <p className="text-success-800 dark:text-success-300 mt-1 text-xs leading-5">
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
