import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {
  BackendService,
  Cliente,
  DisponibilidadResponseDTO,
  Espacio,
  ReservaRequestDTO,
  ReservaResponseDTO,
  ServicioAdicional
} from '../../services/backend.service';

export interface EspacioCard {
  idEspacio: number;
  nombre: string;
  tipo: string;
  capacidad: number;
  tarifaPorHora: number;
  precio: string;
  estilo: string;
}

export interface ServicioCard {
  idServicio: number;
  numero: string;
  nombre: string;
  descripcion: string;
  precioUnitario?: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {

  espacios: EspacioCard[] = [
    {
      idEspacio: 1,
      nombre: 'Salón Aura',
      tipo: 'Eventos sociales',
      capacidad: 120,
      tarifaPorHora: 250000,
      precio: '$250.000',
      estilo: 'aura'
    },
    {
      idEspacio: 2,
      nombre: 'Terraza Nómada',
      tipo: 'Eventos al aire libre',
      capacidad: 80,
      tarifaPorHora: 190000,
      precio: '$190.000',
      estilo: 'nomada'
    },
    {
      idEspacio: 3,
      nombre: 'Studio 74',
      tipo: 'Eventos privados',
      capacidad: 40,
      tarifaPorHora: 130000,
      precio: '$130.000',
      estilo: 'studio'
    }
  ];

  servicios: ServicioCard[] = [
    {
      idServicio: 1,
      numero: '01',
      nombre: 'Catering',
      descripcion: 'Opciones gastronómicas para acompañar cada tipo de evento.',
      precioUnitario: 45000
    },
    {
      idServicio: 2,
      numero: '02',
      nombre: 'Decoración',
      descripcion: 'Ambientación pensada para transformar completamente el espacio.',
      precioUnitario: 120000
    },
    {
      idServicio: 3,
      numero: '03',
      nombre: 'Sonido',
      descripcion: 'Equipos y soluciones para música, presentaciones y celebraciones.',
      precioUnitario: 95000
    },
    {
      idServicio: 4,
      numero: '04',
      nombre: 'Producción',
      descripcion: 'Apoyo para organizar los detalles técnicos de tu experiencia.',
      precioUnitario: 150000
    }
  ];

  clientes: Cliente[] = [];

  // Formulario de disponibilidad (RF-05: fn_validar_disponibilidad)
  busqueda = {
    idEspacio: 1,
    fecha: '',
    horaInicio: '10:00',
    horaFin: '14:00',
    invitados: 80,
    tipoEvento: ''
  };

  consultandoDisponibilidad = false;
  resultadoDisponibilidad: DisponibilidadResponseDTO | null = null;
  errorDisponibilidad = '';

  // Modal de Reserva rápida (SP_CREAR_RESERVA)
  mostrarModalReserva = false;
  creandoReserva = false;
  mensajeReservaExito = '';
  mensajeReservaError = '';

  nuevaReserva = {
    idCliente: 1,
    idEspacio: 1,
    idEmpleado: 1,
    fecha: '',
    horaInicio: '10:00',
    horaFin: '14:00',
    idServicio: 0,
    cantidadServicio: 1
  };

  constructor(
    private backendService: BackendService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarDatosRelacionales();
  }

  cargarDatosRelacionales(): void {
    const estilos = ['aura', 'nomada', 'studio'];

    this.backendService.listarEspacios().subscribe({
      next: (data: Espacio[]) => {
        if (data && data.length > 0) {
          this.espacios = data.map((e, idx) => ({
            idEspacio: e.idEspacio ?? idx + 1,
            nombre: e.nombre,
            tipo: e.tipoEspacio?.nombre || 'Espacio para eventos',
            capacidad: e.capacidad,
            tarifaPorHora: e.tarifaPorHora,
            precio: '$' + Number(e.tarifaPorHora).toLocaleString('es-CO'),
            estilo: estilos[idx % estilos.length]
          }));
          this.busqueda.idEspacio = this.espacios[0].idEspacio;
          this.nuevaReserva.idEspacio = this.espacios[0].idEspacio;
        }
      },
      error: () => {
        // Mantiene datos de respaldo si el backend aún no está levantado
      }
    });

    this.backendService.listarServiciosAdicionales().subscribe({
      next: (data: ServicioAdicional[]) => {
        if (data && data.length > 0) {
          this.servicios = data.map((s, idx) => ({
            idServicio: s.idServicio ?? idx + 1,
            numero: String(idx + 1).padStart(2, '0'),
            nombre: s.nombre,
            descripcion: s.descripcion || 'Servicio complementario para tu evento.',
            precioUnitario: s.precioUnitario
          }));
        }
      },
      error: () => {}
    });

    this.backendService.listarClientes().subscribe({
      next: (data: Cliente[]) => {
        this.clientes = data || [];
        if (this.clientes.length > 0 && this.clientes[0].idCliente) {
          this.nuevaReserva.idCliente = this.clientes[0].idCliente;
        }
      },
      error: () => {}
    });
  }

  verificarDisponibilidad(): void {
    this.resultadoDisponibilidad = null;
    this.errorDisponibilidad = '';

    if (!this.busqueda.fecha) {
      this.errorDisponibilidad = 'Selecciona la fecha del evento para consultar disponibilidad.';
      return;
    }

    const fechaInicio = `${this.busqueda.fecha}T${this.busqueda.horaInicio}:00`;
    const fechaFin = `${this.busqueda.fecha}T${this.busqueda.horaFin}:00`;

    this.consultandoDisponibilidad = true;
    this.backendService.consultarDisponibilidad(
      Number(this.busqueda.idEspacio),
      fechaInicio,
      fechaFin
    ).subscribe({
      next: (res) => {
        this.consultandoDisponibilidad = false;
        this.resultadoDisponibilidad = res;
      },
      error: (err) => {
        this.consultandoDisponibilidad = false;
        this.errorDisponibilidad = err.error?.message || err.error?.detail || 'No se pudo conectar con el servidor para validar disponibilidad.';
      }
    });
  }

  abrirModalReserva(espacio?: EspacioCard): void {
    this.mensajeReservaExito = '';
    this.mensajeReservaError = '';
    if (espacio) {
      this.nuevaReserva.idEspacio = espacio.idEspacio;
    } else if (this.busqueda.idEspacio) {
      this.nuevaReserva.idEspacio = Number(this.busqueda.idEspacio);
    }
    if (this.busqueda.fecha) {
      this.nuevaReserva.fecha = this.busqueda.fecha;
      this.nuevaReserva.horaInicio = this.busqueda.horaInicio;
      this.nuevaReserva.horaFin = this.busqueda.horaFin;
    }
    this.mostrarModalReserva = true;
  }

  cerrarModalReserva(): void {
    this.mostrarModalReserva = false;
  }

  confirmarReserva(): void {
    this.mensajeReservaExito = '';
    this.mensajeReservaError = '';

    if (!this.nuevaReserva.fecha) {
      this.mensajeReservaError = 'Debes seleccionar una fecha válida.';
      return;
    }

    const payload: ReservaRequestDTO = {
      idCliente: Number(this.nuevaReserva.idCliente),
      idEspacio: Number(this.nuevaReserva.idEspacio),
      idEmpleado: this.nuevaReserva.idEmpleado ? Number(this.nuevaReserva.idEmpleado) : null,
      fechaInicio: `${this.nuevaReserva.fecha}T${this.nuevaReserva.horaInicio}:00`,
      fechaFin: `${this.nuevaReserva.fecha}T${this.nuevaReserva.horaFin}:00`,
      servicios: Number(this.nuevaReserva.idServicio) > 0
        ? [{ idServicio: Number(this.nuevaReserva.idServicio), cantidad: Number(this.nuevaReserva.cantidadServicio) || 1 }]
        : []
    };

    this.creandoReserva = true;
    this.backendService.crearReserva(payload).subscribe({
      next: (res: ReservaResponseDTO) => {
        this.creandoReserva = false;
        this.mensajeReservaExito = `¡Reserva #${res.idReserva} creada! Estado: ${res.estado} | Costo Total: $${Number(res.costoTotal).toLocaleString('es-CO')}`;
      },
      error: (err) => {
        this.creandoReserva = false;
        this.mensajeReservaError = err.error?.message || err.error?.detail || 'Error al crear la reserva en la base de datos.';
      }
    });
  }

  irADashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
