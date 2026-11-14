import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { KafkaApi, Resumen, apiError } from '../kafka/kafka-api';

@Component({ selector: 'app-analitica', imports: [DatePipe, DecimalPipe],
  templateUrl: './analitica.html', styleUrl: '../kafka/kafka.css' })
export class Analitica implements OnInit {
  private readonly api = inject(KafkaApi);
  private readonly destroyRef = inject(DestroyRef);
  readonly resumen = signal<Resumen | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly actualizado = signal<Date | null>(null);
  ngOnInit() { this.actualizar(); }
  actualizar() {
    if (this.cargando()) return;
    this.cargando.set(true); this.error.set('');
    this.api.resumen().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.cargando.set(false))).subscribe({
      next: resumen => { this.resumen.set(resumen); this.actualizado.set(new Date()); },
      error: error => this.error.set(apiError(error)),
    });
  }
}
