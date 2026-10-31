package co.edu.unbosque.reservasbackend.nosql.service;

import co.edu.unbosque.reservasbackend.nosql.document.DetalleEvento;
import co.edu.unbosque.reservasbackend.nosql.document.Resena;
import co.edu.unbosque.reservasbackend.nosql.repository.DetalleEventoRepository;
import co.edu.unbosque.reservasbackend.nosql.repository.ResenaRepository;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio de logica documental en MongoDB (RF-16, RF-17, RF-18).
 * Gestiona operaciones CRUD, consultas complejas, pipelines de agregacion
 * e inicializacion automatica de indices y datos semilla.
 */
@Service
public class EventoNoSqlService {

    private final DetalleEventoRepository detalleEventoRepository;
    private final ResenaRepository resenaRepository;
    private final MongoTemplate mongoTemplate;

    public EventoNoSqlService(DetalleEventoRepository detalleEventoRepository,
                             ResenaRepository resenaRepository,
                             MongoTemplate mongoTemplate) {
        this.detalleEventoRepository = detalleEventoRepository;
        this.resenaRepository = resenaRepository;
        this.mongoTemplate = mongoTemplate;
    }

    // =========================================================================
    // 1. CRUD COLECCION: detalles_evento (RF-16)
    // =========================================================================

    public DetalleEvento guardarOActualizarDetalleEvento(DetalleEvento detalle) {
        if (detalle.getIdReserva() == null || detalle.getIdReserva() < 1) {
            throw new IllegalArgumentException("El idReserva es obligatorio y debe ser mayor a 0");
        }
        if (detalle.getIdCliente() == null || detalle.getIdCliente() < 1) {
            detalle.setIdCliente(1);
        }
        if (detalle.getTipoEvento() == null || detalle.getTipoEvento().isBlank()) {
            detalle.setTipoEvento("social");
        }
        if (detalle.getNombreEvento() == null || detalle.getNombreEvento().isBlank()) {
            detalle.setNombreEvento("Evento Reserva #" + detalle.getIdReserva());
        }
        if (detalle.getFechaInicio() == null) {
            detalle.setFechaInicio(Instant.now().plus(7, ChronoUnit.DAYS));
        }
        if (detalle.getFechaFin() == null) {
            detalle.setFechaFin(detalle.getFechaInicio().plus(4, ChronoUnit.HOURS));
        }
        if (detalle.getEspacio() == null) {
            DetalleEvento.EspacioEmbed esp = new DetalleEvento.EspacioEmbed();
            esp.setIdEspacio(1);
            esp.setNombre("Salón Imperial Aura");
            esp.setTipo("Salón de Eventos Sociales");
            esp.setIdSucursal(1);
            esp.setCiudad("Bogotá");
            detalle.setEspacio(esp);
        } else {
            if (detalle.getEspacio().getIdEspacio() == null) detalle.getEspacio().setIdEspacio(1);
            if (detalle.getEspacio().getNombre() == null) detalle.getEspacio().setNombre("Espacio Principal");
            if (detalle.getEspacio().getIdSucursal() == null) detalle.getEspacio().setIdSucursal(1);
        }

        Optional<DetalleEvento> existente = detalleEventoRepository.findByIdReserva(detalle.getIdReserva());
        if (existente.isPresent()) {
            DetalleEvento actual = existente.get();
            detalle.setId(actual.getId());
            detalle.setCreadoEn(actual.getCreadoEn() != null ? actual.getCreadoEn() : Instant.now());
        } else if (detalle.getCreadoEn() == null) {
            detalle.setCreadoEn(Instant.now());
        }
        detalle.setActualizadoEn(Instant.now());

        if (detalle.getEstadoReserva() == null || detalle.getEstadoReserva().isBlank()) {
            detalle.setEstadoReserva("confirmada");
        }

        return detalleEventoRepository.save(detalle);
    }

    public Optional<DetalleEvento> obtenerDetallePorReserva(Integer idReserva) {
        return detalleEventoRepository.findByIdReserva(idReserva);
    }

    public List<DetalleEvento> listarDetallesEvento(String tipoEvento, Integer idCliente) {
        if (tipoEvento != null && !tipoEvento.isBlank()) {
            return detalleEventoRepository.findByTipoEvento(tipoEvento);
        }
        if (idCliente != null) {
            return detalleEventoRepository.findByIdCliente(idCliente);
        }
        return detalleEventoRepository.findAll();
    }

    public boolean eliminarDetallePorReserva(Integer idReserva) {
        Optional<DetalleEvento> existente = detalleEventoRepository.findByIdReserva(idReserva);
        if (existente.isPresent()) {
            detalleEventoRepository.delete(existente.get());
            return true;
        }
        return false;
    }

