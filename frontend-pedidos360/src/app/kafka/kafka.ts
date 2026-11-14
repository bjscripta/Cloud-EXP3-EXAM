import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, finalize, forkJoin } from 'rxjs';
import { DetalleGrupo, Grupo, KafkaApi, Topico, apiError } from './kafka-api';

@Component({ selector: 'app-kafka', imports: [FormsModule], templateUrl: './kafka.html', styleUrl: './kafka.css' })
export class Kafka implements OnInit {
  private readonly api = inject(KafkaApi);
  private readonly destroyRef = inject(DestroyRef);
  private topicRequest = 0;
  private groupRequest = 0;
  readonly topicos = signal<string[]>([]);
  readonly grupos = signal<Grupo[]>([]);
  readonly topico = signal<Topico | null>(null);
  readonly grupo = signal<DetalleGrupo | null>(null);
  readonly cargando = signal(false);
  readonly cargandoTopico = signal(false);
  readonly cargandoGrupo = signal(false);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  nuevo = { name: '', partitions: 3, replicationFactor: 2, retentionMs: 604800000 };
  configuracion = { retentionMs: 604800000, cleanupPolicy: 'delete', minIsr: 2 };
  totalParticiones = 3;
  confirmarEliminar = '';
  ngOnInit() { this.actualizar(); }
  actualizar() {
    if (this.cargando()) return;
    this.cargando.set(true); this.error.set('');
    forkJoin({ topicos: this.api.topicos(), grupos: this.api.grupos() }).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.cargando.set(false)),
    ).subscribe({
      next: datos => {
        this.topicos.set(datos.topicos.filter(name => !name.startsWith('__')));
        this.grupos.set(datos.grupos);
        const actual = this.topico();
        if (actual && datos.topicos.includes(actual.name)) this.seleccionarTopico(actual.name);
        else if (actual) { ++this.topicRequest; this.topico.set(null); this.cargandoTopico.set(false); }
        const grupo = this.grupo();
        if (grupo && datos.grupos.some(g => g.groupId === grupo.groupId)) this.seleccionarGrupo(grupo.groupId);
        else if (grupo) { ++this.groupRequest; this.grupo.set(null); this.cargandoGrupo.set(false); }
      },
      error: error => this.error.set(apiError(error)),
    });
  }
  seleccionarTopico(name: string) {
    const version = ++this.topicRequest;
    this.topico.set(null); this.cargandoTopico.set(true); this.confirmarEliminar = ''; this.error.set('');
    this.api.topico(name).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: topic => {
        if (version !== this.topicRequest) return;
        this.topico.set(topic); this.cargandoTopico.set(false);
        this.totalParticiones = topic.partitions.length;
        this.configuracion = { retentionMs: Number(topic.configs['retention.ms'] ?? 604800000),
          cleanupPolicy: topic.configs['cleanup.policy'] ?? 'delete', minIsr: Number(topic.configs['min.insync.replicas'] ?? 2) };
      },
      error: error => { if (version === this.topicRequest) { this.error.set(apiError(error)); this.cargandoTopico.set(false); } },
    });
  }
  seleccionarGrupo(id: string) {
    const version = ++this.groupRequest;
    this.grupo.set(null); this.cargandoGrupo.set(true); this.error.set('');
    this.api.grupo(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: grupo => { if (version === this.groupRequest) { this.grupo.set(grupo); this.cargandoGrupo.set(false); } },
      error: error => { if (version === this.groupRequest) { this.error.set(apiError(error)); this.cargandoGrupo.set(false); } },
    });
  }
  crear() {
    this.ejecutar(this.api.crear({ name: this.nuevo.name.trim(), partitions: this.nuevo.partitions,
      replicationFactor: this.nuevo.replicationFactor,
      configs: { 'retention.ms': String(this.nuevo.retentionMs), 'cleanup.policy': 'delete',
        'min.insync.replicas': String(Math.min(2, this.nuevo.replicationFactor)) } }), 'Tópico creado.', () => { this.nuevo.name = ''; });
  }
  guardarConfiguracion() {
    const topic = this.topico(); if (!topic) return;
    this.ejecutar(this.api.configurar(topic.name, { 'retention.ms': String(this.configuracion.retentionMs),
      'cleanup.policy': this.configuracion.cleanupPolicy, 'min.insync.replicas': String(this.configuracion.minIsr) }), 'Configuración actualizada.');
  }
  aumentarParticiones() {
    const topic = this.topico(); if (!topic || this.totalParticiones <= topic.partitions.length) return;
    this.ejecutar(this.api.particiones(topic.name, this.totalParticiones), 'Particiones aumentadas.');
  }
  eliminar() {
    const topic = this.topico(); if (!topic || this.confirmarEliminar !== topic.name) return;
    this.ejecutar(this.api.eliminar(topic.name), 'Tópico eliminado.', () => { this.topico.set(null); this.confirmarEliminar = ''; });
  }
  private ejecutar<T>(operation: Observable<T>, success: string, after?: () => void) {
    if (this.ocupado()) return;
    this.ocupado.set(true); this.error.set(''); this.mensaje.set('');
    operation.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.ocupado.set(false))).subscribe({
      next: () => { after?.(); this.mensaje.set(success); this.actualizar(); },
      error: error => this.error.set(apiError(error)),
    });
  }
}
