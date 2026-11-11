# Sistema de Información Web para Gestión de Reservas de Espacios para Eventos

Proyecto de arquitectura de persistencia políglota (**PostgreSQL 16 + MongoDB 7.0**) con backend en **Spring Boot 4 (Java 21)** y frontend en **Angular 20**, desplegado sobre contenedores Docker en máquinas virtuales **Debian 12 (Bookworm)**.

---

## Equipo de Desarrollo — Universidad El Bosque

| Integrante | Rol en el Proyecto | Usuario GitHub | Correo Institucional |
| :--- | :--- | :--- | :--- |
| **Samuel Andrés Mesa Comaz** | Project Manager / Backend & Integración | `@JUSTSAM3` | `samesa@unbosque.edu.co` |
| **Samuel Camelo Araque** | Arquitecto RDBMS (PostgreSQL / PL/pgSQL) | `@Sasamuelll` | `scameloa@unbosque.edu.co` |
| **Miguel Ángel Alza Sanabria** | Arquitecto NoSQL (MongoDB) & Frontend | `@Malzap12` | `malzap@unbosque.edu.co` |

- **Asignatura:** Bases de Datos 2 — Programa de Ingeniería de Sistemas
- **Docente:** Ing. Christian Felipe Duarte

---

## Arquitectura del Sistema

El sistema implementa persistencia políglota separando responsabilidades según la naturaleza de los datos:

1. **Motor Relacional — PostgreSQL 16 (`reservas_db`):**
   - Modelo normalizado hasta **3FN** con **10 tablas**: `sucursales`, `tipos_espacio`, `espacios`, `clientes`, `empleados`, `servicios_adicionales`, `reservas`, `reserva_servicios`, `facturas` y `pagos`.
   - Control de concurrencia y prevención de doble reserva mediante restricción de exclusión **GiST (`btree_gist`)** sobre rangos temporales (`tsrange`) y disparadores en PL/pgSQL.
   - Lógica de negocio encapsulada en el motor: procedimientos transaccionales (`SP_CREAR_RESERVA`, `SP_REGISTRAR_PAGO`, `SP_CANCELAR_RESERVA`), funciones (`fn_validar_disponibilidad`, `fn_validar_horario`, `fn_calcular_costo_total`, `fn_estado_pago`), 6 triggers y la vista analítica `vw_historial_cliente`.
   - Esquema de seguridad basado en roles de menor privilegio (`app_reservas` para operación DML/ejecución de rutinas y `reportes_reservas` de solo lectura).

2. **Motor Documental — MongoDB 7.0 (`reservas_eventos_docs`):**
   - Colección **`detalles_evento`**: almacena la estructura logística variable de cada reserva (lista de invitados con restricciones alimentarias, requerimientos especiales por área, cronograma/agenda y atributos libres por tipo de evento) validada con `$jsonSchema`.
   - Colección **`resenas`**: almacena evaluaciones post-evento (calificación general 1–5, desglose multidimensional, etiquetas y respuesta administrativa), validando previamente en PostgreSQL que la reserva exista y esté en estado `'finalizada'`.
   - Sincronización automática entre motores: al cancelar una reserva en PostgreSQL se actualiza automáticamente `estado_reserva: "cancelada"` en MongoDB.

3. **Backend — Spring Boot (`reservasbackend/`):**
   - API RESTful en Java 21 con Spring Data JPA, JDBC (`JdbcTemplate` para invocación de SPs y funciones), Spring Data MongoDB (`MongoTemplate` para consultas avanzadas con proyección y pipelines de agregación) y migraciones versionadas con **Flyway (`V1` a `V7`)**.

4. **Frontend — Angular (`FrontEnd/`):**
   - Aplicación web SPA que permite explorar el catálogo de espacios, verificar disponibilidad en tiempo real, crear reservas con servicios adicionales, registrar pagos/facturación, gestionar la logística documental en MongoDB, publicar reseñas y visualizar reportes analíticos.

---

## Estructura del Repositorio

