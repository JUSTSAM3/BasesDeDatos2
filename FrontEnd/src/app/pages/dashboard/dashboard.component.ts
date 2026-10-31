import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  BackendService,
  Cliente,
  DetalleEventoNoSql,
  Empleado,
  Espacio,
  EspacioRequestDTO,
  PagoRequestDTO,
  ReporteCalificacionNoSql,
  ReporteEspacioNoSql,
  ReporteServicioNoSql,
  ResenaNoSql,
  ReservaRequestDTO,
  ReservaResponseDTO,
  ServicioAdicional,
  Sucursal,
  TipoEspacio
} from '../../services/backend.service';

export interface ReservaTabla {
  idReserva: number;
  idCliente: number;
  idEspacio: number;
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

  // Datos y Reportes Documentales (MongoDB)
  detallesEventoMongo: DetalleEventoNoSql[] = [];
  resenasMongo: ResenaNoSql[] = [];
  reporteEspaciosMongo: ReporteEspacioNoSql[] = [];
  reporteServiciosMongo: ReporteServicioNoSql[] = [];
  reporteCalificacionesMongo: ReporteCalificacionNoSql[] = [];
  mongoDisponible = true;

  mensajeAlerta = '';
  esError = false;

  // Modales / Formularios
  modalActivo: 'ninguno' | 'reserva' | 'pago' | 'cliente' | 'espacio' | 'detalleMongo' | 'resenaMongo' = 'ninguno';

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

  formDetalleMongo = {
    idReserva: 0,
    idCliente: 1,
    idEspacio: 1,
    nombreEspacio: '',
    tipoEvento: 'boda',
    nombreEvento: '',
    numInvitadosEstimado: 100,
    invitadoPrincipal: '',
    requerimientoTipo: 'montaje',
    requerimientoDescripcion: '',
    actividadAgenda: '',
    tematicaExtra: ''
  };

  formResenaMongo = {
    idReserva: 0,
    idCliente: 1,
    idEspacio: 1,
    calificacionGeneral: 5,
    calEspacio: 5,
    calServicios: 5,
    calCoordinacion: 5,
    calPrecio: 4,
    comentario: '',
    recomendaria: true,
    etiquetasTexto: 'puntualidad, excelente servicio'
  };

  constructor(private backendService: BackendService) {}

