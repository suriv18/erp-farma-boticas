import type { ReactNode } from 'react';
import { Card } from '../card/Card';
import { Pagination, type PaginationProps } from '../pagination/Pagination';

export type DataTableColumn<T> = {
  header: string;
  cell: (row: T, index: number) => ReactNode;
};

export type DataTableProps<T> = {
  columns: DataTableColumn<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  emptyMessage: string;
  isLoading?: boolean;
  isError?: boolean;
  errorMessage?: string;
  startIndex?: number;
  pagination?: PaginationProps;
};

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  emptyMessage,
  isLoading = false,
  isError = false,
  errorMessage = 'No se pudo cargar la información.',
  startIndex = 0,
  pagination
}: DataTableProps<T>) {
  return (
    <Card className="overflow-hidden">
      <div className="overflow-x-auto" role="region" aria-label="Tabla de resultados" tabIndex={0}>
        <table className="w-full text-left text-sm">
          <thead className="border-b border-neutral-100 bg-neutral-50 dark:border-neutral-800 dark:bg-neutral-800/60">
            <tr>
              {columns.map((column) => (
                <th
                  scope="col"
                  key={column.header}
                  className="px-4 py-3 font-semibold text-neutral-600 dark:text-neutral-300"
                >
                  {column.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
            {isLoading ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-6 text-center text-neutral-500 dark:text-neutral-400"
                >
                  Cargando…
                </td>
              </tr>
            ) : null}
            {!isLoading && isError ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="text-danger-700 dark:text-danger-400 px-4 py-6 text-center"
                >
                  {errorMessage}
                </td>
              </tr>
            ) : null}
            {!isLoading && !isError && rows.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-6 text-center text-neutral-500 dark:text-neutral-400"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : null}
            {!isLoading && !isError
              ? rows.map((row, index) => (
                  <tr
                    key={rowKey(row)}
                    className="hover:bg-neutral-50 dark:hover:bg-neutral-800/40"
                  >
                    {columns.map((column) => (
                      <td
                        key={column.header}
                        className="px-4 py-3 text-neutral-700 dark:text-neutral-200"
                      >
                        {column.cell(row, startIndex + index)}
                      </td>
                    ))}
                  </tr>
                ))
              : null}
          </tbody>
        </table>
      </div>
      {pagination ? (
        <Pagination
          {...pagination}
          disabled={isLoading || isError || Boolean(pagination.disabled)}
        />
      ) : null}
    </Card>
  );
}
