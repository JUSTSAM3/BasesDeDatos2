import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  BackendService,
  Cliente,
  Empleado,
  Espacio,
  EspacioRequestDTO,
  PagoRequestDTO,
  ReservaRequestDTO,
  ReservaResponseDTO,
  ServicioAdicional,
  Sucursal,
  TipoEspacio
} from '../../services/backend.service';

export interface ReservaTabla {
  idReserva: number;
  cliente: string;
  espacio: string;
  fecha: string;
  hora: string;
  costoTotal: number;
  estado: string;
  estadoPago?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {

  resumen = [
    { titulo: 'Total reservas', valor: 0 },
    { titulo: 'Clientes activos', valor: 0 },
    { titulo: 'Espacios disponibles', valor: 0 },
    { titulo: 'Reservas pendientes', valor: 0 }
  ];

  reservas: ReservaTabla[] = [];
  clientes: Cliente[] = [];
  espacios: Espacio[] = [];
  empleados: Empleado[] = [];
  sucursales: Sucursal[] = [];
  tiposEspacio: TipoEspacio[] = [];
  servicios: ServicioAdicional[] = [];

  mensajeAlerta = '';
  esError = false;

  // Modales / Formularios
  modalActivo: 'ninguno' | 'reserva' | 'pago' | 'cliente' | 'espacio' = 'ninguno';

  formReserva = {
    idCliente: 1,
    idEspacio: 1,
    idEmpleado: 1,
    fecha: '',
    horaInicio: '10:00',
    horaFin: '14:00',
    idServicio: 0,
    cantidadServicio: 1
  };

  formPago: PagoRequestDTO = {
    idReserva: 0,
    tipoPago: 'anticipo',
    monto: 100000,
    metodoPago: 'transferencia'
  };

  formCliente: Cliente = {
    tipoCliente: 'natural',
    identificacion: '',
    nombreRazonSocial: '',
    email: '',
    telefono: ''
  };

  formEspacio: EspacioRequestDTO = {
    idSucursal: 1,
    nombre: '',
    idTipoEspacio: 1,
    capacidad: 50,
    tarifaPorHora: 150000,
    estado: 'disponible'
  };

  constructor(private backendService: BackendService) {}

  ngOnInit(): void {
    this.cargarTodo();
  }

  cargarTodo(): void {
    forkJoin({
      clientes: this.backendService.listarClientes(),
      espacios: this.backendService.listarEspacios(),
      empleados: this.backendService.listarEmpleados(),
      sucursales: this.backendService.listarSucursales(),
      tiposEspacio: this.backendService.listarTiposEspacio(),
      servicios: this.backendService.listarServiciosAdicionales()
    }).subscribe({
      next: (res) => {
        this.clientes = res.clientes || [];
        if (this.clientes.length > 0 && this.clientes[0].idCliente) {
          this.formReserva.idCliente = this.clientes[0].idCliente;
        }

        this.espacios = res.espacios || [];
        if (this.espacios.length > 0 && this.espacios[0].idEspacio) {
          this.formReserva.idEspacio = this.espacios[0].idEspacio;
        }

        this.empleados = res.empleados || [];
        if (this.empleados.length > 0 && this.empleados[0].idEmpleado) {
          this.formReserva.idEmpleado = this.empleados[0].idEmpleado;
        }

        this.sucursales = res.sucursales || [];
        if (this.sucursales.length > 0 && this.sucursales[0].idSucursal) {
          this.formEspacio.idSucursal = this.sucursales[0].idSucursal;
        }

        this.tiposEspacio = res.tiposEspacio || [];
        if (this.tiposEspacio.length > 0 && this.tiposEspacio[0].idTipoEspacio) {
          this.formEspacio.idTipoEspacio = this.tiposEspacio[0].idTipoEspacio;
        }

        this.servicios = res.servicios || [];

        this.actualizarMetricas();
        this.cargarReservas();
      },
      error: () => {
        this.cargarReservas();
      }
    });
  }

  cargarReservas(): void {
    this.backendService.listarReservas().subscribe({
      next: (data: ReservaResponseDTO[]) => {
        this.reservas = (data || []).map((r) => {
          const cli = this.clientes.find((c) => c.idCliente === r.idCliente);
          const esp = this.espacios.find((e) => e.idEspacio === r.idEspacio);
          const fechaObj = r.fechaInicio ? new Date(r.fechaInicio) : null;
          const finObj = r.fechaFin ? new Date(r.fechaFin) : null;

          return {
            idReserva: r.idReserva,
            cliente: cli ? cli.nombreRazonSocial : `Cliente #${r.idCliente}`,
            espacio: esp ? esp.nombre : `Espacio #${r.idEspacio}`,
            fecha: fechaObj ? fechaObj.toLocaleDateString('es-CO') : r.fechaInicio,
            hora:
              fechaObj && finObj
                ? `${fechaObj.toTimeString().slice(0, 5)} - ${finObj.toTimeString().slice(0, 5)}`
                : '',
            costoTotal: Number(r.costoTotal || 0),
            estado: r.estado ? r.estado.charAt(0).toUpperCase() + r.estado.slice(1).toLowerCase() : 'Pendiente',
            estadoPago: r.estadoPago || 'pendiente'
          };
        });

        this.actualizarMetricas();
      },
      error: () => {}
    });
  }

  actualizarMetricas(): void {
    const espaciosDisp = this.espacios.filter(
      (e) => !e.estado || e.estado.toLowerCase() === 'disponible'
    ).length;
    const pendientes = this.reservas.filter(
      (r) => r.estado.toLowerCase() === 'pendiente'
    ).length;

    this.resumen = [
      { titulo: 'Total reservas', valor: this.reservas.length },
      { titulo: 'Clientes activos', valor: this.clientes.length },
      { titulo: 'Espacios disponibles', valor: espaciosDisp },
      { titulo: 'Reservas pendientes', valor: pendientes }
    ];
  }

  mostrarMensaje(msg: string, error = false): void {
    this.mensajeAlerta = msg;
    this.esError = error;
    setTimeout(() => {
      if (this.mensajeAlerta === msg) this.mensajeAlerta = '';
    }, 6000);
  }

  // ==================== ACCIONES RELACIONALES ====================
  abrirModal(tipo: 'ninguno' | 'reserva' | 'pago' | 'cliente' | 'espacio', reserva?: ReservaTabla): void {
    this.modalActivo = tipo;
    if (tipo === 'pago' && reserva) {
      this.formPago = {
        idReserva: reserva.idReserva,
        tipoPago: 'anticipo',
        monto: Math.max(Math.round(reserva.costoTotal * 0.3), 10000),
        metodoPago: 'transferencia'
      };
    }
  }

  guardarReserva(): void {
    if (!this.formReserva.fecha) {
      this.mostrarMensaje('Selecciona una fecha para la reserva.', true);
      return;
    }

    const dto: ReservaRequestDTO = {
      idCliente: Number(this.formReserva.idCliente),
      idEspacio: Number(this.formReserva.idEspacio),
      idEmpleado: this.formReserva.idEmpleado ? Number(this.formReserva.idEmpleado) : null,
      fechaInicio: `${this.formReserva.fecha}T${this.formReserva.horaInicio}:00`,
      fechaFin: `${this.formReserva.fecha}T${this.formReserva.horaFin}:00`,
      servicios:
        Number(this.formReserva.idServicio) > 0
          ? [{ idServicio: Number(this.formReserva.idServicio), cantidad: Number(this.formReserva.cantidadServicio) || 1 }]
          : []
    };

    this.backendService.crearReserva(dto).subscribe({
      next: (res) => {
        this.modalActivo = 'ninguno';
        this.mostrarMensaje(`Reserva #${res.idReserva} creada por $${Number(res.costoTotal).toLocaleString('es-CO')}`);
        this.cargarReservas();
      },
      error: (err) => {
        this.mostrarMensaje(err.error?.message || err.error?.detail || 'Error al crear la reserva.', true);
      }
    });
  }

  cancelarReserva(idReserva: number): void {
    if (!confirm(`¿Estás seguro de que deseas cancelar la reserva #${idReserva}? Esta acción no se puede deshacer.`)) {
      return;
    }
    this.backendService.cancelarReserva(idReserva).subscribe({
      next: (res) => {
        this.mostrarMensaje(res.mensaje || `Reserva #${idReserva} cancelada.`);
        this.cargarReservas();
      },
      error: (err) => {
        this.mostrarMensaje(err.error?.message || err.error?.detail || 'No se pudo cancelar la reserva.', true);
      }
    });
  }

  guardarPago(): void {
    this.backendService.registrarPago(this.formPago).subscribe({
      next: (res) => {
        this.modalActivo = 'ninguno';
        this.mostrarMensaje(
          `Pago registrado (Factura ${res.numeroFactura}). Estado reserva: ${res.estadoReserva} | Pago: ${res.estadoPago}`
        );
        this.cargarReservas();
      },
      error: (err) => {
        this.mostrarMensaje(err.error?.message || err.error?.detail || 'Error al registrar el pago.', true);
      }
    });
  }

  guardarCliente(): void {
    this.backendService.crearCliente(this.formCliente).subscribe({
      next: (cli) => {
        this.modalActivo = 'ninguno';
        this.mostrarMensaje(`Cliente "${cli.nombreRazonSocial}" registrado con éxito.`);
        this.formCliente = { tipoCliente: 'natural', identificacion: '', nombreRazonSocial: '', email: '', telefono: '' };
        this.cargarTodo();
      },
      error: (err) => {
        this.mostrarMensaje(err.error?.message || 'Error al crear el cliente.', true);
      }
    });
  }

  guardarEspacio(): void {
    this.backendService.crearEspacio(this.formEspacio).subscribe({
      next: (esp) => {
        this.modalActivo = 'ninguno';
        this.mostrarMensaje(`Espacio "${esp.nombre}" creado con éxito.`);
        this.formEspacio = {
          idSucursal: this.sucursales[0]?.idSucursal || 1,
          nombre: '',
          idTipoEspacio: this.tiposEspacio[0]?.idTipoEspacio || 1,
          capacidad: 50,
          tarifaPorHora: 150000,
          estado: 'disponible'
        };
        this.cargarTodo();
      },
      error: (err) => {
        this.mostrarMensaje(err.error?.message || 'Error al crear el espacio.', true);
      }
    });
  }
}
