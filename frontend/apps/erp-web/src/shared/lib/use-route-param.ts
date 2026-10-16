import { useParams } from 'react-router';

export function useRouteParam(name: string): string {
  return useParams()[name] as string;
}
