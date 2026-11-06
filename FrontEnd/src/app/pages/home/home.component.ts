import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {
  BackendService,
  DisponibilidadResponseDTO,
  Empleado,
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

  espacios: EspacioCard[] = [];
  cargandoEspacios = true;
  errorCargaEspacios = false;

  servicios: ServicioCard[] = [];

  // Formulario de disponibilidad
  busqueda = {
    idEspacio: 1,
    fecha: '',
    horaInicio: '10:00',
    horaFin: '14:00',
    invitados: 50,
    tipoEvento: ''
  };

  consultandoDisponibilidad = false;
  resultadoDisponibilidad: DisponibilidadResponseDTO | null = null;
  errorDisponibilidad = '';

  // Modal de Reserva rápida
  mostrarModalReserva = false;
  creandoReserva = false;
  mensajeReservaExito = '';
  mensajeReservaError = '';

  // Modal de Acceso Staff (Empleados)
  mostrarModalStaff = false;
  validandoStaff = false;
  errorStaff = '';
  credencialesStaff = {
    email: '',
    clave: ''
  };

  modoCliente: 'nuevo' | 'registrado' = 'nuevo';

  nuevaReserva = {
    idCliente: null as number | null,
    nuevoCliente: {
      tipoCliente: 'natural',
      identificacion: '',
      nombreRazonSocial: '',
      email: '',
      telefono: ''
    },
    idEspacio: 1,
    idEmpleado: null as number | null,
    fecha: '',
    horaInicio: '10:00',
    horaFin: '14:00',
    idServicio: 0,
    cantidadServicio: 1
  };

  constructor(
    private backendService: BackendService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarDatosRelacionales();
  }

  get espaciosFiltrados(): EspacioCard[] {
    return this.espacios.filter((e) => {
      const cumpleCapacidad = !this.busqueda.invitados || e.capacidad >= Number(this.busqueda.invitados);
      const cumpleTipo = !this.busqueda.tipoEvento || e.tipo.toLowerCase().includes(this.busqueda.tipoEvento.toLowerCase());
      return cumpleCapacidad && cumpleTipo;
    });
  }

  cargarDatosRelacionales(): void {
    const estilos = ['aura', 'nomada', 'studio'];
    this.cargandoEspacios = true;
    this.errorCargaEspacios = false;

    this.backendService.listarEspacios().subscribe({
      next: (data: Espacio[]) => {
        this.cargandoEspacios = false;
        if (data && data.length > 0) {
          this.espacios = data.map((e, idx) => ({
            idEspacio: e.idEspacio ?? idx + 1,
            nombre: e.nombre,
            tipo: e.nombreTipoEspacio || e.tipoEspacio?.nombre || 'Espacio para eventos',
            capacidad: e.capacidad,
            tarifaPorHora: e.tarifaPorHora,
            precio: '$' + Number(e.tarifaPorHora).toLocaleString('es-CO'),
            estilo: estilos[idx % estilos.length]
          }));
          if (this.espacios.length > 0) {
            this.busqueda.idEspacio = this.espacios[0].idEspacio;
            this.nuevaReserva.idEspacio = this.espacios[0].idEspacio;
          }
        } else {
          this.espacios = [];
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoEspacios = false;
        this.errorCargaEspacios = true;
        this.espacios = [];
        this.cdr.detectChanges();
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
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
      }
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
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.consultandoDisponibilidad = false;
        this.errorDisponibilidad = err.error?.message || err.error?.detail || 'No se pudo conectar con el servidor para validar disponibilidad.';
        this.cdr.detectChanges();
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
      this.mensajeReservaError = 'Debes seleccionar una fecha válida para la reserva.';
      return;
    }

    if (this.modoCliente === 'nuevo') {
      if (!this.nuevaReserva.nuevoCliente.identificacion || !this.nuevaReserva.nuevoCliente.nombreRazonSocial) {
        this.mensajeReservaError = 'Por favor ingresa tu nombre y número de documento / identificación.';
        return;
      }

      this.creandoReserva = true;
      this.backendService.crearCliente(this.nuevaReserva.nuevoCliente).subscribe({
        next: (cli) => {
          this.ejecutarCreacionReserva(cli.idCliente!);
        },
        error: (err) => {
          this.creandoReserva = false;
          this.mensajeReservaError = err.error?.message || 'No se pudo registrar el cliente. Verifica los datos.';
          this.cdr.detectChanges();
        }
      });
    } else {
      if (!this.nuevaReserva.idCliente) {
        this.mensajeReservaError = 'Por favor ingresa tu número o ID de cliente registrado.';
        return;
      }
      this.creandoReserva = true;
      this.ejecutarCreacionReserva(Number(this.nuevaReserva.idCliente));
    }
  }

  private ejecutarCreacionReserva(idCliente: number): void {
    const payload: ReservaRequestDTO = {
      idCliente: idCliente,
      idEspacio: Number(this.nuevaReserva.idEspacio),
      idEmpleado: null,
      fechaInicio: `${this.nuevaReserva.fecha}T${this.nuevaReserva.horaInicio}:00`,
      fechaFin: `${this.nuevaReserva.fecha}T${this.nuevaReserva.horaFin}:00`,
      servicios: Number(this.nuevaReserva.idServicio) > 0
        ? [{ idServicio: Number(this.nuevaReserva.idServicio), cantidad: Number(this.nuevaReserva.cantidadServicio) || 1 }]
        : []
    };

    this.backendService.crearReserva(payload).subscribe({
      next: (res: ReservaResponseDTO) => {
        this.creandoReserva = false;
        this.mensajeReservaExito = `¡Reserva #${res.idReserva} creada con éxito! Estado: ${res.estado} | Costo Total: $${Number(res.costoTotal).toLocaleString('es-CO')}`;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.creandoReserva = false;
        this.mensajeReservaError = err.error?.message || err.error?.detail || 'No fue posible procesar tu reserva. Verifica los datos seleccionados.';
        this.cdr.detectChanges();
      }
    });
  }

  irADashboard(): void {
    this.abrirModalStaff();
  }

  abrirModalStaff(event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    const sesionActiva = sessionStorage.getItem('staff_sesion');
    if (sesionActiva) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.errorStaff = '';
    this.credencialesStaff = { email: '', clave: '' };
    this.mostrarModalStaff = true;
    this.cdr.detectChanges();
  }

  cerrarModalStaff(): void {
    this.mostrarModalStaff = false;
    this.errorStaff = '';
  }

  iniciarSesionStaff(): void {
    this.errorStaff = '';
    const correo = this.credencialesStaff.email.trim().toLowerCase();
    const clave = this.credencialesStaff.clave.trim();

    if (!correo || !clave) {
      this.errorStaff = 'Ingresa tu correo corporativo y clave de acceso.';
      return;
    }

    this.validandoStaff = true;
    this.backendService.autenticarEmpleado({ email: correo, clave }).subscribe({
      next: (empleado: Empleado) => {
        this.validandoStaff = false;
        sessionStorage.setItem('staff_sesion', JSON.stringify(empleado));
        this.mostrarModalStaff = false;
        this.cdr.detectChanges();
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.validandoStaff = false;
        this.errorStaff =
          err?.error?.mensaje ||
          (err?.status === 401
            ? 'Acceso denegado: correo corporativo o clave no válidos.'
            : 'No fue posible verificar las credenciales en este momento.');
        this.cdr.detectChanges();
      }
    });
  }

  irASeccion(idSeccion: string, event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    const elemento = document.getElementById(idSeccion);
    if (elemento) {
      elemento.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