```text
BasesDeDatos2/
├── docker-compose.yml                  # Contenedor PostgreSQL 16 (Debian Bookworm)
├── docker-compose-mongo.yml            # Contenedor MongoDB 7.0
├── .env.example                        # Plantilla de variables de entorno (sin claves en texto plano)
├── Entregables/                        # Documentación, diagramas y scripts del proyecto
│   ├── Diagramas/                      # Modelos E-R, Relacional, Documental, C4 y Secuencia
│   ├── Documentos/                     # Informe Ejecutivo, SRS, Diccionario de Datos, etc.
│   └── Scrips/                         # Scripts consolidados de PostgreSQL (01..05) y MongoDB (01..03)
├── reservasbackend/                    # Backend Spring Boot 4 (Java 21)
│   └── src/main/resources/
│       ├── application.yml             # Configuración parametrizada por variables de entorno
│       ├── mongo_schema_angel.js       # Script integral de MongoDB (esquema, semilla, CRUD y agregaciones)
│       └── db/migration/               # Migraciones Flyway V1 a V7
└── FrontEnd/                           # Aplicación Web Angular 20
```

---

## Requisitos Previos

- **Docker** `>= 24.0` y **Docker Compose** `>= 2.20` (sobre Debian 12 Bookworm o entorno local).
- **Java JDK 21** (incluye Maven Wrapper `./mvnw`).
- **Node.js** `>= 20.x` y **npm** `>= 10.x`.
- Clientes opcionales de consola: `psql` (PostgreSQL) y `mongosh` (MongoDB Shell).

---

## Guía Paso a Paso de Configuración y Despliegue

### 1. Configurar Variables de Entorno

Copie la plantilla `.env.example` a `.env` en la raíz del proyecto y asigne contraseñas seguras:

```bash
cp .env.example .env
```

Variables definidas en `.env`:

| Variable | Descripción | Ejemplo / Valor por defecto |
| :--- | :--- | :--- |
| `POSTGRES_HOST` | IP o host de la VM/contenedor PostgreSQL | `localhost` o `192.168.80.27` |
| `POSTGRES_PORT` | Puerto de PostgreSQL | `5432` |
| `POSTGRES_DB` | Nombre de la base de datos relacional | `reservas_db` |
| `POSTGRES_USER` | Usuario administrador (usado por contenedor y Flyway) | `reservas_admin` |
| `POSTGRES_PASSWORD` | Contraseña del administrador de PostgreSQL | *(definir en `.env`)* |
| `APP_DB_USER` | Rol operativo de menor privilegio para el backend | `app_reservas` |
| `APP_DB_PASSWORD` | Contraseña del rol `app_reservas` | *(definir en `.env`)* |
| `MONGO_HOST` | IP o host de la VM/contenedor MongoDB | `localhost` |
| `MONGO_PORT` | Puerto de MongoDB | `27017` |
| `MONGO_DB` | Base de datos documental | `reservas_eventos_docs` |
| `MONGO_USER` | Usuario administrador de MongoDB | `admin` |
| `MONGO_PASSWORD` | Contraseña de MongoDB | *(definir en `.env`)* |
| `MONGO_URI` | URI completa de conexión a MongoDB | `mongodb://admin:<clave>@localhost:27017/reservas_eventos_docs?authSource=admin` |
| `STAFF_ACCESS_KEY` | Clave corporativa para acceso al panel de empleados | *(definir en `.env`)* |

---

### 2. Levantar las Bases de Datos con Docker Compose

Para levantar **PostgreSQL 16** (en la VM 1 o entorno local):

```bash
docker compose -f docker-compose.yml up -d
```

Para levantar **MongoDB 7.0** (en la VM 2 o entorno local):

```bash
docker compose -f docker-compose-mongo.yml up -d
```

Verificar que ambos contenedores estén en ejecución:

```bash
docker ps
```

---

### 3. Ejecución Manual de Scripts SQL y MongoDB (Opcional / Evaluación Directa)

