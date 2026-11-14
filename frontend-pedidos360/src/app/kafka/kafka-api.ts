import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface Resumen { pedidosCreados: number; facturasRegistradas: number; montoFacturado: number; }
export interface Particion { partition: number; leader: number; replicas: number[]; isr: number[]; }
export interface Topico { name: string; partitions: Particion[]; configs: Record<string, string>; }
export interface Grupo { groupId: string; state: string; }
export interface DetalleGrupo extends Grupo {
  members: { consumerId: string; clientId: string; partitions: string[] }[];
  offsets: { topic: string; partition: number; committedOffset: number; endOffset: number; lag: number }[];
}

@Injectable({ providedIn: 'root' })
export class KafkaApi {
  private readonly base = `${environment.apiBaseUrl}/api`;
  constructor(private readonly http: HttpClient) {}
  resumen() { return this.http.get<Resumen>(`${this.base}/analitica/resumen`); }
  topicos() { return this.http.get<string[]>(`${this.base}/kafka/topics`); }
  topico(name: string) { return this.http.get<Topico>(this.topicUrl(name)); }
  crear(body: { name: string; partitions: number; replicationFactor: number; configs: Record<string, string> }) {
    return this.http.post<void>(`${this.base}/kafka/topics`, body);
  }
  configurar(name: string, configs: Record<string, string>) {
    return this.http.patch<void>(`${this.topicUrl(name)}/configs`, { configs });
  }
  particiones(name: string, total: number) {
    return this.http.patch<void>(`${this.topicUrl(name)}/partitions`, { total });
  }
  eliminar(name: string) { return this.http.delete<void>(this.topicUrl(name)); }
  grupos() { return this.http.get<Grupo[]>(`${this.base}/kafka/groups`); }
  grupo(id: string) { return this.http.get<DetalleGrupo>(`${this.base}/kafka/groups/${encodeURIComponent(id)}`); }
  private topicUrl(name: string) { return `${this.base}/kafka/topics/${encodeURIComponent(name)}`; }
}

export function apiError(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0: return 'No se pudo conectar con la API. Revisa la conexión y vuelve a intentar.';
    case 400: return 'Los datos no son válidos. Revisa el formulario y la configuración del tópico.';
    case 401: return 'Tu sesión no es válida. Vuelve a iniciar sesión.';
    case 403: return 'No tienes permiso para esta operación. Las modificaciones requieren permiso de escritura.';
    case 404: return 'El tópico o grupo ya no existe. Actualiza la lista.';
    case 409: return 'Ya existe un tópico con ese nombre.';
    case 503: return 'Kafka o el servicio solicitado no está disponible. Si estabas modificando un tópico, consulta su estado antes de repetir la operación.';
    default: return 'No se pudo completar la operación. Vuelve a consultar el estado antes de intentarlo otra vez.';
  }
}
