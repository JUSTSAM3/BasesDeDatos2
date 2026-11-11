// ==============================================================================
// UNIVERSIDAD EL BOSQUE - BASES DE DATOS 2
// SISTEMA DE GESTIÓN DE RESERVAS DE ESPACIOS PARA EVENTOS
// SCRIPT MONGO 01: CREACIÓN DE COLECCIONES, VALIDADORES ($jsonSchema) E ÍNDICES
// ==============================================================================
// Contenido:
//   1. Selección de la base de datos documental 'reservas_eventos_docs'
//   2. Creación o actualización de 'detalles_evento' con $jsonSchema
//   3. Índices de rendimiento y unicidad sobre 'detalles_evento'
//   4. Creación o actualización de 'resenas' con $jsonSchema
//   5. Índices de rendimiento y unicidad sobre 'resenas'
// ==============================================================================

db = db.getSiblingDB("reservas_eventos_docs");

// ==============================================================================
// 1. COLECCIÓN: detalles_evento
//    Almacena un documento por reserva con información logística flexible:
//    espacio desnormalizado, servicios contratados, lista de invitados,
//    requerimientos especiales, cronograma (agenda) y atributos dinámicos.
// ==============================================================================

const validadorDetallesEvento = {
  $jsonSchema: {
    bsonType: "object",
    required: [
      "id_reserva",
      "id_cliente",
      "espacio",
      "tipo_evento",
      "nombre_evento",
      "fecha_inicio",
      "fecha_fin",
      "creado_en"
    ],
    properties: {
      id_reserva: {
        bsonType: "int",
        minimum: 1,
        description: "Referencia lógica a reservas.id_reserva en PostgreSQL"
      },
      id_cliente: {
        bsonType: "int",
        minimum: 1,
        description: "Referencia lógica a clientes.id_cliente en PostgreSQL"
      },
      tipo_evento: {
        enum: ["boda", "cumpleanos", "conferencia", "corporativo", "grado", "social", "otro"]
      },
      nombre_evento: {
        bsonType: "string",
        maxLength: 150
      },
      fecha_inicio: {
        bsonType: "date"
      },
      fecha_fin: {
        bsonType: "date"
      },
      num_invitados_estimado: {
        bsonType: "int",
        minimum: 0
      },
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
            restricciones_alimentarias: {
              bsonType: "array",
              items: { bsonType: "string" }
            }
          }
        }
      },
      requerimientos_especiales: {
        bsonType: "array",
        items: {
          bsonType: "object",
          required: ["tipo", "descripcion"],
          properties: {
            tipo: {
              enum: ["montaje", "tecnico", "alimentacion", "accesibilidad", "seguridad", "otro"]
            },
            descripcion: { bsonType: "string" },
            estado: {
              enum: ["pendiente", "en_proceso", "cumplido"]
            }
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
      datos_extra: {
        bsonType: "object",
        description: "Atributos adicionales variables según la naturaleza del evento"
      },
      estado_reserva: {
        enum: ["pendiente", "confirmada", "cancelada", "finalizada"]
      },
      creado_en: {
        bsonType: "date"
      },
      actualizado_en: {
        bsonType: "date"
      }
    }
  }
};

if (db.getCollectionNames().includes("detalles_evento")) {
  db.runCommand({
    collMod: "detalles_evento",
    validator: validadorDetallesEvento,
    validationLevel: "strict",
    validationAction: "error"
  });
} else {
  db.createCollection("detalles_evento", {
    validator: validadorDetallesEvento,
    validationLevel: "strict",
    validationAction: "error"
  });
}

// Índices de la colección detalles_evento (alineados con las entidades Java @Indexed)
db.detalles_evento.createIndex({ id_reserva: 1 }, { unique: true, name: "ux_id_reserva" });
db.detalles_evento.createIndex({ "espacio.id_espacio": 1, fecha_inicio: 1 }, { name: "ix_espacio_fecha" });
db.detalles_evento.createIndex({ tipo_evento: 1 }, { name: "ix_tipo_evento" });
db.detalles_evento.createIndex({ "servicios.id_servicio": 1 }, { name: "ix_servicios" });
db.detalles_evento.createIndex({ id_cliente: 1 }, { name: "ix_cliente" });

// ==============================================================================
// 2. COLECCIÓN: resenas
//    Almacena la evaluación post-evento emitida por el cliente (escala 1 a 5),
//    desglose por dimensión, etiquetas descriptivas y respuesta administrativa.
// ==============================================================================

const validadorResenas = {
  $jsonSchema: {
    bsonType: "object",
    required: ["id_reserva", "id_cliente", "id_espacio", "calificacion_general", "fecha_resena"],
    properties: {
      id_reserva: {
        bsonType: "int",
        minimum: 1
      },
      id_cliente: {
        bsonType: "int",
        minimum: 1
      },
      id_espacio: {
        bsonType: "int",
        minimum: 1
      },
      calificacion_general: {
        bsonType: "int",
        minimum: 1,
        maximum: 5
      },
      calificaciones: {
        bsonType: "object",
        properties: {
          espacio:      { bsonType: "int", minimum: 1, maximum: 5 },
          servicios:    { bsonType: "int", minimum: 1, maximum: 5 },
          coordinacion: { bsonType: "int", minimum: 1, maximum: 5 },
          precio:       { bsonType: "int", minimum: 1, maximum: 5 }
        }
      },
      comentario: {
        bsonType: "string",
        maxLength: 2000
      },
      recomendaria: {
        bsonType: "bool"
      },
      etiquetas: {
        bsonType: "array",
        items: { bsonType: "string" }
      },
      respuesta_admin: {
        bsonType: "object",
        required: ["texto", "id_empleado", "fecha"],
        properties: {
          texto:       { bsonType: "string" },
          id_empleado: { bsonType: "int" },
          fecha:       { bsonType: "date" }
        }
      },
      fecha_resena: {
        bsonType: "date"
      }
    }
  }
};

if (db.getCollectionNames().includes("resenas")) {
  db.runCommand({
    collMod: "resenas",
    validator: validadorResenas,
    validationLevel: "strict",
    validationAction: "error"
  });
} else {
  db.createCollection("resenas", {
    validator: validadorResenas,
    validationLevel: "strict",
    validationAction: "error"
  });
}

// Índices de la colección resenas
db.resenas.createIndex({ id_reserva: 1 }, { unique: true, name: "ux_resena_reserva" });
db.resenas.createIndex({ id_espacio: 1, calificacion_general: -1 }, { name: "ix_espacio_calificacion" });
db.resenas.createIndex({ id_cliente: 1 }, { name: "ix_resena_cliente" });

print("\nColecciones 'detalles_evento' y 'resenas' configuradas con validadores $jsonSchema e índices.");

