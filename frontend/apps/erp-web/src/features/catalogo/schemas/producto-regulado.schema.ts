import { z } from 'zod';
import { fechaOpcional, textoMax, textoRango } from './campos';

export const productoReguladoSchema = z
  .object({
    tipoProducto: textoRango('El tipo de producto', 2, 40),
    rubroCodigo: textoMax('El rubro', 50),
    tipoRegistro: textoMax('El tipo de registro', 40),
    numeroRegistro: textoMax('El número de registro', 100),
    denominacion: textoRango('La denominación', 2, 500),
    concentracionTexto: textoMax('La concentración', 300),
    presentacionRegulatoria: textoMax('La presentación regulatoria', 500),
    formaFarmaceuticaCodigo: z.string(),
    viaAdministracionCodigo: z.string(),
    unidadMedidaCodigo: z.string(),
    condicionVentaCodigo: z.string(),
    clasificacionAtc: textoMax('La clasificación ATC', 30),
    clasificacionControladaCodigo: z.string(),
    tipoLiberacion: textoMax('El tipo de liberación', 40),
    origenFabricacion: textoMax('El origen de fabricación', 40),
    paisOrigen: textoMax('El país de origen', 100),
    subpartidaNacional: textoMax('La subpartida nacional', 30),
    titularRegistro: textoMax('El titular de registro', 300),
    fabricante: textoMax('El fabricante', 300),
    importador: textoMax('El importador', 300),
    establecimientoExpendio: textoMax('El establecimiento de expendio', 200),
    vigenteDesde: fechaOpcional,
    vigenteHasta: fechaOpcional,
    fuente: textoMax('La fuente', 300),
    versionFuente: textoMax('La versión de fuente', 100)
  })
  .superRefine((valores, contexto) => {
    if (Boolean(valores.tipoRegistro.trim()) !== Boolean(valores.numeroRegistro.trim())) {
      contexto.addIssue({
        code: 'custom',
        path: ['numeroRegistro'],
        message: 'El tipo y el número de registro deben informarse juntos.'
      });
    }
    if (
      valores.vigenteDesde &&
      valores.vigenteHasta &&
      valores.vigenteHasta < valores.vigenteDesde
    ) {
      contexto.addIssue({
        code: 'custom',
        path: ['vigenteHasta'],
        message: 'La vigencia hasta no puede ser anterior a la vigencia desde.'
      });
    }
  });

export type ProductoReguladoFormValues = z.infer<typeof productoReguladoSchema>;