    // =========================================================================
    // 2. CRUD COLECCION: resenas (RF-17)
    // =========================================================================

    public Resena guardarOActualizarResena(Resena resena) {
        if (resena.getIdReserva() == null || resena.getIdReserva() < 1) {
            throw new IllegalArgumentException("El idReserva es obligatorio para registrar la reseña");
        }
        if (resena.getCalificacionGeneral() == null
                || resena.getCalificacionGeneral() < 1
                || resena.getCalificacionGeneral() > 5) {
            throw new IllegalArgumentException("La calificación general debe estar entre 1 y 5");
        }
        if (resena.getIdCliente() == null || resena.getIdCliente() < 1) {
            resena.setIdCliente(1);
        }
        if (resena.getIdEspacio() == null || resena.getIdEspacio() < 1) {
            resena.setIdEspacio(1);
        }
        if (resena.getFechaResena() == null) {
            resena.setFechaResena(Instant.now());
        }

        Optional<Resena> existente = resenaRepository.findByIdReserva(resena.getIdReserva());
        existente.ifPresent(r -> resena.setId(r.getId()));

        return resenaRepository.save(resena);
    }

    public Resena responderResenaAdmin(Integer idReserva, Resena.RespuestaAdmin respuesta) {
        Resena resena = resenaRepository.findByIdReserva(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("No existe reseña para la reserva #" + idReserva));

        if (respuesta.getFecha() == null) {
            respuesta.setFecha(Instant.now());
        }
        if (respuesta.getIdEmpleado() == null) {
            respuesta.setIdEmpleado(1);
        }
        resena.setRespuestaAdmin(respuesta);
        return resenaRepository.save(resena);
    }

    public List<Resena> listarResenas(Integer idEspacio, Integer idCliente) {
        if (idEspacio != null) {
            return resenaRepository.findByIdEspacio(idEspacio);
        }
        if (idCliente != null) {
            return resenaRepository.findByIdCliente(idCliente);
        }
        return resenaRepository.findAll();
    }

    // =========================================================================
    // 3. PIPELINES DE AGREGACION (RF-18)
    // =========================================================================

