// ==============================================================================
// UNIVERSIDAD EL BOSQUE - BASES DE DATOS 2
// SISTEMA DE GESTIÓN DE RESERVAS DE ESPACIOS PARA EVENTOS
// SCRIPT MONGO 02: DATOS SEMILLA Y OPERACIONES CRUD
// ==============================================================================
// Contenido:
//   1. Inserción de datos semilla en 'detalles_evento'
//   2. Inserción de datos semilla en 'resenas'
//   3. Operaciones CRUD completas (Create, Read, Update con $set/$push, Delete)
//   4. Demostración de validación estricta ($jsonSchema) ante datos inválidos
// ==============================================================================

db = db.getSiblingDB("reservas_eventos_docs");

// ==============================================================================
// 1. DATOS SEMILLA EN 'detalles_evento' (Idempotente con upsert)
//    Coherentes con los registros de PostgreSQL (clientes 1..3, espacios 1..4)
// ==============================================================================

// Evento 1: Boda en Salón Imperial Aura (Reserva #1 / #101)
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
        {
          nombre: "Camila Gómez",
          email: "camila@mail.com",
          confirmado: true,
          mesa: Int32(1),
          restricciones_alimentarias: ["vegetariana"]
        },
        {
          nombre: "Andrés Ruiz",
          email: "andres@mail.com",
          confirmado: true,
          mesa: Int32(2),
          restricciones_alimentarias: ["sin gluten"]
        }
      ],
      requerimientos_especiales: [
        { tipo: "montaje", descripcion: "Arco floral blanco y pista LED", estado: "en_proceso" },
        { tipo: "alimentacion", descripcion: "Menú especial sin gluten en mesa 2", estado: "pendiente" }
      ],
      agenda: [
        {
          hora_inicio: new Date("2026-11-21T14:30:00Z"),
          hora_fin: new Date("2026-11-21T16:00:00Z"),
          actividad: "Ceremonia central y brindis",
          responsable: "Diana Carolina Ríos"
        }
      ],
      datos_extra: {
        padrinos: ["Luis Rodríguez", "Ana María Pérez"],
        tematica: "Clásico Romántico"
      },
      estado_reserva: "confirmada",
      creado_en: new Date()
    }
  },
  { upsert: true }
);

// Evento 2: Conferencia en Auditorio Magno El Bosque (Reserva #2 / #102)
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
      datos_extra: {
        ponentes: ["Dra. Marta León", "Ing. Julián Castro"],
        requiereStreaming: true
      },
      estado_reserva: "confirmada",
      creado_en: new Date()
    }
  },
  { upsert: true }
);

// Evento 3: Lanzamiento Corporativo en Terraza Nómada 93 (Reserva #3 / #103)
db.detalles_evento.updateOne(
  { id_reserva: Int32(103) },
  {
    $setOnInsert: {
      id_reserva: Int32(103),
      id_cliente: Int32(2),
      espacio: {
        id_espacio: Int32(2),
        nombre: "Terraza Nómada 93",
        tipo: "Terraza Lounge",
        id_sucursal: Int32(2),
        ciudad: "Bogotá"
      },
      tipo_evento: "corporativo",
      nombre_evento: "Lanzamiento Anual de Producto TechNova",
      fecha_inicio: new Date("2026-11-28T17:00:00Z"),
      fecha_fin: new Date("2026-11-28T22:00:00Z"),
      num_invitados_estimado: Int32(90),
      servicios: [
        { id_servicio: Int32(2), nombre: "Sonido e Iluminación Profesional", cantidad: Int32(1) },
        { id_servicio: Int32(4), nombre: "Fotografía y Video Cobertura Total", cantidad: Int32(1) }
      ],
      requerimientos_especiales: [
        { tipo: "seguridad", descripcion: "Control de acceso con código QR en recepción", estado: "pendiente" }
      ],
      datos_extra: {
        empresaOrganizadora: "TechNova Soluciones S.A.S.",
        codigoVestimenta: "Smart Casual"
      },
      estado_reserva: "pendiente",
      creado_en: new Date()
    }
  },
  { upsert: true }
);

// ==============================================================================
// 2. DATOS SEMILLA EN 'resenas' (Idempotente con upsert)
// ==============================================================================

db.resenas.updateOne(
  { id_reserva: Int32(101) },
  {
    $setOnInsert: {
      id_reserva: Int32(101),
      id_cliente: Int32(1),
      id_espacio: Int32(1),
      calificacion_general: Int32(5),
      calificaciones: {
        espacio: Int32(5),
        servicios: Int32(5),
        coordinacion: Int32(5),
        precio: Int32(4)
      },
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
      calificaciones: {
        espacio: Int32(5),
        servicios: Int32(5),
        coordinacion: Int32(4),
        precio: Int32(5)
      },
      comentario: "Auditorio impecable con excelente conectividad para streaming.",
      recomendaria: true,
      etiquetas: ["tecnología", "auditorio amplio"],
      fecha_resena: new Date()
    }
  },
  { upsert: true }
);

