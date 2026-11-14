import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, finalize } from 'rxjs';
import { AdminApi, Resource, Row, Snapshot, adminError } from './admin-api';

interface Field {
  key: string; label: string; type: 'text' | 'email' | 'number' | 'select' | 'checkbox' | 'textarea' | 'relation';
  required?: boolean; options?: string[]; relation?: Resource; lockedOnEdit?: boolean; min?: number; step?: string;
}
interface Section { resource: Resource; label: string; fields: Field[]; columns: { key: string; label: string; relation?: Resource }[]; }

@Component({ selector: 'app-admin', imports: [FormsModule, DecimalPipe], templateUrl: './admin.html', styleUrl: './admin.css' })
export class Admin implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly destroyRef = inject(DestroyRef);
  readonly data = signal<Snapshot>({});
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly selected = signal<Resource>('clientes');
  readonly editing = signal(false);
  readonly deleting = signal<Row | null>(null);
  readonly showDemo = signal(false);
  demoConfirmed = false;
  deleteConfirmed = false;
  draft: Row = {};
  lines: { productoId: number | null; cantidad: number }[] = [];
  readonly sections: Section[] = [
    { resource: 'clientes', label: 'Clientes', fields: [
      { key: 'nombre', label: 'Nombre', type: 'text', required: true }, { key: 'email', label: 'Correo', type: 'email', required: true },
      { key: 'telefono', label: 'Teléfono', type: 'text' }, { key: 'direccion', label: 'Dirección', type: 'text' },
    ], columns: [{ key: 'nombre', label: 'Nombre' }, { key: 'email', label: 'Correo' }, { key: 'telefono', label: 'Teléfono' }, { key: 'direccion', label: 'Dirección' }] },
    { resource: 'productos', label: 'Productos', fields: [
      { key: 'nombre', label: 'Nombre', type: 'text', required: true }, { key: 'descripcion', label: 'Descripción', type: 'textarea' },
      { key: 'precio', label: 'Precio', type: 'number', required: true, min: 0, step: '0.01' }, { key: 'activo', label: 'Activo', type: 'checkbox' },
    ], columns: [{ key: 'nombre', label: 'Nombre' }, { key: 'precio', label: 'Precio' }, { key: 'activo', label: 'Activo' }] },
    { resource: 'inventarios', label: 'Inventario', fields: [
      { key: 'productoId', label: 'Producto', type: 'relation', relation: 'productos', required: true, lockedOnEdit: true },
      { key: 'cantidadDisponible', label: 'Stock disponible', type: 'number', required: true, min: 0 },
      { key: 'stockMinimo', label: 'Stock mínimo', type: 'number', required: true, min: 0 },
    ], columns: [{ key: 'productoId', label: 'Producto', relation: 'productos' }, { key: 'cantidadDisponible', label: 'Disponible' }, { key: 'stockMinimo', label: 'Mínimo' }] },
    { resource: 'pedidos', label: 'Pedidos', fields: [
      { key: 'clienteId', label: 'Cliente', type: 'relation', relation: 'clientes', required: true, lockedOnEdit: true },
      { key: 'direccionEntrega', label: 'Dirección de entrega', type: 'text', required: true },
      { key: 'estado', label: 'Estado', type: 'select', options: ['PENDIENTE', 'CONFIRMADO', 'ENVIADO', 'ENTREGADO', 'CANCELADO'] },
    ], columns: [{ key: 'clienteId', label: 'Cliente', relation: 'clientes' }, { key: 'lineas', label: 'Productos' }, { key: 'total', label: 'Total' }, { key: 'estado', label: 'Estado' }] },
    { resource: 'facturas', label: 'Facturas', fields: [
      { key: 'pedidoId', label: 'Pedido', type: 'relation', relation: 'pedidos', required: true, lockedOnEdit: true },
      { key: 'estadoPago', label: 'Estado del pago', type: 'select', required: true, options: ['PENDIENTE', 'PAGADO', 'ANULADO'] },
    ], columns: [{ key: 'pedidoId', label: 'Pedido', relation: 'pedidos' }, { key: 'clienteId', label: 'Cliente', relation: 'clientes' }, { key: 'montoTotal', label: 'Monto' }, { key: 'estadoPago', label: 'Pago' }] },
    { resource: 'envios', label: 'Envíos', fields: [
      { key: 'pedidoId', label: 'Pedido', type: 'relation', relation: 'pedidos', required: true, lockedOnEdit: true },
      { key: 'direccionEntrega', label: 'Dirección de entrega', type: 'text', required: true },
      { key: 'estadoEnvio', label: 'Estado del envío', type: 'select', required: true, options: ['EN_PREPARACION', 'DESPACHADO', 'ENTREGADO', 'CANCELADO'] },
      { key: 'numeroSeguimiento', label: 'Seguimiento', type: 'text', required: true },
    ], columns: [{ key: 'pedidoId', label: 'Pedido', relation: 'pedidos' }, { key: 'estadoEnvio', label: 'Estado' }, { key: 'numeroSeguimiento', label: 'Seguimiento' }, { key: 'direccionEntrega', label: 'Dirección' }] },
  ];
  readonly section = computed(() => this.sections.find(s => s.resource === this.selected())!);
  readonly rows = computed(() => this.data()[this.selected()]?.rows ?? []);
  readonly canCreate = computed(() => !['facturas', 'envios'].includes(this.selected()));
  ngOnInit() { this.refresh(); }
  refresh() {
    if (this.loading()) return;
    this.loading.set(true); this.error.set('');
    this.api.data().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))).subscribe({
      next: data => this.data.set(data), error: error => this.error.set(adminError(error)),
    });
  }
  choose(resource: Resource) {
    if (this.busy()) return;
    this.selected.set(resource); this.editing.set(false); this.deleting.set(null); this.error.set(''); this.message.set('');
  }
  records(resource: Resource) { return this.data()[resource]?.rows ?? []; }
  label(resource: Resource, id: unknown): string {
    const row = this.records(resource).find(r => Number(r.id) === Number(id));
    if (!row) return `#${id} (sin referencia disponible)`;
    if (resource === 'pedidos') return `Pedido #${row.id} · ${this.label('clientes', row['clienteId'])}`;
    return `#${row.id} · ${row['nombre'] ?? row.id}`;
  }
  cell(row: Row, key: string, relation?: Resource): string {
    if (relation) return this.label(relation, row[key]);
    const value = row[key];
    if (key === 'lineas') return Array.isArray(value) && value.length
      ? value.map(l => `${l.cantidad} × ${l.nombreProducto || this.label('productos', l.productoId)}`).join('; ')
      : 'Pedido anterior sin detalle de productos';
    if (typeof value === 'boolean') return value ? 'Sí' : 'No';
    if (typeof value === 'number') return new Intl.NumberFormat('es-CL', { maximumFractionDigits: 2 }).format(value);
    return value == null || value === '' ? '—' : String(value);
  }
  open(row?: Row) {
    this.draft = row ? structuredClone(row) : { activo: true, cantidadDisponible: 0, stockMinimo: 5, estado: 'PENDIENTE' };
    this.lines = Array.isArray(this.draft['lineas']) ? structuredClone(this.draft['lineas']) : [{ productoId: null, cantidad: 1 }];
    this.editing.set(true); this.deleting.set(null); this.error.set('');
  }
  fillAddress() {
    const customer = this.records('clientes').find(r => r.id === this.draft['clienteId']);
    if (customer) this.draft['direccionEntrega'] = customer['direccion'] ?? '';
  }
  total(): number {
    return this.lines.reduce((sum, line) => {
      const product = this.records('productos').find(r => r.id === line.productoId);
      return sum + Number(product?.['precio'] ?? 0) * Number(line.cantidad || 0);
    }, 0);
  }
  validLines() { return this.lines.length > 0 && this.lines.every(l => l.productoId && Number.isInteger(l.cantidad) && l.cantidad > 0 && l.cantidad <= 10000); }
  save() {
    const resource = this.selected();
    const body = structuredClone(this.draft);
    if (resource === 'pedidos' && !body.id) {
      if (!this.validLines()) return;
      body['lineas'] = this.lines.map(l => ({ productoId: l.productoId, cantidad: l.cantidad }));
    }
    this.run(this.api.save(resource, body), 'Registro guardado. Los derivados del pedido se generan de forma asíncrona.', () => this.editing.set(false));
  }
  askDelete(row: Row) { this.deleting.set(row); this.deleteConfirmed = false; }
  remove() {
    const row = this.deleting(); if (!row?.id || !this.deleteConfirmed) return;
    this.run(this.api.remove(this.selected(), row.id), 'Registro eliminado.', () => this.deleting.set(null));
  }
  seed() {
    if (!this.demoConfirmed) return;
    this.run(this.api.seed(), 'Datos de prueba preparados. Actualiza en unos segundos para consultar facturas y envíos.', () => { this.showDemo.set(false); this.demoConfirmed = false; });
  }
  related(resource: 'facturas' | 'envios', id: number | undefined) {
    return this.records(resource).filter(r => Number(r['pedidoId']) === Number(id)).map(r => `#${r.id} · ${r[resource === 'facturas' ? 'estadoPago' : 'estadoEnvio']}`).join(', ') || 'Pendiente de generación';
  }
  private run<T>(operation: Observable<T>, message: string, after?: () => void) {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    operation.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: () => { after?.(); this.message.set(message); this.refresh(); }, error: error => { this.error.set(adminError(error)); this.refreshAfterError(); },
    });
  }
  private refreshAfterError() {
    this.api.data().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: data => this.data.set(data), error: () => {} });
  }
}
