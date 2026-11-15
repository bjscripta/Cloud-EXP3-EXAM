import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../environments/environment';
export interface RabbitQueue { name: string; durable: boolean; messages?: number; consumers?: number; protectedResource: boolean; arguments: Record<string, unknown>; }
export interface RabbitExchange { name: string; type: string; durable: boolean; protectedResource: boolean; }
export interface RabbitBinding { source: string; destination: string; destination_type: string; routing_key: string; properties_key: string; protectedResource: boolean; }
export interface RabbitResources { queues: RabbitQueue[]; exchanges: RabbitExchange[]; bindings: RabbitBinding[]; }
export interface QueueRequest { name: string; durable: boolean; deadLetterExchange: string; deadLetterRoutingKey: string; }
export interface ExchangeRequest { name: string; type: string; durable: boolean; }
export interface BindingRequest { source: string; queue: string; routingKey: string; }
@Injectable({ providedIn: 'root' })
export class RabbitApi {
  private readonly base = `${environment.apiBaseUrl}/api/rabbit`;
  constructor(private readonly http: HttpClient) {}
  resources() { return this.http.get<RabbitResources>(`${this.base}/resources`); }
  queue(body: QueueRequest) { return this.http.post<void>(`${this.base}/queues`, body); }
  exchange(body: ExchangeRequest) { return this.http.post<void>(`${this.base}/exchanges`, body); }
  bind(body: BindingRequest) { return this.http.post<void>(`${this.base}/bindings`, body); }
  unbind(binding: RabbitBinding) { return this.http.delete<void>(`${this.base}/bindings`, { body: { source: binding.source, queue: binding.destination, propertiesKey: binding.properties_key } }); }
  deleteQueue(name: string) { return this.http.delete<void>(`${this.base}/queues/${encodeURIComponent(name)}`); }
  deleteExchange(name: string) { return this.http.delete<void>(`${this.base}/exchanges/${encodeURIComponent(name)}`); }
  publish(body: { exchange: string; routingKey: string; payload: string }) { return this.http.post<{ routed: boolean }>(`${this.base}/messages`, body); }
  purge(name: string, businessDlq: boolean) {
    return this.http.delete<unknown>(`${this.base}/${businessDlq ? 'dlq' : 'queues'}/${encodeURIComponent(name)}/messages`);
  }
}
export function rabbitError(error: HttpErrorResponse): string {
  if (error.status === 0) return 'No se pudo conectar con la API.';
  if (error.status === 401) return 'Vuelve a iniciar sesión para continuar.';
  if (error.status === 403) return 'Esta operación requiere el permiso Pedidos360.Write.';
  if (error.status === 404) return 'El recurso ya no existe. Actualiza la lista.';
  return error.error?.message || error.error?.error || 'No se pudo completar la operación de RabbitMQ. Actualiza su estado antes de volver a intentar.';
}
