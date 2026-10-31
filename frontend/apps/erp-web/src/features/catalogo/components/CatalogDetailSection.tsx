import { useId, type ReactNode } from 'react';
import { Card } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';

export function CatalogDetailSection({
  title,
  description,
  items
}: {
  title: string;
  description: string;
  items: ReadonlyArray<readonly [string, ReactNode]>;
}) {
  const id = useId();
  return (
    <section aria-labelledby={id} className="mt-5">
      <Card className="overflow-hidden">
        <div className="border-b border-neutral-200 bg-neutral-50 px-5 py-4 dark:border-neutral-800 dark:bg-neutral-800/40">
          <h2 id={id} className="font-bold text-neutral-900 dark:text-neutral-50">
            {title}
          </h2>
          <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">{description}</p>
        </div>
        <dl className="grid gap-x-6 gap-y-5 p-5 wrap-anywhere sm:grid-cols-2 lg:grid-cols-3">
          {items.map(([label, value]) => (
            <DatoItem key={label} label={label}>
              {value}
            </DatoItem>
          ))}
        </dl>
      </Card>
    </section>
  );
}