db.resenas.updateOne(
  { id_reserva: Int32(103) },
  {
    $setOnInsert: {
      id_reserva: Int32(103),
      id_cliente: Int32(3),
      id_espacio: Int32(2),
      calificacion_general: Int32(4),
      calificaciones: {
        espacio: Int32(5),
        servicios: Int32(4),
        coordinacion: Int32(4),
        precio: Int32(4)
      },
      comentario: "La vista desde Terraza Nómada 93 es espectacular para eventos al atardecer.",
      recomendaria: true,
      etiquetas: ["ambiente lounge", "buena ubicación"],
      fecha_resena: new Date()
    }
  },
  { upsert: true }
);

// ==============================================================================
// 3. DEMOSTRACIÓN DE OPERACIONES CRUD (Create, Read, Update, Delete)
// ==============================================================================

print("\n--- 3.1 CRUD EN COLECCIÓN 'detalles_evento' ---");

// Limpieza previa por si existía de una corrida interrumpida
db.detalles_evento.deleteOne({ id_reserva: Int32(199) });

// CREATE (insertOne)
db.detalles_evento.insertOne({
  id_reserva: Int32(199),
  id_cliente: Int32(1),
  espacio: {
    id_espacio: Int32(2),
    nombre: "Terraza Nómada 93",
    tipo: "Terraza Lounge",
    id_sucursal: Int32(2),
    ciudad: "Bogotá"
  },
  tipo_evento: "social",
  nombre_evento: "Celebración de Prueba CRUD",
  fecha_inicio: new Date("2026-12-01T18:00:00Z"),
  fecha_fin: new Date("2026-12-01T23:00:00Z"),
  num_invitados_estimado: Int32(60),
  servicios: [
    { id_servicio: Int32(2), nombre: "Sonido e Iluminación Profesional", cantidad: Int32(1) }
  ],
  requerimientos_especiales: [
    { tipo: "montaje", descripcion: "Mobiliario tipo lounge", estado: "pendiente" }
  ],
  estado_reserva: "confirmada",
  creado_en: new Date()
});
print("Detalle de evento insertado.");

// READ (findOne)
print("[READ] Consultando documento recién insertado (id_reserva = 199):");
printjson(db.detalles_evento.findOne({ id_reserva: Int32(199) }));

// UPDATE ($set para atributos escalares y $push para arreglos embebidos)
db.detalles_evento.updateOne(
  { id_reserva: Int32(199) },
  {
    $set: {
      num_invitados_estimado: Int32(75),
      actualizado_en: new Date()
    },
    $push: {
      requerimientos_especiales: {
        tipo: "tecnico",
        descripcion: "Micrófono inalámbrico adicional",
        estado: "pendiente"
      }
    }
  }
);
print("[UPDATE] Documento actualizado ($set num_invitados_estimado=75 y $push requerimiento).");

// DELETE (deleteOne)
db.detalles_evento.deleteOne({ id_reserva: Int32(199) });
print("[DELETE] Documento de prueba eliminado de 'detalles_evento'.");


print("\n--- 3.2 CRUD EN COLECCIÓN 'resenas' ---");

db.resenas.deleteOne({ id_reserva: Int32(199) });

// CREATE (insertOne)
db.resenas.insertOne({
  id_reserva: Int32(199),
  id_cliente: Int32(1),
  id_espacio: Int32(2),
  calificacion_general: Int32(4),
  calificaciones: {
    espacio: Int32(4),
    servicios: Int32(5),
    coordinacion: Int32(4),
    precio: Int32(4)
  },
  comentario: "Excelente servicio y vista, muy recomendado.",
  recomendaria: true,
  etiquetas: ["vista panorámica", "buen servicio"],
  fecha_resena: new Date()
});
print("[CREATE] Reseña insertada.");

// READ (find)
print("[READ] Consultando reseña:");
printjson(db.resenas.findOne({ id_reserva: Int32(199) }));

// UPDATE ($set para registrar respuesta administrativa)
db.resenas.updateOne(
  { id_reserva: Int32(199) },
  {
    $set: {
      respuesta_admin: {
        texto: "¡Muchas gracias por sus comentarios! Nos alegra que disfrutaran de la Terraza.",
        id_empleado: Int32(3),
        fecha: new Date()
      }
    }
  }
);
print("[UPDATE] Respuesta administrativa agregada a la reseña.");

// DELETE (deleteOne)
db.resenas.deleteOne({ id_reserva: Int32(199) });
print("[DELETE] Reseña de prueba eliminada de 'resenas'.");

// ==============================================================================
// 4. DEMOSTRACIÓN DE RECHAZO POR VALIDADOR $jsonSchema (Integridad documental)
// ==============================================================================
print("\n--- 4. PRUEBA DE VALIDACIÓN $jsonSchema (Rechazo de calificación fuera de rango 1..5) ---");
try {
  db.resenas.insertOne({
    id_reserva: Int32(9999),
    id_cliente: Int32(1),
    id_espacio: Int32(1),
    calificacion_general: Int32(10), // Inválido: el máximo permitido por $jsonSchema es 5
    fecha_resena: new Date()
  });
  print("El documento inválido fue insertado (no debería ocurrir).");
} catch (err) {
  print("El validador $jsonSchema rechazó correctamente el documento inválido: " + err.message);
}

