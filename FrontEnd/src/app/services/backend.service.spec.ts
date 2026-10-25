import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BackendService, ReservaRequestDTO, PagoRequestDTO } from './backend.service';

describe('BackendService (Relacional)', () => {
  let service: BackendService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8080/api';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BackendService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(BackendService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe listar espacios desde /api/espacios/listar', () => {
    const mockEspacios = [{ idEspacio: 1, nombre: 'Salón Aura', capacidad: 120, tarifaPorHora: 250000 }];

    service.listarEspacios().subscribe((espacios) => {
      expect(espacios.length).toBe(1);
      expect(espacios[0].nombre).toBe('Salón Aura');
    });

    const req = httpMock.expectOne(`${apiUrl}/espacios/listar`);
    expect(req.request.method).toBe('GET');
    req.flush(mockEspacios);
  });

  it('debe consultar disponibilidad en /api/reservas/disponibilidad', () => {
    const mockDisp = {
      idEspacio: 1,
      fechaInicio: '2026-11-10T10:00:00',
      fechaFin: '2026-11-10T14:00:00',
      disponible: true,
      mensaje: 'Disponible'
    };

    service.consultarDisponibilidad(1, '2026-11-10T10:00:00', '2026-11-10T14:00:00').subscribe((res) => {
      expect(res.disponible).toBe(true);
    });

    const req = httpMock.expectOne(
      `${apiUrl}/reservas/disponibilidad?idEspacio=1&fechaInicio=2026-11-10T10:00:00&fechaFin=2026-11-10T14:00:00`
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockDisp);
  });

  it('debe crear una reserva en /api/reservas/crear', () => {
    const dto: ReservaRequestDTO = {
      idCliente: 1,
      idEspacio: 1,
      idEmpleado: 1,
      fechaInicio: '2026-11-10T10:00:00',
      fechaFin: '2026-11-10T14:00:00',
      servicios: []
    };

    service.crearReserva(dto).subscribe((res) => {
      expect(res.idReserva).toBe(10);
      expect(res.estado).toBe('pendiente');
    });

    const req = httpMock.expectOne(`${apiUrl}/reservas/crear`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...dto, idReserva: 10, estado: 'pendiente', costoTotal: 1000000 });
  });

  it('debe registrar un pago en /api/pagos/registrar', () => {
    const pagoDto: PagoRequestDTO = {
      idReserva: 10,
      tipoPago: 'anticipo',
      monto: 300000,
      metodoPago: 'transferencia'
    };

    service.registrarPago(pagoDto).subscribe((res) => {
      expect(res.idPago).toBe(5);
      expect(res.estadoReserva).toBe('confirmada');
    });

    const req = httpMock.expectOne(`${apiUrl}/pagos/registrar`);
    expect(req.request.method).toBe('POST');
    req.flush({
      idPago: 5,
      idReserva: 10,
      idFactura: 3,
      numeroFactura: 'FAC-003',
      monto: 300000,
      tipoPago: 'anticipo',
      metodoPago: 'transferencia',
      fechaPago: '2026-10-25T12:00:00',
      estadoReserva: 'confirmada',
      estadoPago: 'Anticipo pagado'
    });
  });
});