  ngOnInit(): void {
    this.cargarTodo();
    this.cargarDatosMongo();
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
            idCliente: r.idCliente,
            idEspacio: r.idEspacio,
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

  cargarDatosMongo(): void {
    forkJoin({
      detalles: this.backendService.listarDetallesEvento(),
      resenas: this.backendService.listarResenas(),
      repEspacios: this.backendService.reporteEspaciosMasReservados(),
      repServicios: this.backendService.reporteServiciosMasSolicitados(),
      repCalificaciones: this.backendService.reporteCalificacionesPorEspacio()
    }).subscribe({
      next: (res) => {
        this.mongoDisponible = true;
        this.detallesEventoMongo = res.detalles || [];
        this.resenasMongo = res.resenas || [];
        this.reporteEspaciosMongo = res.repEspacios || [];
        this.reporteServiciosMongo = res.repServicios || [];
        this.reporteCalificacionesMongo = res.repCalificaciones || [];
      },
      error: () => {
        this.mongoDisponible = false;
      }
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
  abrirModal(
    tipo: 'ninguno' | 'reserva' | 'pago' | 'cliente' | 'espacio' | 'detalleMongo' | 'resenaMongo',
    reserva?: ReservaTabla
  ): void {
    this.modalActivo = tipo;
    if (tipo === 'pago' && reserva) {
      this.formPago = {
        idReserva: reserva.idReserva,
        tipoPago: 'anticipo',
        monto: Math.max(Math.round(reserva.costoTotal * 0.3), 10000),
        metodoPago: 'transferencia'
      };
    } else if (tipo === 'detalleMongo' && reserva) {
      this.formDetalleMongo = {
        idReserva: reserva.idReserva,
        idCliente: reserva.idCliente,
        idEspacio: reserva.idEspacio,
        nombreEspacio: reserva.espacio,
        tipoEvento: 'social',
        nombreEvento: `Evento ${reserva.cliente} (#${reserva.idReserva})`,
        numInvitadosEstimado: 80,
        invitadoPrincipal: reserva.cliente,
        requerimientoTipo: 'montaje',
        requerimientoDescripcion: 'Montaje imperial y sonido ambiental',
        actividadAgenda: 'Recepción de invitados y apertura',
        tematicaExtra: 'Corporativo / Social'
      };
    } else if (tipo === 'resenaMongo' && reserva) {
      this.formResenaMongo = {
        idReserva: reserva.idReserva,
        idCliente: reserva.idCliente,
        idEspacio: reserva.idEspacio,
        calificacionGeneral: 5,
        calEspacio: 5,
        calServicios: 5,
        calCoordinacion: 5,
        calPrecio: 4,
        comentario: `Excelente experiencia en ${reserva.espacio}.`,
        recomendaria: true,
        etiquetasTexto: 'puntualidad, excelente atención'
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
        this.mostrarMensaje(err.error?.message || err.error?.detail || 'Error al crear el espacio.', true);
      }
    });
  }

  // ==================== ACCIONES NOSQL (MONGODB) ====================
  guardarDetalleMongo(): void {
    const espCat = this.espacios.find((e) => e.idEspacio === this.formDetalleMongo.idEspacio);

    const payload: DetalleEventoNoSql = {
      idReserva: this.formDetalleMongo.idReserva,
      idCliente: this.formDetalleMongo.idCliente,
      tipoEvento: this.formDetalleMongo.tipoEvento,
      nombreEvento: this.formDetalleMongo.nombreEvento,
      numInvitadosEstimado: Number(this.formDetalleMongo.numInvitadosEstimado) || 50,
      espacio: {
        idEspacio: this.formDetalleMongo.idEspacio,
        nombre: espCat?.nombre || this.formDetalleMongo.nombreEspacio || 'Salón Principal',
        tipo: espCat?.nombreTipoEspacio || 'General',
        idSucursal: espCat?.idSucursal || 1,
        ciudad: 'Bogotá'
      },
      invitados: this.formDetalleMongo.invitadoPrincipal
        ? [{ nombre: this.formDetalleMongo.invitadoPrincipal, confirmado: true, mesa: 1 }]
        : [],
      requerimientosEspeciales: this.formDetalleMongo.requerimientoDescripcion
        ? [{
            tipo: this.formDetalleMongo.requerimientoTipo,
            descripcion: this.formDetalleMongo.requerimientoDescripcion,
            estado: 'pendiente'
          }]
        : [],
      agenda: this.formDetalleMongo.actividadAgenda
        ? [{ actividad: this.formDetalleMongo.actividadAgenda, responsable: 'Coordinador' }]
        : [],
      datosExtra: {
        tematica: this.formDetalleMongo.tematicaExtra || 'Estándar'
      },
      estadoReserva: 'confirmada'
    };

    this.backendService.guardarDetalleEvento(payload).subscribe({
      next: (doc) => {
        this.modalActivo = 'ninguno';
        this.mostrarMensaje(`Detalle documental guardado en MongoDB para la reserva #${doc.idReserva}.`);
        this.cargarDatosMongo();
      },
      error: (err) => {
        this.mostrarMensaje(err.error?.detail || 'Error al guardar en MongoDB (verifica la VM de Mongo).', true);
      }
    });
  }

  guardarResenaMongo(): void {
    const etiquetas = this.formResenaMongo.etiquetasTexto
      ? this.formResenaMongo.etiquetasTexto.split(',').map((t) => t.trim()).filter((t) => t.length > 0)
      : [];

    const payload: ResenaNoSql = {
      idReserva: this.formResenaMongo.idReserva,
      idCliente: this.formResenaMongo.idCliente,
      idEspacio: this.formResenaMongo.idEspacio,
      calificacionGeneral: Number(this.formResenaMongo.calificacionGeneral),
      calificaciones: {
        espacio: Number(this.formResenaMongo.calEspacio),
        servicios: Number(this.formResenaMongo.calServicios),
        coordinacion: Number(this.formResenaMongo.calCoordinacion),
        precio: Number(this.formResenaMongo.calPrecio)
      },
      comentario: this.formResenaMongo.comentario,
      recomendaria: this.formResenaMongo.recomendaria,
      etiquetas
    };

    this.backendService.guardarResena(payload).subscribe({
      next: (res) => {
        this.modalActivo = 'ninguno';
        this.mostrarMensaje(`Reseña (${res.calificacionGeneral}/5 ★) registrada en MongoDB para la reserva #${res.idReserva}.`);
        this.cargarDatosMongo();
      },
      error: (err) => {
        this.mostrarMensaje(err.error?.detail || 'Error al registrar la reseña en MongoDB.', true);
      }
    });
  }

  sincronizarSemillaMongo(): void {
    this.backendService.inicializarMongo().subscribe({
      next: () => {
        this.mostrarMensaje('Índices y documentos semilla verificados en MongoDB.');
        this.cargarDatosMongo();
      },
      error: () => {
        this.mostrarMensaje('No se pudo conectar con MongoDB. Verifica que el contenedor esté activo.', true);
      }
    });
  }

  probarRutinasRdbms(): void {
    this.backendService.ejecutarPruebasRdbms().subscribe({
      next: (res) => {
        this.mostrarMensaje(`[PostgreSQL EDT 7.1] ${res.message} (${res.executionTimeMs} ms)`);
      },
      error: (err) => {
        this.mostrarMensaje(err.error?.error || 'Fallo al ejecutar la suite de pruebas RDBMS.', true);
      }
    });
  }
}
