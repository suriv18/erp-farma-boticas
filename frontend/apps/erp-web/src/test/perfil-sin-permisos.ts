import { http, HttpResponse } from 'msw';
import { server } from './mocks/server';

export function servirPerfilSinPermisos(): Promise<void> {
  return new Promise((resolve) => {
    server.use(
      http.get('*/api/v1/auth/me', () => {
        resolve();
        return HttpResponse.json({
          userId: 'user-1',
          tenantId: 'tenant-1',
          displayName: 'Luis',
          username: 'luis',
          email: null,
          assignments: [],
          permissions: []
        });
      })
    );
  });
}
