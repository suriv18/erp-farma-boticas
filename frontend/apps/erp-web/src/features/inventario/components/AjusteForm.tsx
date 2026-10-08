import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { FormError } from '../../../shared/components/FormError';
import { SelectField, TextField } from '../../../shared/components/FormFields';
import { TIPOS_AJUSTE, type Posicion } from '../api/inventario.types';
import { AJUSTE_FORM_VACIO, ETIQUETAS_TIPO_AJUSTE, ajusteDesdePosicion } from '../lib/ajuste';
import type { OpcionAlmacen } from '../lib/estructura';
import { ajusteSchema, type AjusteFormValues } from '../schemas/ajuste.schema';
import { SkuSelect } from './SkuSelect';

type AjusteFormProps = {
  posicion: Posicion | null;
  almacenes: OpcionAlmacen[];
  isSubmitting: boolean;
  error: string | null;
  onSubmit: (values: AjusteFormValues) => void;
};

export function AjusteForm({
  posicion,
  almacenes,
  isSubmitting,
  error,
  onSubmit
}: AjusteFormProps) {
  const [busquedaSku, setBusquedaSku] = useState('');
  const {
    formState: { errors },
    handleSubmit,
    register,
    setValue,
    control
  } = useForm<AjusteFormValues>({
    defaultValues: posicion ? ajusteDesdePosicion(posicion) : AJUSTE_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(ajusteSchema)
  });
  const skuId = useWatch({ control, name: 'skuId' });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      {posicion ? (
        <dl className="grid gap-4 sm:grid-cols-2">
          <DatoItem label="Lote">{posicion.numeroLote}</DatoItem>
          <DatoItem label="Stock físico">{posicion.cantidadFisica}</DatoItem>
        </dl>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField
            id="ajuste-almacen"
            label="Almacén"
            error={errors.almacenId?.message}
            {...register('almacenId')}
          >
            <option value="">Selecciona un almacén</option>
            {almacenes.map((almacen) => (
              <option key={almacen.id} value={almacen.id}>
                {almacen.establecimiento} — {almacen.nombre}
              </option>
            ))}
          </SelectField>
          <TextField
            id="ajuste-buscar-sku"
            label="Buscar SKU"
            value={busquedaSku}
            onChange={(event) => setBusquedaSku(event.target.value)}
          />
          <SkuSelect
            id="ajuste-sku"
            label="SKU"
            placeholder="Selecciona un SKU"
            search={busquedaSku}
            value={skuId}
            error={errors.skuId?.message}
            onChange={(id) => setValue('skuId', id, { shouldValidate: true, shouldDirty: true })}
          />
          <TextField
            id="ajuste-numero-lote"
            label="Número de lote"
            error={errors.numeroLote?.message}
            {...register('numeroLote')}
          />
          <TextField
            id="ajuste-vencimiento"
            label="Fecha de vencimiento"
            type="date"
            error={errors.fechaVencimiento?.message}
            {...register('fechaVencimiento')}
          />
        </div>
      )}
      <div className="grid gap-4 sm:grid-cols-2">
        {posicion ? (
          <SelectField
            id="ajuste-tipo"
            label="Tipo de ajuste"
            error={errors.tipo?.message}
            {...register('tipo')}
          >
            {TIPOS_AJUSTE.map((tipo) => (
              <option key={tipo} value={tipo}>
                {ETIQUETAS_TIPO_AJUSTE[tipo]}
              </option>
            ))}
          </SelectField>
        ) : null}
        <TextField
          id="ajuste-cantidad"
          label="Cantidad"
          inputMode="decimal"
          error={errors.cantidad?.message}
          {...register('cantidad')}
        />
      </div>
      <TextField
        id="ajuste-motivo"
        label="Motivo"
        error={errors.motivo?.message}
        {...register('motivo')}
      />
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {posicion ? 'Registrar ajuste' : 'Registrar ingreso'}
      </Button>
    </form>
  );
}
