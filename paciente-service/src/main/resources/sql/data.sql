-- ══════════════════════════════════════════
--  paciente_db  -  Datos de ejemplo
--  Contraseña de todos: password123
--  BCrypt: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- ══════════════════════════════════════════

INSERT INTO usuarios (email, password_hash, nombre_completo, rol)
VALUES
  ('juan.perez@gmail.com',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Juan Pérez',     'PACIENTE'),
  ('maria.gonzalez@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'María González', 'PACIENTE'),
  ('carlos.rojas@gmail.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Carlos Rojas',   'PACIENTE')
ON CONFLICT (email) DO NOTHING;

INSERT INTO pacientes (rut, nombre, apellido, fecha_nacimiento, email, telefono, direccion)
VALUES
  ('12345678-9', 'Juan',   'Pérez',    '1990-05-15', 'juan.perez@gmail.com',     '912345678', 'Av. Principal 123, Santiago'),
  ('98765432-1', 'María',  'González', '1985-03-22', 'maria.gonzalez@gmail.com', '987654321', 'Calle Secundaria 456, Antofagasta'),
  ('11223344-5', 'Carlos', 'Rojas',    '1978-11-08', 'carlos.rojas@gmail.com',   '956789012', 'Pasaje Los Aromos 789, Valparaíso')