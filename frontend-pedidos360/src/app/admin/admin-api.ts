import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../environments/environment';
export type Resource = 'clientes' | 'productos' | 'inventarios' | 'pedidos' | 'facturas' | 'envios';
export interface Row { id?: number; [key: string]: unknown; }
export interface ServiceData { available: boolean; rows: Row[]; message?: string; }
export type Snapshot = Partial<Record<Resource, ServiceData>>;
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly base = `${environment.apiBaseUrl}/api/admin`;
  constructor(private readonly http: HttpClient) {}
  data() { return this.http.get<Snapshot>(`${this.base}/datos`); }
  save(resource: Resource, row: Row) {
    return row.id ? this.http.put<Row>(`${this.base}/datos/${resource}/${row.id}`, row)
      : this.http.post<Row>(`${this.base}/datos/${resource}`, row);
  }
  remove(resource: Resource, id: number) { return this.http.delete<void>(`${this.base}/datos/${resource}/${id}`); }
  seed() { return this.http.post<{ message: string; created: Record<string, number> }>(`${this.base}/demo`, {}); }
}
export function adminError(error: HttpErrorResponse): string {
  if (error.status === 0) return 'No se pudo conectar con el servidor.';
  if (error.status === 401) return 'Vuelve a iniciar sesión para continuar.';
  if (error.status === 403) return 'Esta operación requiere el permiso Pedidos360.Write.';
  return error.error?.message || 'No se pudo completar la operación. Actualiza para revisar los datos antes de volver a intentar.';
}
