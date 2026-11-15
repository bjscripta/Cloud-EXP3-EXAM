import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, finalize } from 'rxjs';
import { BindingRequest, ExchangeRequest, QueueRequest, RabbitApi, RabbitBinding, RabbitQueue, RabbitResources, rabbitError } from './rabbit-api';

type Confirmation = { kind: 'queue' | 'exchange' | 'binding' | 'purge'; name: string; binding?: RabbitBinding; businessDlq?: boolean };
@Component({ selector: 'app-rabbit', imports: [FormsModule], templateUrl: './rabbit.html', styleUrl: '../kafka/kafka.css' })
export class Rabbit implements OnInit {
  private readonly api = inject(RabbitApi);
  private readonly destroyRef = inject(DestroyRef);
  readonly resources = signal<RabbitResources>({ queues: [], exchanges: [], bindings: [] });
  readonly loaded = signal(false);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly confirmation = signal<Confirmation | null>(null);
  confirmedName = '';
  queue: QueueRequest = { name: '', durable: true, deadLetterExchange: '', deadLetterRoutingKey: '' };
  exchange: ExchangeRequest = { name: '', type: 'direct', durable: true };
  binding: BindingRequest = { source: '', queue: '', routingKey: '' };
  test = { exchange: '', routingKey: '', payload: 'Mensaje de prueba' };
  readonly customQueues = computed(() => this.resources().queues.filter(q => !q.protectedResource));
  readonly exchanges = computed(() => this.resources().exchanges.filter(e => e.name && !e.name.startsWith('amq.')));
  readonly customExchanges = computed(() => this.exchanges().filter(e => !e.protectedResource));
  readonly bindings = computed(() => this.resources().bindings.filter(b => b.source));
  ngOnInit() { this.refresh(); }
  refresh() {
    if (this.loading()) return;
    this.loading.set(true); this.error.set('');
    this.api.resources().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
      next: data => { this.resources.set(data); this.loaded.set(true); }, error: error => this.error.set(rabbitError(error)),
    });
  }
  createQueue() { this.run(this.api.queue(this.queue), 'Cola creada o declarada con la misma configuración.', () => this.queue.name = ''); }
  createExchange() { this.run(this.api.exchange(this.exchange), 'Exchange creado o declarado con la misma configuración.', () => this.exchange.name = ''); }
  createBinding() { this.run(this.api.bind(this.binding), 'Binding creado.'); }
  ask(kind: Confirmation['kind'], name: string, binding?: RabbitBinding, businessDlq?: boolean) {
    this.confirmedName = ''; this.confirmation.set({ kind, name, binding, businessDlq });
  }
  confirm() {
    const operation = this.confirmation();
    if (!operation || this.confirmedName !== operation.name) return;
    let request: Observable<unknown>;
    switch (operation.kind) {
      case 'queue': request = this.api.deleteQueue(operation.name); break;
      case 'exchange': request = this.api.deleteExchange(operation.name); break;
      case 'binding': if (!operation.binding) return; request = this.api.unbind(operation.binding); break;
      case 'purge': request = this.api.purge(operation.name, !!operation.businessDlq); break;
    }
    this.run(request, operation.kind === 'purge' ? 'Mensajes eliminados; la cola se conserva.' : 'Recurso eliminado.', () => this.confirmation.set(null));
  }
  publish() {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    this.api.publish(this.test).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: result => { this.message.set(result.routed ? 'Mensaje enrutado a una cola. Actualiza en unos segundos para consultar los contadores.' : 'El exchange recibió el mensaje, pero ninguna cola coincidió con su routing key. Revisa el binding.'); this.refresh(); },
      error: error => this.error.set(rabbitError(error)),
    });
  }
  isDlq(queue: RabbitQueue) { return queue.protectedResource && queue.name.endsWith('.dlq'); }
  dlx(queue: RabbitQueue) { return String(queue.arguments?.['x-dead-letter-exchange'] ?? '—'); }
  private run<T>(request: Observable<T>, message: string, after?: () => void) {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: () => { after?.(); this.message.set(message); this.refresh(); }, error: error => this.error.set(rabbitError(error)),
    });
  }
}