Aunque **Flyway** aplica automáticamente las migraciones `V1` a `V7` al iniciar el backend, los scripts consolidados en `Entregables/Scrips/` pueden ejecutarse directamente en orden:

#### PostgreSQL (`psql`):

```bash
psql -h localhost -U reservas_admin -d reservas_db -f Entregables/Scrips/01_DDL_Creacion_Tablas_Constraints_Indices.sql
psql -h localhost -U reservas_admin -d reservas_db -f Entregables/Scrips/02_Logica_Negocio_Procedimientos_Funciones_Triggers.sql
psql -h localhost -U reservas_admin -d reservas_db -f Entregables/Scrips/03_Seguridad_Roles_y_Permisos.sql
psql -h localhost -U reservas_admin -d reservas_db -f Entregables/Scrips/04_DML_Insercion_Datos_Semilla.sql
psql -h localhost -U reservas_admin -d reservas_db -f Entregables/Scrips/05_SQL_Reportes_y_Transacciones_Consolidado.sql
```

#### MongoDB (`mongosh`):

```bash
mongosh "mongodb://admin:<MONGO_PASSWORD>@localhost:27017/admin" < Entregables/Scrips/Mongo_01_Creacion_Colecciones_Validadores_Indices.js
mongosh "mongodb://admin:<MONGO_PASSWORD>@localhost:27017/admin" < Entregables/Scrips/Mongo_02_Datos_Semilla_y_Operaciones_CRUD.js
mongosh "mongodb://admin:<MONGO_PASSWORD>@localhost:27017/admin" < Entregables/Scrips/Mongo_03_Consultas_Complejas_y_Agregaciones.js
```

*(O en un solo paso usando `reservasbackend/src/main/resources/mongo_schema_angel.js`).*

---

### 4. Iniciar el Backend (Spring Boot)

```bash
cd reservasbackend
# En Linux / macOS:
export $(grep -v '^#' ../.env | xargs)
./mvnw spring-boot:run

# En Windows (PowerShell):
.\mvnw.cmd spring-boot:run
```

El backend quedará disponible en `http://localhost:8080`.

#### Endpoints Principales de la API REST:

- **Espacios y Catálogos:** `GET /api/espacios/listar`, `POST /api/espacios/crear`, `GET /api/espacios/tipos`, `GET /api/espacios/servicios`
- **Reservas (PL/pgSQL):** `GET /api/reservas/listar`, `POST /api/reservas/crear`, `GET /api/reservas/disponibilidad`, `POST /api/reservas/{id}/cancelar`
- **Facturación y Pagos:** `POST /api/pagos/registrar`, `GET /api/pagos/estado/{idReserva}`
- **Clientes y Empleados:** `GET /api/clientes/listar`, `POST /api/clientes/crear`, `GET /api/empleados/listar`, `POST /api/empleados/login`
- **MongoDB — Detalles de Evento:** `GET /api/nosql/detalles-evento`, `GET /api/nosql/detalles-evento/busqueda-avanzada`, `GET /api/nosql/detalles-evento/reserva/{idReserva}`, `POST /api/nosql/detalles-evento`, `DELETE /api/nosql/detalles-evento/reserva/{idReserva}`
- **MongoDB — Reseñas:** `GET /api/nosql/resenas`, `POST /api/nosql/resenas`, `PATCH /api/nosql/resenas/reserva/{idReserva}/respuesta`, `DELETE /api/nosql/resenas/reserva/{idReserva}`
- **MongoDB — Reportes de Agregación:** `GET /api/nosql/reportes/espacios-mas-reservados`, `GET /api/nosql/reportes/servicios-mas-solicitados`, `GET /api/nosql/reportes/calificaciones-espacios`

---

### 5. Iniciar el Frontend (Angular)

En una nueva terminal:

```bash
cd FrontEnd
npm install
npm start
```

Abrir en el navegador: `http://localhost:4200`

---

### 6. Ejecución de Pruebas Automatizadas

Para ejecutar las **39 pruebas unitarias y de integración** del backend:

```bash
cd reservasbackend
./mvnw test
```

