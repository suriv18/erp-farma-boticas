import { Boxes, Filter, Plus, Search } from 'lucide-react';
import { Badge, Button, Card } from '@boticas/ui-web';

const inventoryRows = [
  { product: 'Paracetamol 500 mg', sku: 'MED-00124', stock: 248, status: 'Disponible' },
  { product: 'Ibuprofeno 400 mg', sku: 'MED-00316', stock: 84, status: 'Disponible' },
  { product: 'Amoxicilina 500 mg', sku: 'MED-00642', stock: 12, status: 'Stock bajo' },
  { product: 'Alcohol medicinal 96°', sku: 'HIG-00108', stock: 6, status: 'Stock bajo' }
];

export function InventoryPage() {
  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-semibold text-teal-700">Operaciones</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Inventario</h1>
          <p className="mt-2 text-sm text-slate-500">
            Stock por producto, almacén, lote y vencimiento.
          </p>
        </div>
        <Button>
          <Plus className="size-4" />
          Registrar ajuste
        </Button>
      </div>

      <Card className="mt-7 overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative max-w-md flex-1">
            <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              placeholder="Buscar por producto o SKU"
              className="h-10 w-full rounded-xl border border-slate-200 pr-3 pl-10 text-sm outline-none focus:border-teal-600 focus:ring-3 focus:ring-teal-100"
            />
          </div>
          <Button variant="secondary" size="sm">
            <Filter className="size-4" />
            Filtros
          </Button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase">
              <tr>
                <th className="px-6 py-3">Producto</th>
                <th className="px-6 py-3">SKU</th>
                <th className="px-6 py-3">Stock</th>
                <th className="px-6 py-3">Estado</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {inventoryRows.map((row) => (
                <tr key={row.sku} className="hover:bg-slate-50/70">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="grid size-9 place-items-center rounded-lg bg-teal-50 text-teal-700">
                        <Boxes className="size-4" />
                      </div>
                      <span className="font-semibold text-slate-800">{row.product}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-slate-500">{row.sku}</td>
                  <td className="px-6 py-4 font-semibold text-slate-800">{row.stock}</td>
                  <td className="px-6 py-4">
                    <Badge tone={row.status === 'Disponible' ? 'success' : 'warning'}>
                      {row.status}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
