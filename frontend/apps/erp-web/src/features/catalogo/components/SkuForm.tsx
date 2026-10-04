import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { TIPOS_SKU } from '../api/skus.types';
import { SKU_VACIO } from '../lib/sku-form';
import {
  useOpcionesCategorias,
  useOpcionesMarcas,
  useOpcionesProductosRegulados,
  useOpcionesSoporte
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
  isSubmitting?: boolean;
  error?: string | null;
};

export function SkuForm({
  defaultValues = SKU_VACIO,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: SkuFormProps) {
  const productos = useOpcionesProductosRegulados();
  const categorias = useOpcionesCategorias();
  const marcas = useOpcionesMarcas();
  const unidades = useOpcionesSoporte(unidadesMedidaApi);

  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<SkuFormValues>({
    defaultValues,
    mode: 'onTouched',
    resolver: zodResolver(skuSchema)
  });

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
      {grupos.map((grupo) => (
        <fieldset key={grupo.titulo} className="space-y-4">
          <legend className="text-sm font-bold text-neutral-900 dark:text-neutral-50">
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
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
