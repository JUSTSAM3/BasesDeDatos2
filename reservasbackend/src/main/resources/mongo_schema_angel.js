// =====================================================================
// EDT 4.1.1 - Diseño de esquema de colecciones MongoDB
// Proyecto: Sistema de Gestión de Reservas de Espacios para Eventos
// Uso: mongosh < 4.1.1_esquema_colecciones.js
// Crea las colecciones con validación $jsonSchema e índices.
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

print("Colecciones detalles_evento y resenas creadas con validación e índices.");
