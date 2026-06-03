-- ══════════════════════════════════════════
--  medico_db  -  Schema
--  Ejecutar en PostgreSQL como superusuario
-- ══════════════════════════════════════════

-- PASO 1: Crear la base de datos (solo si no existe)
-- CREATE DATABASE medico_db;
-- \c medico_db

-- PASO 2: Tablas
CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGSERIAL     PRIMARY KEY,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    nombre_completo VARCHAR(200)  NOT NULL,
    rol             VARCHAR(30)   NOT NULL,
    activo          BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS medicos (
    id           BIGSERIAL     PRIMARY KEY,
    usuario_id   BIGINT,
    rut          VARCHAR(12)   NOT NULL UNIQUE,
    nombre       VARCHAR(100)  NOT NULL,
    apellido     VARCHAR(100)  NOT NULL,
    especialidad VARCHAR(100)  NOT NULL,
    hospital     VARCHAR(100)  NOT NULL,
    email        VARCHAR(150),
    telefono     VARCHAR(12)
);