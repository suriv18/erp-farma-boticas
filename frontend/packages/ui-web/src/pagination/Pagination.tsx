import { useEffect } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '../button/Button';

export type PaginationProps = {
  page: number;
  size: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onSizeChange?: (size: number) => void;
  disabled?: boolean;
  isLoading?: boolean;
  itemLabel?: string;
};

function pageItems(current: number, total: number): (number | string)[] {
  if (total <= 7) return Array.from({ length: total }, (_, index) => index);
  const start = Math.max(1, Math.min(current - 1, total - 4));
  const end = start + 2;
  return [
    0,
    ...(start === 2 ? [1] : start > 2 ? ['start-gap'] : []),
    start,
    start + 1,
    end,
    ...(end === total - 3 ? [total - 2] : end < total - 3 ? ['end-gap'] : []),
    total - 1
  ];
}

/** Controlled, zero-based pagination. The consumer owns fetching. */
export function Pagination({
  page,
  size,
  totalElements,
  onPageChange,
  onSizeChange,
  disabled = false,
  isLoading = false,
  itemLabel = 'registros'
}: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const currentPage = isLoading ? Math.max(0, page) : Math.min(Math.max(0, page), totalPages - 1);
  const blocked = disabled || isLoading;
  useEffect(() => {
    if (!blocked && currentPage !== page) onPageChange(currentPage);
  }, [currentPage, blocked, onPageChange, page]);
  const from = totalElements === 0 ? 0 : currentPage * size + 1;
  const to = Math.min(totalElements, (currentPage + 1) * size);
  return (
    <nav
      aria-label="Paginación"
      className="flex flex-col gap-3 border-t border-neutral-200 bg-neutral-50/60 px-4 py-3 text-sm text-neutral-600 xl:flex-row xl:flex-wrap xl:items-center xl:justify-between dark:border-neutral-800 dark:bg-neutral-900/60 dark:text-neutral-300"
    >
      <div className="flex flex-wrap items-center justify-between gap-x-5 gap-y-2 xl:justify-start">
        {onSizeChange ? (
          <label className="flex items-center gap-2">
            Filas por página
            <select
              aria-label="Filas por página"
              value={size}
              disabled={blocked}
              onChange={(event) => {
                onSizeChange(Number(event.target.value));
                onPageChange(0);
              }}
              className="focus-visible:outline-primary-600 h-11 rounded-lg border border-neutral-200 bg-white px-3 font-medium text-neutral-900 focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            >
              {[...new Set([20, 50, 100, size])]
                .sort((a, b) => a - b)
                .map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
            </select>
          </label>
        ) : null}
        <span role="status" aria-atomic="true" className="tabular-nums">
          {isLoading
            ? 'Cargando resultados…'
            : totalElements === 0
              ? '0 resultados'
              : `${from}–${to} de ${totalElements} ${itemLabel}`}
        </span>
      </div>
      <div className="flex items-center justify-between gap-1 sm:justify-end sm:gap-2">
        <Button
          variant="ghost"
          className="shrink-0 px-3 disabled:opacity-40"
          disabled={blocked || currentPage <= 0}
          onClick={() => onPageChange(currentPage - 1)}
        >
          <ChevronLeft className="size-4" aria-hidden="true" />
          <span className="sr-only lg:not-sr-only">Anterior</span>
        </Button>
        <span className="whitespace-nowrap tabular-nums lg:sr-only">
          {isLoading ? `Página ${currentPage + 1}` : `Página ${currentPage + 1} de ${totalPages}`}
        </span>
        <div className="hidden items-center gap-1 lg:flex">
          {isLoading ? (
            <span className="px-3 tabular-nums" aria-hidden="true">
              {currentPage + 1}
            </span>
          ) : (
            pageItems(currentPage, totalPages).map((item) =>
              typeof item === 'string' ? (
                <span key={item} className="px-1 text-neutral-400" aria-hidden="true">
                  …
                </span>
              ) : (
                <Button
                  key={item}
                  variant={item === currentPage ? 'primary' : 'ghost'}
                  className="min-w-11 shrink-0 px-3 tabular-nums disabled:opacity-50"
                  aria-label={`Página ${item + 1}`}
                  aria-current={item === currentPage ? 'page' : undefined}
                  disabled={blocked || totalElements === 0}
                  onClick={() => {
                    if (item !== currentPage) onPageChange(item);
                  }}
                >
                  {item + 1}
                </Button>
              )
            )
          )}
        </div>
        <Button
          variant="ghost"
          className="shrink-0 px-3 disabled:opacity-40"
          disabled={blocked || currentPage + 1 >= totalPages}
          onClick={() => onPageChange(currentPage + 1)}
        >
          <span className="sr-only lg:not-sr-only">Siguiente</span>
          <ChevronRight className="size-4" aria-hidden="true" />
        </Button>
      </div>
    </nav>
  );
}