    public List<Map<String, Object>> reporteEspaciosMasReservados() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("estado_reserva").ne("cancelada")),
                Aggregation.group("espacio.id_espacio", "espacio.nombre", "espacio.ciudad")
                        .count().as("totalEventos")
                        .sum("num_invitados_estimado").as("totalInvitadosEstimados")
                        .addToSet("tipo_evento").as("tiposEventoAtendidos"),
                Aggregation.project("totalEventos", "totalInvitadosEstimados", "tiposEventoAtendidos")
                        .and("_id.id_espacio").as("idEspacio")
                        .and("_id.nombre").as("nombreEspacio")
                        .and("_id.ciudad").as("ciudad")
                        .andExclude("_id"),
                Aggregation.sort(Sort.Direction.DESC, "totalEventos", "totalInvitadosEstimados")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "detalles_evento", Document.class);
        return new ArrayList<>(results.getMappedResults());
    }

    public List<Map<String, Object>> reporteServiciosMasSolicitados() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("estado_reserva").ne("cancelada")),
                Aggregation.unwind("servicios"),
                Aggregation.group("servicios.id_servicio", "servicios.nombre")
                        .count().as("eventosQueLoSolicitaron")
                        .sum("servicios.cantidad").as("unidadesTotalesSolicitadas"),
                Aggregation.project("eventosQueLoSolicitaron", "unidadesTotalesSolicitadas")
                        .and("_id.id_servicio").as("idServicio")
                        .and("_id.nombre").as("nombreServicio")
                        .andExclude("_id"),
                Aggregation.sort(Sort.Direction.DESC, "unidadesTotalesSolicitadas", "eventosQueLoSolicitaron")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "detalles_evento", Document.class);
        return new ArrayList<>(results.getMappedResults());
    }

    public List<Map<String, Object>> reporteCalificacionesPorEspacio() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.lookup("detalles_evento", "id_reserva", "id_reserva", "eventoInfo"),
                Aggregation.unwind("eventoInfo", true),
                Aggregation.group("id_espacio")
                        .first("eventoInfo.espacio.nombre").as("nombreEspacio")
                        .count().as("totalResenas")
                        .avg("calificacion_general").as("promedioGeneral")
                        .avg("calificaciones.espacio").as("promedioEspacio")
                        .avg("calificaciones.servicios").as("promedioServicios")
                        .avg("calificaciones.coordinacion").as("promedioCoordinacion")
                        .avg("calificaciones.precio").as("promedioPrecio"),
                Aggregation.project("nombreEspacio", "totalResenas", "promedioGeneral", "promedioEspacio",
                                "promedioServicios", "promedioCoordinacion", "promedioPrecio")
                        .and("_id").as("idEspacio")
                        .andExclude("_id"),
                Aggregation.sort(Sort.Direction.DESC, "promedioGeneral", "totalResenas")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "resenas", Document.class);
        return new ArrayList<>(results.getMappedResults());
    }

    // =========================================================================
    // 4. INICIALIZACION AUTOMATICA DE INDICES Y DATOS SEMILLA
    // =========================================================================

    public Map<String, Object> inicializarEsquemaYDatosSemilla() {
        asegurarIndices();

        long detallesAntes = detalleEventoRepository.count();
        long resenasAntes = resenaRepository.count();
        int detallesInsertados = 0;
        int resenasInsertadas = 0;

        if (detallesAntes == 0) {
            detallesInsertados = insertarDetallesSemilla();
        }
        if (resenasAntes == 0) {
            resenasInsertadas = insertarResenasSemilla();
        }

        return Map.of(
                "status", "OK",
                "detallesEventoTotal", detalleEventoRepository.count(),
                "detallesEventoInsertados", detallesInsertados,
                "resenasTotal", resenaRepository.count(),
                "resenasInsertadas", resenasInsertadas
        );
    }

    private void asegurarIndices() {
        mongoTemplate.indexOps("detalles_evento").createIndex(
                new Index().on("id_reserva", Sort.Direction.ASC).unique().named("ux_id_reserva")
        );
        mongoTemplate.indexOps("detalles_evento").createIndex(
                new Index().on("espacio.id_espacio", Sort.Direction.ASC)
                        .on("fecha_inicio", Sort.Direction.ASC).named("ix_espacio_fecha")
        );
        mongoTemplate.indexOps("detalles_evento").createIndex(
                new Index().on("tipo_evento", Sort.Direction.ASC).named("ix_tipo_evento")
        );
        mongoTemplate.indexOps("detalles_evento").createIndex(
                new Index().on("servicios.id_servicio", Sort.Direction.ASC).named("ix_servicios")
        );
        mongoTemplate.indexOps("detalles_evento").createIndex(
                new Index().on("id_cliente", Sort.Direction.ASC).named("ix_cliente")
        );

        mongoTemplate.indexOps("resenas").createIndex(
                new Index().on("id_reserva", Sort.Direction.ASC).unique().named("ux_resena_reserva")
        );
        mongoTemplate.indexOps("resenas").createIndex(
                new Index().on("id_espacio", Sort.Direction.ASC)
                        .on("calificacion_general", Sort.Direction.DESC).named("ix_espacio_calificacion")
        );
        mongoTemplate.indexOps("resenas").createIndex(
                new Index().on("id_cliente", Sort.Direction.ASC).named("ix_resena_cliente")
        );
    }

    private int insertarDetallesSemilla() {
        Instant ahora = Instant.now();

        DetalleEvento boda = new DetalleEvento();
        boda.setIdReserva(101);
        boda.setIdCliente(1);
        boda.setTipoEvento("boda");
        boda.setNombreEvento("Boda Rodríguez - Torres");
        boda.setFechaInicio(ahora.plus(10, ChronoUnit.DAYS));
        boda.setFechaFin(ahora.plus(10, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS));
        boda.setNumInvitadosEstimado(150);
        boda.setEstadoReserva("confirmada");
        boda.setCreadoEn(ahora);
        boda.setActualizadoEn(ahora);

        DetalleEvento.EspacioEmbed esp1 = new DetalleEvento.EspacioEmbed();
        esp1.setIdEspacio(1);
        esp1.setNombre("Salón Imperial Aura");
        esp1.setTipo("Salón de Eventos Sociales");
        esp1.setIdSucursal(1);
        esp1.setCiudad("Bogotá");
        boda.setEspacio(esp1);

        DetalleEvento.ServicioEmbed srvCatering = new DetalleEvento.ServicioEmbed();
        srvCatering.setIdServicio(1);
        srvCatering.setNombre("Catering Gourmet (Por Persona)");
        srvCatering.setCantidad(150);

        DetalleEvento.ServicioEmbed srvSonido = new DetalleEvento.ServicioEmbed();
        srvSonido.setIdServicio(2);
        srvSonido.setNombre("Sonido e Iluminación Profesional");
        srvSonido.setCantidad(1);

        DetalleEvento.ServicioEmbed srvFlores = new DetalleEvento.ServicioEmbed();
        srvFlores.setIdServicio(3);
        srvFlores.setNombre("Decoración Floral y Mobiliario");
        srvFlores.setCantidad(1);
        boda.setServicios(List.of(srvCatering, srvSonido, srvFlores));

        DetalleEvento.InvitadoEmbed inv1 = new DetalleEvento.InvitadoEmbed();
        inv1.setNombre("Camila Gómez");
        inv1.setEmail("camila.gomez@mail.com");
        inv1.setConfirmado(true);
        inv1.setMesa(1);
        inv1.setRestriccionesAlimentarias(List.of("vegetariana"));

        DetalleEvento.InvitadoEmbed inv2 = new DetalleEvento.InvitadoEmbed();
        inv2.setNombre("Andrés Felipe Ruiz");
        inv2.setEmail("andres.ruiz@mail.com");
        inv2.setConfirmado(true);
        inv2.setMesa(2);
        inv2.setRestriccionesAlimentarias(List.of("sin gluten"));
        boda.setInvitados(List.of(inv1, inv2));

        DetalleEvento.RequerimientoEspecialEmbed req1 = new DetalleEvento.RequerimientoEspecialEmbed();
        req1.setTipo("montaje");
        req1.setDescripcion("Arco floral blanco en entrada principal y pista LED");
        req1.setEstado("en_proceso");
        boda.setRequerimientosEspeciales(List.of(req1));

        DetalleEvento.ActividadAgendaEmbed ag1 = new DetalleEvento.ActividadAgendaEmbed();
        ag1.setHoraInicio(ahora.plus(10, ChronoUnit.DAYS));
        ag1.setHoraFin(ahora.plus(10, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS));
        ag1.setActividad("Ceremonia central y brindis");
        ag1.setResponsable("Diana Carolina Ríos");
        boda.setAgenda(List.of(ag1));

        boda.setDatosExtra(Map.of(
                "padrinos", List.of("Luis Rodríguez", "Ana María Pérez"),
                "tematica", "Clásico Romántico",
                "valsPrincipal", "Perfect - Ed Sheeran"
        ));

        DetalleEvento conferencia = new DetalleEvento();
        conferencia.setIdReserva(102);
        conferencia.setIdCliente(2);
        conferencia.setTipoEvento("conferencia");
        conferencia.setNombreEvento("Summit de Inteligencia Artificial & Cloud 2026");
        conferencia.setFechaInicio(ahora.plus(15, ChronoUnit.DAYS));
        conferencia.setFechaFin(ahora.plus(15, ChronoUnit.DAYS).plus(5, ChronoUnit.HOURS));
        conferencia.setNumInvitadosEstimado(220);
        conferencia.setEstadoReserva("confirmada");
        conferencia.setCreadoEn(ahora);
        conferencia.setActualizadoEn(ahora);

        DetalleEvento.EspacioEmbed esp2 = new DetalleEvento.EspacioEmbed();
        esp2.setIdEspacio(3);
        esp2.setNombre("Auditorio Magno El Bosque");
        esp2.setTipo("Auditorio y Conferencias");
        esp2.setIdSucursal(3);
        esp2.setCiudad("Bogotá");
        conferencia.setEspacio(esp2);

        DetalleEvento.ServicioEmbed srvStream = new DetalleEvento.ServicioEmbed();
        srvStream.setIdServicio(5);
        srvStream.setNombre("Streaming y Grabación HD");
        srvStream.setCantidad(1);

        DetalleEvento.ServicioEmbed srvCoffee = new DetalleEvento.ServicioEmbed();
        srvCoffee.setIdServicio(1);
        srvCoffee.setNombre("Catering Gourmet (Por Persona)");
        srvCoffee.setCantidad(220);
        conferencia.setServicios(List.of(srvStream, srvCoffee, srvSonido));

        DetalleEvento.RequerimientoEspecialEmbed req2 = new DetalleEvento.RequerimientoEspecialEmbed();
        req2.setTipo("tecnico");
        req2.setDescripcion("Enlace dedicado simétrico de 500 Mbps y 4 micrófonos de solapa");
        req2.setEstado("cumplido");
        conferencia.setRequerimientosEspeciales(List.of(req2));

        conferencia.setDatosExtra(Map.of(
                "ponentes", List.of("Dra. Marta León (MIT)", "Ing. Julián Castro (TechNova)"),
                "requiereStreaming", true,
                "idiomaSimultaneo", "Español / Inglés"
        ));

        DetalleEvento corporativo = new DetalleEvento();
        corporativo.setIdReserva(103);
        corporativo.setIdCliente(2);
        corporativo.setTipoEvento("corporativo");
        corporativo.setNombreEvento("Lanzamiento Anual de Producto TechNova");
        corporativo.setFechaInicio(ahora.plus(20, ChronoUnit.DAYS));
        corporativo.setFechaFin(ahora.plus(20, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
        corporativo.setNumInvitadosEstimado(90);
        corporativo.setEstadoReserva("pendiente");
        corporativo.setCreadoEn(ahora);
        corporativo.setActualizadoEn(ahora);

        DetalleEvento.EspacioEmbed esp3 = new DetalleEvento.EspacioEmbed();
        esp3.setIdEspacio(2);
        esp3.setNombre("Terraza Nómada 93");
        esp3.setTipo("Terraza Lounge");
        esp3.setIdSucursal(2);
        esp3.setCiudad("Bogotá");
        corporativo.setEspacio(esp3);

        DetalleEvento.ServicioEmbed srvFoto = new DetalleEvento.ServicioEmbed();
        srvFoto.setIdServicio(4);
        srvFoto.setNombre("Fotografía y Video Cobertura Total");
        srvFoto.setCantidad(1);
        corporativo.setServicios(List.of(srvSonido, srvFoto));

        corporativo.setDatosExtra(Map.of(
                "empresaOrganizadora", "TechNova Soluciones S.A.S.",
                "codigoVestimenta", "Smart Casual",
                "patrocinadores", List.of("CloudCorp", "DevHub")
        ));

        detalleEventoRepository.saveAll(List.of(boda, conferencia, corporativo));
        return 3;
    }

    private int insertarResenasSemilla() {
        Instant ahora = Instant.now();

        Resena r1 = new Resena();
        r1.setIdReserva(101);
        r1.setIdCliente(1);
        r1.setIdEspacio(1);
        r1.setCalificacionGeneral(5);
        Resena.CalificacionesDetalle cal1 = new Resena.CalificacionesDetalle();
        cal1.setEspacio(5);
        cal1.setServicios(5);
        cal1.setCoordinacion(5);
        cal1.setPrecio(4);
        r1.setCalificaciones(cal1);
        r1.setComentario("El Salón Imperial Aura superó nuestras expectativas. El catering y la acústica fueron impecables.");
        r1.setRecomendaria(true);
        r1.setEtiquetas(List.of("excelente servicio", "puntualidad", "catering delicioso"));
        r1.setFechaResena(ahora.minus(2, ChronoUnit.DAYS));

        Resena.RespuestaAdmin resp1 = new Resena.RespuestaAdmin();
        resp1.setTexto("¡Muchas gracias Alejandro! Fue un honor acompañarlos en su boda.");
        resp1.setIdEmpleado(3);
        resp1.setFecha(ahora.minus(1, ChronoUnit.DAYS));
        r1.setRespuestaAdmin(resp1);

        Resena r2 = new Resena();
        r2.setIdReserva(102);
        r2.setIdCliente(2);
        r2.setIdEspacio(3);
        r2.setCalificacionGeneral(5);
        Resena.CalificacionesDetalle cal2 = new Resena.CalificacionesDetalle();
        cal2.setEspacio(5);
        cal2.setServicios(5);
        cal2.setCoordinacion(4);
        cal2.setPrecio(5);
        r2.setCalificaciones(cal2);
        r2.setComentario("El Auditorio Magno tiene conectividad perfecta para streaming internacional y gran comodidad.");
        r2.setRecomendaria(true);
        r2.setEtiquetas(List.of("tecnología", "auditorio amplio", "buen sonido"));
        r2.setFechaResena(ahora.minus(1, ChronoUnit.DAYS));

        Resena r3 = new Resena();
        r3.setIdReserva(103);
        r3.setIdCliente(3);
        r3.setIdEspacio(2);
        r3.setCalificacionGeneral(4);
        Resena.CalificacionesDetalle cal3 = new Resena.CalificacionesDetalle();
        cal3.setEspacio(5);
        cal3.setServicios(4);
        cal3.setCoordinacion(4);
        cal3.setPrecio(4);
        r3.setCalificaciones(cal3);
        r3.setComentario("La vista desde Terraza Nómada 93 es espectacular para eventos al atardecer.");
        r3.setRecomendaria(true);
        r3.setEtiquetas(List.of("ambiente lounge", "buena ubicación"));
        r3.setFechaResena(ahora);

        resenaRepository.saveAll(List.of(r1, r2, r3));
        return 3;
    }
}

