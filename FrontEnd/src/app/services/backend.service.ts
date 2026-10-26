import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Sucursal {
  idSucursal?: number;
  nombre: string;
  direccion?: string;
  ciudad?: string;
  telefono?: string;
}

export interface TipoEspacio {
  idTipoEspacio?: number;
  nombre: string;
  descripcion?: string;
}

export interface Espacio {
  idEspacio?: number;
  sucursal?: Sucursal;
  nombre: string;
  tipoEspacio?: TipoEspacio;
  capacidad: number;
  tarifaPorHora: number;
  estado?: string;
}

export interface EspacioRequestDTO {
  idSucursal: number;
  nombre: string;
  idTipoEspacio: number;
  capacidad: number;
  tarifaPorHora: number;
  estado?: string;
}

export interface Cliente {
  idCliente?: number;
  tipoCliente?: string;
  identificacion: string;
  nombreRazonSocial: string;
  email?: string;
  telefono?: string;
}

export interface Empleado {
  idEmpleado?: number;
  nombre: string;
  cargo?: string;
  email?: string;
}

export interface ServicioAdicional {
  idServicio?: number;
  nombre: string;
  descripcion?: string;
  precioUnitario: number;
}

export interface ServicioAdicionalItemDTO {
  idServicio: number;
  cantidad: number;
}

export interface ReservaRequestDTO {
  idCliente: number;
  idEspacio: number;
  idEmpleado?: number | null;
  fechaInicio: string;
  fechaFin: string;
  servicios?: ServicioAdicionalItemDTO[];
}

export interface ReservaResponseDTO {
  idReserva: number;
  idCliente: number;
  idEspacio: number;
  idEmpleado?: number;
  fechaReserva?: string;
  fechaInicio: string;
  fechaFin: string;
  estado: string;
  costoTotal: number;
}

export interface DisponibilidadResponseDTO {
  idEspacio: number;
  fechaInicio: string;
  fechaFin: string;
  disponible: boolean;
  mensaje: string;
}

export interface PagoRequestDTO {
  idReserva: number;
  tipoPago: string;
  monto: number;
  metodoPago: string;
}

export interface PagoResponseDTO {
  idPago: number;
  idReserva: number;
  idFactura: number;
  numeroFactura: string;
  monto: number;
  tipoPago: string;
  metodoPago: string;
  fechaPago: string;
  estadoReserva: string;
  estadoPago: string;
}

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  probarConexion(): Observable<string> {
    return this.http.get(`${this.apiUrl}/test`, { responseType: 'text' });
  }

  // ==================== ESPACIOS & CATÁLOGOS ====================
  listarEspacios(): Observable<Espacio[]> {
    return this.http.get<Espacio[]>(`${this.apiUrl}/espacios/listar`);
  }

  crearEspacio(dto: EspacioRequestDTO): Observable<Espacio> {
    return this.http.post<Espacio>(`${this.apiUrl}/espacios/crear`, dto);
  }

  listarTiposEspacio(): Observable<TipoEspacio[]> {
    return this.http.get<TipoEspacio[]>(`${this.apiUrl}/espacios/tipos`);
  }

  listarServiciosAdicionales(): Observable<ServicioAdicional[]> {
    return this.http.get<ServicioAdicional[]>(`${this.apiUrl}/espacios/servicios`);
  }

  // ==================== RESERVAS (SPs & Funciones) ====================
  listarReservas(): Observable<ReservaResponseDTO[]> {
    return this.http.get<ReservaResponseDTO[]>(`${this.apiUrl}/reservas/listar`);
  }

  obtenerReserva(id: number): Observable<ReservaResponseDTO> {
    return this.http.get<ReservaResponseDTO>(`${this.apiUrl}/reservas/${id}`);
  }

  crearReserva(dto: ReservaRequestDTO): Observable<ReservaResponseDTO> {
    return this.http.post<ReservaResponseDTO>(`${this.apiUrl}/reservas/crear`, dto);
  }

  consultarDisponibilidad(
    idEspacio: number,
    fechaInicio: string,
    fechaFin: string,
    idReservaExcluir?: number
  ): Observable<DisponibilidadResponseDTO> {
    let params = new HttpParams()
      .set('idEspacio', idEspacio)
      .set('fechaInicio', fechaInicio)
      .set('fechaFin', fechaFin);

    if (idReservaExcluir != null) {
      params = params.set('idReservaExcluir', idReservaExcluir);
    }

    return this.http.get<DisponibilidadResponseDTO>(`${this.apiUrl}/reservas/disponibilidad`, { params });
  }

  cancelarReserva(id: number): Observable<{ idReserva: number; estado: string; mensaje: string }> {
    return this.http.post<{ idReserva: number; estado: string; mensaje: string }>(
      `${this.apiUrl}/reservas/${id}/cancelar`,
      {}
    );
  }

  // ==================== PAGOS ====================
  registrarPago(dto: PagoRequestDTO): Observable<PagoResponseDTO> {
    return this.http.post<PagoResponseDTO>(`${this.apiUrl}/pagos/registrar`, dto);
  }

  consultarEstadoPago(idReserva: number): Observable<{ idReserva: number; estadoPago: string }> {
    return this.http.get<{ idReserva: number; estadoPago: string }>(`${this.apiUrl}/pagos/estado/${idReserva}`);
  }

  // ==================== CLIENTES ====================
  listarClientes(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(`${this.apiUrl}/clientes/listar`);
  }

  crearCliente(cliente: Cliente): Observable<Cliente> {
    return this.http.post<Cliente>(`${this.apiUrl}/clientes/crear`, cliente);
  }

  // ==================== EMPLEADOS ====================
  listarEmpleados(): Observable<Empleado[]> {
    return this.http.get<Empleado[]>(`${this.apiUrl}/empleados/listar`);
  }

  crearEmpleado(empleado: Empleado): Observable<Empleado> {
    return this.http.post<Empleado>(`${this.apiUrl}/empleados/crear`, empleado);
  }

  // ==================== SUCURSALES ====================
  listarSucursales(): Observable<Sucursal[]> {
    return this.http.get<Sucursal[]>(`${this.apiUrl}/sucursales/listar`);
  }

  crearSucursal(sucursal: Sucursal): Observable<Sucursal> {
    return this.http.post<Sucursal>(`${this.apiUrl}/sucursales/crear`, sucursal);
  }
}
