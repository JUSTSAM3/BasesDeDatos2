// =====================================================================
// EDT 4.1.1 / 4.2.1 - Esquema, Datos Semilla y Agregaciones MongoDB
// Proyecto: Sistema de Gestión de Reservas de Espacios para Eventos
// Uso: mongosh < mongo_schema_angel.js
// Crea las colecciones con validación $jsonSchema, índices, datos
// iniciales y ejecuta consultas complejas y pipelines de agregación.
// =====================================================================

db = db.getSiblingDB("reservas_eventos_docs");

// ---------------------------------------------------------------------
// 1. Colección: detalles_evento  (RF-16, RF-18)
//    Un documento por reserva. Información de estructura variable.
// ---------------------------------------------------------------------
db.createCollection("detalles_evento", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["id_reserva", "id_cliente", "espacio", "tipo_evento",
                 "nombre_evento", "fecha_inicio", "fecha_fin", "creado_en"],
      properties: {
        id_reserva:   { bsonType: "int", minimum: 1, description: "FK lógica a reservas.id_reserva (PostgreSQL)" },
        id_cliente:   { bsonType: "int", minimum: 1, description: "FK lógica a clientes.id_cliente" },
        tipo_evento:  { enum: ["boda", "cumpleanos", "conferencia", "corporativo", "grado", "social", "otro"] },
        nombre_evento:{ bsonType: "string", maxLength: 150 },
        fecha_inicio: { bsonType: "date" },
        fecha_fin:    { bsonType: "date" },
        num_invitados_estimado: { bsonType: "int", minimum: 0 },
        espacio: {
          bsonType: "object",
          required: ["id_espacio", "nombre", "id_sucursal"],
          properties: {
            id_espacio:  { bsonType: "int" },
            nombre:      { bsonType: "string" },
            tipo:        { bsonType: "string" },
            id_sucursal: { bsonType: "int" },
            ciudad:      { bsonType: "string" }
          }
        },
        servicios: {
          bsonType: "array",
          items: {
            bsonType: "object",
            required: ["id_servicio", "nombre", "cantidad"],
            properties: {
              id_servicio: { bsonType: "int" },
              nombre:      { bsonType: "string" },
              cantidad:    { bsonType: "int", minimum: 1 }
            }
          }
        },
        invitados: {
          bsonType: "array",
          maxItems: 2000,
          items: {
            bsonType: "object",
            required: ["nombre"],
            properties: {
              nombre:     { bsonType: "string", maxLength: 120 },
              email:      { bsonType: "string", pattern: "^.+@.+\\..+$" },
              telefono:   { bsonType: "string" },
              confirmado: { bsonType: "bool" },
              mesa:       { bsonType: "int" },
              restricciones_alimentarias: { bsonType: "array", items: { bsonType: "string" } }
            }
          }
        },
        requerimientos_especiales: {
          bsonType: "array",
          items: {
            bsonType: "object",
            required: ["tipo", "descripcion"],
            properties: {
              tipo:        { enum: ["montaje", "tecnico", "alimentacion", "accesibilidad", "seguridad", "otro"] },
              descripcion: { bsonType: "string" },
              estado:      { enum: ["pendiente", "en_proceso", "cumplido"] }
            }
          }
        },
        agenda: {
          bsonType: "array",
          items: {
            bsonType: "object",
            required: ["hora_inicio", "actividad"],
            properties: {
              hora_inicio: { bsonType: "date" },
              hora_fin:    { bsonType: "date" },
              actividad:   { bsonType: "string" },
              responsable: { bsonType: "string" }
            }
          }
        },
        datos_extra:    { bsonType: "object", description: "Campos libres según tipo_evento" },
        estado_reserva: { enum: ["pendiente", "confirmada", "cancelada", "finalizada"] },
        creado_en:      { bsonType: "date" },
        actualizado_en: { bsonType: "date" }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

db.detalles_evento.createIndex({ id_reserva: 1 }, { unique: true, name: "ux_id_reserva" });
db.detalles_evento.createIndex({ "espacio.id_espacio": 1, fecha_inicio: 1 }, { name: "ix_espacio_fecha" });
db.detalles_evento.createIndex({ tipo_evento: 1 }, { name: "ix_tipo_evento" });
db.detalles_evento.createIndex({ "servicios.id_servicio": 1 }, { name: "ix_servicios" });
db.detalles_evento.createIndex({ id_cliente: 1 }, { name: "ix_cliente" });

// ---------------------------------------------------------------------
// 2. Colección: resenas  (RF-17, RF-18)
//    Una reseña por reserva finalizada.
// ---------------------------------------------------------------------
db.createCollection("resenas", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["id_reserva", "id_cliente", "id_espacio", "calificacion_general", "fecha_resena"],
      properties: {
        id_reserva: { bsonType: "int", minimum: 1 },
        id_cliente: { bsonType: "int", minimum: 1 },
        id_espacio: { bsonType: "int", minimum: 1 },
        calificacion_general: { bsonType: "int", minimum: 1, maximum: 5 },
        calificaciones: {
          bsonType: "object",
          properties: {
            espacio:      { bsonType: "int", minimum: 1, maximum: 5 },
            servicios:    { bsonType: "int", minimum: 1, maximum: 5 },
            coordinacion: { bsonType: "int", minimum: 1, maximum: 5 },
            precio:       { bsonType: "int", minimum: 1, maximum: 5 }
          }
        },
        comentario:   { bsonType: "string", maxLength: 2000 },
        recomendaria: { bsonType: "bool" },
        etiquetas:    { bsonType: "array", items: { bsonType: "string" } },
        respuesta_admin: {
          bsonType: "object",
          required: ["texto", "id_empleado", "fecha"],
          properties: {
            texto:       { bsonType: "string" },
            id_empleado: { bsonType: "int" },
            fecha:       { bsonType: "date" }
          }
        },
        fecha_resena: { bsonType: "date" }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

db.resenas.createIndex({ id_reserva: 1 }, { unique: true, name: "ux_resena_reserva" });
db.resenas.createIndex({ id_espacio: 1, calificacion_general: -1 }, { name: "ix_espacio_calificacion" });
db.resenas.createIndex({ id_cliente: 1 }, { name: "ix_resena_cliente" });

// ---------------------------------------------------------------------
// 3. Operaciones CRUD: Inserción idempotente de datos semilla
// ---------------------------------------------------------------------
db.detalles_evento.updateOne(
  { id_reserva: Int32(101) },
  {
    $setOnInsert: {
      id_reserva: Int32(101),
      id_cliente: Int32(1),
      espacio: {
        id_espacio: Int32(1),
        nombre: "Salón Imperial Aura",
        tipo: "Salón de Eventos Sociales",
        id_sucursal: Int32(1),
        ciudad: "Bogotá"
      },
      tipo_evento: "boda",
      nombre_evento: "Boda Rodríguez - Torres",
      fecha_inicio: new Date("2026-11-21T14:00:00Z"),
      fecha_fin: new Date("2026-11-21T20:00:00Z"),
      num_invitados_estimado: Int32(150),
      servicios: [
        { id_servicio: Int32(1), nombre: "Catering Gourmet (Por Persona)", cantidad: Int32(150) },
        { id_servicio: Int32(2), nombre: "Sonido e Iluminación Profesional", cantidad: Int32(1) },
        { id_servicio: Int32(3), nombre: "Decoración Floral y Mobiliario", cantidad: Int32(1) }
      ],
      invitados: [
        { nombre: "Camila Gómez", email: "camila@mail.com", confirmado: true, mesa: Int32(1), restricciones_alimentarias: ["vegetariana"] },
        { nombre: "Andrés Ruiz", email: "andres@mail.com", confirmado: true, mesa: Int32(2), restricciones_alimentarias: ["sin gluten"] }
      ],
      requerimientos_especiales: [
        { tipo: "montaje", descripcion: "Arco floral blanco y pista LED", estado: "en_proceso" }
      ],
      agenda: [
        { hora_inicio: new Date("2026-11-21T14:30:00Z"), hora_fin: new Date("2026-11-21T16:00:00Z"), actividad: "Ceremonia y brindis", responsable: "Diana Ríos" }
      ],
      datos_extra: { padrinos: ["Luis Rodríguez", "Ana Pérez"], tematica: "Clásico Romántico" },
      estado_reserva: "confirmada",
      creado_en: new Date()
    }
  },
  { upsert: true }
);

db.detalles_evento.updateOne(
  { id_reserva: Int32(102) },
  {
    $setOnInsert: {
      id_reserva: Int32(102),
      id_cliente: Int32(2),
      espacio: {
        id_espacio: Int32(3),
        nombre: "Auditorio Magno El Bosque",
        tipo: "Auditorio y Conferencias",
        id_sucursal: Int32(3),
        ciudad: "Bogotá"
      },
      tipo_evento: "conferencia",
      nombre_evento: "Summit de Inteligencia Artificial & Cloud 2026",
      fecha_inicio: new Date("2026-11-25T09:00:00Z"),
      fecha_fin: new Date("2026-11-25T15:00:00Z"),
      num_invitados_estimado: Int32(220),
      servicios: [
        { id_servicio: Int32(5), nombre: "Streaming y Grabación HD", cantidad: Int32(1) },
        { id_servicio: Int32(1), nombre: "Catering Gourmet (Por Persona)", cantidad: Int32(220) },
        { id_servicio: Int32(2), nombre: "Sonido e Iluminación Profesional", cantidad: Int32(1) }
      ],
      requerimientos_especiales: [
        { tipo: "tecnico", descripcion: "Enlace dedicado simétrico 500 Mbps", estado: "cumplido" }
      ],
      datos_extra: { ponentes: ["Dra. Marta León", "Ing. Julián Castro"], requiereStreaming: true },
      estado_reserva: "confirmada",
      creado_en: new Date()
    }
  },
  { upsert: true }
);

db.resenas.updateOne(
  { id_reserva: Int32(101) },
  {
    $setOnInsert: {
      id_reserva: Int32(101),
      id_cliente: Int32(1),
      id_espacio: Int32(1),
      calificacion_general: Int32(5),
      calificaciones: { espacio: Int32(5), servicios: Int32(5), coordinacion: Int32(5), precio: Int32(4) },
      comentario: "El Salón Imperial Aura superó nuestras expectativas. Excelente coordinación.",
      recomendaria: true,
      etiquetas: ["excelente servicio", "puntualidad", "catering delicioso"],
      respuesta_admin: {
        texto: "¡Muchas gracias por confiar en nosotros!",
        id_empleado: Int32(3),
        fecha: new Date()
      },
      fecha_resena: new Date()
    }
  },
  { upsert: true }
);

db.resenas.updateOne(
  { id_reserva: Int32(102) },
  {
    $setOnInsert: {
      id_reserva: Int32(102),
      id_cliente: Int32(2),
      id_espacio: Int32(3),
      calificacion_general: Int32(5),
      calificaciones: { espacio: Int32(5), servicios: Int32(5), coordinacion: Int32(4), precio: Int32(5) },
      comentario: "Auditorio impecable con excelente conectividad para streaming.",
      recomendaria: true,
      etiquetas: ["tecnología", "auditorio amplio"],
      fecha_resena: new Date()
    }
  },
  { upsert: true }
);

// ---------------------------------------------------------------------
// 4. Pipelines de Agregación (RF-18)
// ---------------------------------------------------------------------

print("\n--- AGREGACIÓN 1: Espacios más reservados ---");
printjson(
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
    { $sort: { totalEventos: -1, totalInvitadosEstimados: -1 } }
  ]).toArray()
);

print("\n--- AGREGACIÓN 2: Servicios adicionales más solicitados ---");
printjson(
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
    { $sort: { unidadesTotalesSolicitadas: -1 } }
  ]).toArray()
);

print("\n--- AGREGACIÓN 3: Promedio de calificaciones por espacio (con $lookup a detalles_evento) ---");
printjson(
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
    { $sort: { promedioGeneral: -1, totalResenas: -1 } }
  ]).toArray()
);

print("\nColecciones detalles_evento y resenas creadas con validación, índices, datos semilla y agregaciones.");
