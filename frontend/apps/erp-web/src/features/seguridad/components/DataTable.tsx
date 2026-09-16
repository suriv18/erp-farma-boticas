import type { ReactNode } from 'react';
import { Card } from '@boticas/ui-web';

export type DataTableColumn<T> = {
  header: string;
  cell: (row: T) => ReactNode;
};

export type DataTableProps<T> = {
  columns: DataTableColumn<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  emptyMessage: string;
  isLoading?: boolean;
  isError?: boolean;
  errorMessage?: string;
};

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  emptyMessage,
  isLoading = false,
  isError = false,
  errorMessage = 'No se pudo cargar la información.'
}: DataTableProps<T>) {
  return (
    <Card className="overflow-hidden">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-slate-100 bg-slate-50">
          <tr>
            {columns.map((column) => (
              <th key={column.header} className="px-4 py-3 font-semibold text-slate-600">
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {isLoading ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-6 text-center text-slate-500">
                Cargando…
              </td>
            </tr>
          ) : null}
          {!isLoading && isError ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-6 text-center text-rose-700">
                {errorMessage}
              </td>
            </tr>
          ) : null}
          {!isLoading && !isError && rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-6 text-center text-slate-500">
                {emptyMessage}
              </td>
            </tr>
          ) : null}
          {!isLoading && !isError
            ? rows.map((row) => (
                <tr key={rowKey(row)} className="hover:bg-slate-50">
                  {columns.map((column) => (
                    <td key={column.header} className="px-4 py-3 text-slate-700">
                      {column.cell(row)}
                    </td>
                  ))}
                </tr>
              ))
            : null}
        </tbody>
      </table>
    </Card>
  );
}
