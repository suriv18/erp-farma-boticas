export type PaginationProps = {
  page: number;
  size: number;
  totalElements: number;
  onPageChange: (page: number) => void;
};

export function Pagination({ page, size, totalElements, onPageChange }: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min(totalElements, (page + 1) * size);

  return (
    <div className="flex items-center justify-between px-4 py-3 text-sm text-neutral-600 dark:text-neutral-300">
      <span>
        {from}–{to} de {totalElements}
      </span>
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
          className="h-9 rounded-lg border border-neutral-200 bg-white px-3 font-semibold text-neutral-700 shadow-sm transition-colors hover:border-neutral-300 hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800"
        >
          Anterior
        </button>
        <button
          type="button"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
          className="h-9 rounded-lg border border-neutral-200 bg-white px-3 font-semibold text-neutral-700 shadow-sm transition-colors hover:border-neutral-300 hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800"
        >
          Siguiente
        </button>
      </div>
    </div>
  );
}
