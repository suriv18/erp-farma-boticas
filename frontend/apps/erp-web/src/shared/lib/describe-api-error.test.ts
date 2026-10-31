import { ApiError } from '@boticas/api-client';
import { describeApiError } from './describe-api-error';

describe('describeApiError', () => {
  it('usa el detalle del problema cuando existe', () => {
    const error = new ApiError('Conflict', 409, {
      detail: 'Ya existe una empresa con el RUC indicado.'
    });
    expect(describeApiError(error)).toBe('Ya existe una empresa con el RUC indicado.');
  });

  it('usa el título del problema cuando no hay detalle', () => {
    expect(
      describeApiError(new ApiError('Bad Request', 400, { title: 'Solicitud inválida' }))
    ).toBe('Solicitud inválida');
  });

  it('usa el mensaje del error cuando no hay problema', () => {
    expect(describeApiError(new ApiError('Fallo inesperado', 500))).toBe('Fallo inesperado');
  });

  it('explica la falta de permisos en un 403', () => {
    expect(describeApiError(new ApiError('Forbidden', 403))).toBe(
      'No tienes permiso para esta acción.'
    );
  });

  it('explica el recurso inexistente en un 404', () => {
    expect(describeApiError(new ApiError('Not Found', 404))).toBe(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('devuelve un mensaje genérico para errores que no vienen de la API', () => {
    expect(describeApiError(new Error('boom'))).toBe(
      'No se pudo completar la operación. Inténtalo de nuevo.'
    );
  });
});
