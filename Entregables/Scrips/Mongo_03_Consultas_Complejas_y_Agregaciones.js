// ==============================================================================
// UNIVERSIDAD EL BOSQUE - BASES DE DATOS 2
// SISTEMA DE GESTIÓN DE RESERVAS DE ESPACIOS PARA EVENTOS
// SCRIPT MONGO 03: CONSULTAS COMPLEJAS Y PIPELINES DE AGREGACIÓN
// ==============================================================================
// Contenido:
//   1. Consultas complejas con filtros combinados ($and, $in, $gte, $elemMatch),
//      proyecciones de campos y ordenamiento
//   2. Pipelines de agregación analítica ($match, $unwind, $group, $lookup,
//      $project, $sort) equivalentes a los endpoints del backend
// Uso:
//   mongosh < Mongo_03_Consultas_Complejas_y_Agregaciones.js
// ==============================================================================

db = db.getSiblingDB("reservas_eventos_docs");

// ==============================================================================
// 1. CONSULTAS COMPLEJAS (find CON FILTROS COMBINADOS Y PROYECCIÓN)
// ==============================================================================

// ------------------------------------------------------------------------------
// CONSULTA 1: Eventos confirmados de tipo conferencia, social o boda con más de
//             100 invitados en un rango de fechas, proyectando campos estratégicos
//             y excluyendo arreglos pesados como la lista detallada de invitados.
// ------------------------------------------------------------------------------


  db.detalles_evento.find(
    {
      $and: [
        { tipo_evento: { $in: ["conferencia", "social", "boda"] } },
        { estado_reserva: "confirmada" },
        { num_invitados_estimado: { $gte: 100 } },
        { fecha_inicio: { $gte: new Date("2026-01-01T00:00:00Z") } }
      ]
    },
    {
      id_reserva: 1,
      id_cliente: 1,
      tipo_evento: 1,
      nombre_evento: 1,
      "espacio.nombre": 1,
      "espacio.ciudad": 1,
      num_invitados_estimado: 1,
      fecha_inicio: 1,
      fecha_fin: 1,
      estado_reserva: 1,
      _id: 0
    }
  ).sort({ fecha_inicio: 1 }).toArray();

// ------------------------------------------------------------------------------
// CONSULTA 2: Búsqueda en subdocumentos embebidos con $elemMatch.
//             Eventos que tienen requerimientos especiales aún no cumplidos
//             (estado 'pendiente' o 'en_proceso') de tipo montaje o alimentación.
// ------------------------------------------------------------------------------
  db.detalles_evento.find(
    {
      estado_reserva: { $ne: "cancelada" },
      requerimientos_especiales: {
        $elemMatch: {
          tipo: { $in: ["montaje", "alimentacion", "seguridad"] },
          estado: { $in: ["pendiente", "en_proceso"] }
        }
      }
    },
    {
      id_reserva: 1,
      nombre_evento: 1,
      "espacio.nombre": 1,
      requerimientos_especiales: 1,
      _id: 0
    }
  ).sort({ id_reserva: 1 }).toArray();

// ------------------------------------------------------------------------------
// CONSULTA 3: Eventos con invitados confirmados que registran restricciones
//             alimentarias específicas, junto con reseñas destacadas (>= 4).
// ------------------------------------------------------------------------------
  db.detalles_evento.find(
    {
      invitados: {
        $elemMatch: {
          confirmado: true,
          restricciones_alimentarias: { $exists: true, $not: { $size: 0 } }
        }
      }
    },
    {
      id_reserva: 1,
      nombre_evento: 1,
      "invitados.nombre": 1,
      "invitados.mesa": 1,
      "invitados.restricciones_alimentarias": 1,
      _id: 0
    }
  ).toArray();

// ==============================================================================
// 2. PIPELINES DE AGREGACIÓN ANALÍTICA
// ==============================================================================

// ------------------------------------------------------------------------------
// AGREGACIÓN 1: Ranking de espacios más reservados (excluyendo canceladas),
//               total de invitados atendidos y diversidad de tipos de evento.
// ------------------------------------------------------------------------------
  db.detalles_evento.aggregate([
    { $match: { estado_reserva: { $ne: "cancelada" } } },
    {
      $group: {
        _id: {
          id_espacio: "$espacio.id_espacio",
          nombre: "$espacio.nombre",
          ciudad: "$espacio.ciudad"
        },
        totalEventos: { $sum: 1 },
        totalInvitadosEstimados: { $sum: "$num_invitados_estimado" },
        tiposEventoAtendidos: { $addToSet: "$tipo_evento" }
      }
    },
    {
      $project: {
        _id: 0,
        idEspacio: "$_id.id_espacio",
        nombreEspacio: "$_id.nombre",
        ciudad: "$_id.ciudad",
        totalEventos: 1,
        totalInvitadosEstimados: 1,
        tiposEventoAtendidos: 1
      }
    },
    { $sort: { totalEventos: -1, totalInvitadosEstimados: -1 } }
  ]).toArray();

// ------------------------------------------------------------------------------
// AGREGACIÓN 2: Servicios adicionales más solicitados desglosando el arreglo
//               embebido 'servicios' mediante $unwind.
// ------------------------------------------------------------------------------
  db.detalles_evento.aggregate([
    { $match: { estado_reserva: { $ne: "cancelada" } } },
    { $unwind: "$servicios" },
    {
      $group: {
        _id: {
          id_servicio: "$servicios.id_servicio",
          nombre: "$servicios.nombre"
        },
        eventosQueLoSolicitaron: { $sum: 1 },
        unidadesTotalesSolicitadas: { $sum: "$servicios.cantidad" }
      }
    },
    {
      $project: {
        _id: 0,
        idServicio: "$_id.id_servicio",
        nombreServicio: "$_id.nombre",
        eventosQueLoSolicitaron: 1,
        unidadesTotalesSolicitadas: 1
      }
    },
    { $sort: { unidadesTotalesSolicitadas: -1, eventosQueLoSolicitaron: -1 } }
  ]).toArray();

// ------------------------------------------------------------------------------
// AGREGACIÓN 3: Promedio de calificaciones multidimensionales por espacio
//               cruzando 'resenas' con 'detalles_evento' mediante $lookup.
// ------------------------------------------------------------------------------
  db.resenas.aggregate([
    {
      $lookup: {
        from: "detalles_evento",
        localField: "id_reserva",
        foreignField: "id_reserva",
        as: "eventoInfo"
      }
    },
    { $unwind: { path: "$eventoInfo", preserveNullAndEmptyArrays: true } },
    {
      $group: {
        _id: "$id_espacio",
        nombreEspacio: { $first: "$eventoInfo.espacio.nombre" },
        totalResenas: { $sum: 1 },
        promedioGeneral: { $avg: "$calificacion_general" },
        promedioEspacio: { $avg: "$calificaciones.espacio" },
        promedioServicios: { $avg: "$calificaciones.servicios" },
        promedioCoordinacion: { $avg: "$calificaciones.coordinacion" },
        promedioPrecio: { $avg: "$calificaciones.precio" }
      }
    },
    {
      $project: {
        _id: 0,
        idEspacio: "$_id",
        nombreEspacio: 1,
        totalResenas: 1,
        promedioGeneral: 1,
        promedioEspacio: 1,
        promedioServicios: 1,
        promedioCoordinacion: 1,
        promedioPrecio: 1
      }
    },
    { $sort: { promedioGeneral: -1, totalResenas: -1 } }
  ]).toArray()
;

