package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.dto.EspacioRequestDTO;
import co.edu.unbosque.reservasbackend.dto.EspacioResponseDTO;
import co.edu.unbosque.reservasbackend.dto.ServicioAdicionalResponseDTO;
import co.edu.unbosque.reservasbackend.dto.TipoEspacioResponseDTO;
import co.edu.unbosque.reservasbackend.sql.entity.Espacio;
import co.edu.unbosque.reservasbackend.sql.entity.ServicioAdicional;
import co.edu.unbosque.reservasbackend.sql.entity.Sucursal;
import co.edu.unbosque.reservasbackend.sql.entity.TipoEspacio;
import co.edu.unbosque.reservasbackend.sql.repository.EspacioRepository;
import co.edu.unbosque.reservasbackend.sql.repository.ServicioAdicionalRepository;
import co.edu.unbosque.reservasbackend.sql.repository.SucursalRepository;
import co.edu.unbosque.reservasbackend.sql.repository.TipoEspacioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EspacioService {

    private final EspacioRepository espacioRepository;
    private final SucursalRepository sucursalRepository;
    private final TipoEspacioRepository tipoEspacioRepository;
    private final ServicioAdicionalRepository servicioAdicionalRepository;

    public EspacioService(EspacioRepository espacioRepository,
                          SucursalRepository sucursalRepository,
                          TipoEspacioRepository tipoEspacioRepository,
                          ServicioAdicionalRepository servicioAdicionalRepository) {
        this.espacioRepository = espacioRepository;
        this.sucursalRepository = sucursalRepository;
        this.tipoEspacioRepository = tipoEspacioRepository;
        this.servicioAdicionalRepository = servicioAdicionalRepository;
    }

    @Transactional
    public EspacioResponseDTO crearEspacio(EspacioRequestDTO dto) {
        Sucursal sucursal = sucursalRepository.findById(dto.getIdSucursal())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + dto.getIdSucursal()));

        TipoEspacio tipoEspacio = tipoEspacioRepository.findById(dto.getIdTipoEspacio())
                .orElseThrow(() -> new RuntimeException("TipoEspacio no encontrado con ID: " + dto.getIdTipoEspacio()));

        Espacio espacio = new Espacio();
        espacio.setSucursal(sucursal);
        espacio.setNombre(dto.getNombre());
        espacio.setTipoEspacio(tipoEspacio);
        espacio.setCapacidad(dto.getCapacidad());
        espacio.setTarifaPorHora(dto.getTarifaPorHora());
        espacio.setEstado(dto.getEstado() != null ? dto.getEstado() : "disponible");

        Espacio guardado = espacioRepository.save(espacio);
        return mapToEspacioResponseDTO(guardado);
    }

    @Transactional(readOnly = true)
    public List<EspacioResponseDTO> obtenerTodos() {
        return espacioRepository.findAllWithSucursalAndTipo().stream()
                .map(this::mapToEspacioResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TipoEspacioResponseDTO> listarTipos() {
        return tipoEspacioRepository.findAll().stream()
                .map(this::mapToTipoEspacioResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicioAdicionalResponseDTO> listarServicios() {
        return servicioAdicionalRepository.findAll().stream()
                .map(this::mapToServicioAdicionalResponseDTO)
                .toList();
    }

    private EspacioResponseDTO mapToEspacioResponseDTO(Espacio e) {
        Integer idSucursal = e.getSucursal() != null ? e.getSucursal().getIdSucursal() : null;
        String nombreSucursal = e.getSucursal() != null ? e.getSucursal().getNombre() : null;
        Integer idTipo = e.getTipoEspacio() != null ? e.getTipoEspacio().getIdTipoEspacio() : null;
        String nombreTipo = e.getTipoEspacio() != null ? e.getTipoEspacio().getNombre() : null;

        return new EspacioResponseDTO(
                e.getIdEspacio(),
                e.getNombre(),
                idSucursal,
                nombreSucursal,
                idTipo,
                nombreTipo,
                e.getCapacidad(),
                e.getTarifaPorHora(),
                e.getEstado()
        );
    }

    private TipoEspacioResponseDTO mapToTipoEspacioResponseDTO(TipoEspacio t) {
        return new TipoEspacioResponseDTO(
                t.getIdTipoEspacio(),
                t.getNombre(),
                t.getDescripcion()
        );
    }

    private ServicioAdicionalResponseDTO mapToServicioAdicionalResponseDTO(ServicioAdicional s) {
        return new ServicioAdicionalResponseDTO(
                s.getIdServicio(),
                s.getNombre(),
                s.getDescripcion(),
                s.getPrecioUnitario()
        );
    }
}
