import { useEffect } from 'react';
import { FormErrorSummary } from '../../../shared/components/FormErrorSummary';
import { OptionsFeedback } from './OptionsFeedback';
import { CatalogFormActions } from './CatalogFormActions';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { TIPOS_SKU } from '../api/skus.types';
import { SKU_VACIO } from '../lib/sku-form';
import {
  useCategoriasOpcionesQuery,
  useMarcasOpcionesQuery,
  useProductosOpcionesQuery,
  useSoporteOpcionesQuery
} from '../lib/use-opciones';
import { skuSchema, type SkuFormValues } from '../schemas/sku.schema';
import { unidadesMedidaApi } from '../support-catalog/configs/unidades-medida.config';
import { CamposFormulario, type CampoFormulario } from './CamposFormulario';

type Grupo = { titulo: string; campos: CampoFormulario<SkuFormValues>[] };

const ETIQUETAS_TIPO_SKU = { REGULADO: 'Regulado', NO_REGULADO: 'No regulado' } as const;
const OPCIONES_TIPO_SKU = TIPOS_SKU.map((tipo) => ({
  value: tipo,
  label: ETIQUETAS_TIPO_SKU[tipo]
}));

export type SkuFormProps = {
  defaultValues?: SkuFormValues;
  onSubmit: (values: SkuFormValues) => void;
  submitLabel: string;
  onCancel?: (() => void) | undefined;
  isSubmitting?: boolean;
  fullPage?: boolean;
  onDirtyChange?: (dirty: boolean) => void;
  error?: string | null;
};

export function SkuForm({
  defaultValues = SKU_VACIO,
  onSubmit,
  submitLabel,
  onCancel,
  fullPage = false,
  onDirtyChange,
  isSubmitting = false,
  error = null
}: SkuFormProps) {
  const productosQuery = useProductosOpcionesQuery();
  const productos = productosQuery.data ?? [];
  const categoriasQuery = useCategoriasOpcionesQuery();
  const categorias = categoriasQuery.data ?? [];
  const marcasQuery = useMarcasOpcionesQuery();
  const marcas = marcasQuery.data ?? [];
  const unidadesQuery = useSoporteOpcionesQuery(unidadesMedidaApi);
  const unidades = unidadesQuery.data ?? [];

  const {
    formState: { errors, isDirty, submitCount },
    handleSubmit,
    register
  } = useForm<SkuFormValues>({
    defaultValues,
    mode: 'onTouched',
    resolver: zodResolver(skuSchema)
  });

  useEffect(() => {
    onDirtyChange?.(isDirty);
  }, [isDirty, onDirtyChange]);

  const grupos: Grupo[] = [
    {
      titulo: 'Identificación',
      campos: [
        { name: 'tipoSku', label: 'Tipo de SKU', tipo: 'seleccion', opciones: OPCIONES_TIPO_SKU },
        {
          name: 'productoReguladoId',
          label: 'Producto regulado',
          tipo: 'seleccion',
          opciones: productos
        },
        { name: 'codigoInterno', label: 'Código interno', tipo: 'texto' },
        { name: 'descripcionComercial', label: 'Descripción comercial', tipo: 'texto' },
        { name: 'nombreCorto', label: 'Nombre corto', tipo: 'texto' },
        { name: 'presentacionComercial', label: 'Presentación comercial', tipo: 'texto' },
        { name: 'categoriaId', label: 'Categoría', tipo: 'seleccion', opciones: categorias },
        { name: 'marcaId', label: 'Marca', tipo: 'seleccion', opciones: marcas }
      ]
    },
    {
      titulo: 'Unidades y medidas',
      campos: [
        {
          name: 'unidadVentaCodigo',
          label: 'Unidad de venta',
          tipo: 'seleccion',
          opciones: unidades
        },
        { name: 'contenido', label: 'Contenido', tipo: 'decimal' },
        {
          name: 'unidadContenidoCodigo',
          label: 'Unidad de contenido',
          tipo: 'seleccion',
          opciones: unidades
        },
        { name: 'pesoGramos', label: 'Peso (g)', tipo: 'decimal' },
        { name: 'altoCm', label: 'Alto (cm)', tipo: 'decimal' },
        { name: 'anchoCm', label: 'Ancho (cm)', tipo: 'decimal' },
        { name: 'largoCm', label: 'Largo (cm)', tipo: 'decimal' }
      ]
    },
    {
      titulo: 'Venta por fracción',
      campos: [
        { name: 'permiteVentaFraccion', label: 'Permite venta por fracción', tipo: 'checkbox' },
        { name: 'factorFraccion', label: 'Factor de fracción', tipo: 'decimal' },
        {
          name: 'unidadFraccionCodigo',
          label: 'Unidad de fracción',
          tipo: 'seleccion',
          opciones: unidades
        }
      ]
    },
    {
      titulo: 'Control y stock',
      campos: [
        { name: 'requiereLote', label: 'Requiere lote', tipo: 'checkbox' },
        { name: 'requiereVencimiento', label: 'Requiere fecha de vencimiento', tipo: 'checkbox' },
        { name: 'afectoIgv', label: 'Afecto a IGV', tipo: 'checkbox' },
        { name: 'stockMinimoDefault', label: 'Stock mínimo por defecto', tipo: 'decimal' },
        { name: 'stockMaximoDefault', label: 'Stock máximo por defecto', tipo: 'decimal' },
        { name: 'precioVentaReferencia', label: 'Precio de venta de referencia', tipo: 'decimal' },
        { name: 'imagenUri', label: 'URI de imagen', tipo: 'texto' }
      ]
    }
  ];

  return (
    <form
      className="space-y-6"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <p className="text-sm leading-6 text-neutral-500 dark:text-neutral-400">
        Completa el tipo de SKU, código interno, descripción comercial, unidad de venta y stock
        mínimo. Asocia un producto si el SKU es regulado. Para vender por fracción, indica también
        el factor y su unidad.
      </p>
      <OptionsFeedback queries={[productosQuery, categoriasQuery, marcasQuery, unidadesQuery]} />
      {submitCount > 0 ? <FormErrorSummary errors={errors} prefix="sku" /> : null}
      {grupos.map((grupo) => (
        <fieldset
          key={grupo.titulo}
          className="rounded-xl border border-neutral-200 p-4 sm:p-5 dark:border-neutral-700"
        >
          <legend className="text-primary-700 dark:text-primary-300 px-2 text-sm font-bold">
            {grupo.titulo}
          </legend>
          <div className="grid gap-4 sm:grid-cols-2">
            <CamposFormulario
              prefijo="sku"
              campos={grupo.campos}
              register={register}
              errors={errors}
            />
          </div>
        </fieldset>
      ))}
      <CatalogFormActions
        submitLabel={submitLabel}
        isSubmitting={isSubmitting}
        error={error}
        onCancel={onCancel}
        fullPage={fullPage}
      />
    </form>
  );
}
