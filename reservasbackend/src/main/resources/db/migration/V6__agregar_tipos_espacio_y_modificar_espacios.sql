-- =====================================================================
-- Script Migracion Flyway V6: Creacion de la tabla tipos_espacio y
-- actualizacion de la tabla espacios para que apunte a esta nueva entidad.
-- =====================================================================

CREATE TABLE IF NOT EXISTS tipos_espacio (
    id_tipo_espacio SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT
);

-- Insertamos un tipo de espacio por defecto para los registros que ya puedan existir en 'espacios'
INSERT INTO tipos_espacio (nombre, descripcion) VALUES ('General', 'Tipo de espacio por defecto (migracion)');

-- Añadimos la nueva columna a espacios apuntando al tipo por defecto
ALTER TABLE espacios ADD COLUMN id_tipo_espacio INT DEFAULT 1;

-- Establecemos la relacion de llave foranea
ALTER TABLE espacios
    ADD CONSTRAINT fk_espacios_tipo FOREIGN KEY (id_tipo_espacio) REFERENCES tipos_espacio(id_tipo_espacio);

-- Eliminamos la antigua columna 'tipo' tipo String (varchar)
ALTER TABLE espacios DROP COLUMN IF EXISTS tipo;
