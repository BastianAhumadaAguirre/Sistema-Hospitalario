-- ══════════════════════════════════════════
--  medico_db  -  Datos de ejemplo
--  Contraseña de todos: password123
--  BCrypt: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- ══════════════════════════════════════════

INSERT INTO usuarios (email, password_hash, nombre_completo, rol)
VALUES
  ('dr.torres@medhospital.cl',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Dr. Carlos Torres',   'MEDICO'),
  ('dra.vargas@medhospital.cl',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Dra. Laura Vargas',   'MEDICO'),
  ('funcionario@funchospital.cl',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Ana Martínez',        'FUNCIONARIO'),
  ('admin@admhospital.cl',          '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Roberto Silva',       'ADMIN_HOSPITAL')
ON CONFLICT (email) DO NOTHING;

INSERT INTO medicos (usuario_id, rut, nombre, apellido, especialidad, hospital, email, telefono)
VALUES
  (1, '77889900-1', 'Carlos', 'Torres', 'Cardiología',    'Hospital Norte', 'dr.torres@medhospital.cl',  '922334455'),
  (2, '66778899-2', 'Laura',  'Vargas', 'Traumatología',  'Hospital Norte', 'dra.vargas@medhospital.cl', '933445566')