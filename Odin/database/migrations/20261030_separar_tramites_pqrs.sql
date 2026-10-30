-- ============================================================================
-- ODIN - MIGRACIÓN: SEPARACIÓN DE TRÁMITES PQRS Y ASIGNACIÓN DE DÍAS HÁBILES
-- Fecha: 2026-10-30
-- Estado: Pendiente de autorización por el Administrador de Base de Datos
-- ============================================================================
-- Objetivo:
-- 1. Separar el trámite agrupado 'PQRS' en cuatro trámites individuales:
--    - Petición (15 días hábiles)
--    - Queja (15 días hábiles)
--    - Reclamo (15 días hábiles)
--    - Sugerencia (15 días hábiles)
-- 2. Preservar la integridad referencial y el histórico de los 30 radicados
--    preexistentes vinculados a id_tramite = 1.
-- ============================================================================

BEGIN;

-- 1. Actualizar el registro ID 1 (anteriormente 'PQRS') para convertirlo en 'Petición'.
-- Esto garantiza que los radicados históricos sigan referenciando id_tramite = 1
-- sin romper claves foráneas ni alterar identificadores históricos.
UPDATE tramites
SET nombre = 'Petición',
    descripcion = 'Petición general o solicitud ciudadana en ejercicio del derecho fundamental',
    dias_respuesta = 15,
    prioridad_default = 'MEDIA',
    activo = true,
    fecha_actualizacion = NOW()
WHERE id_tramite = 1;

-- 2. Insertar 'Queja' si no existe previamente
INSERT INTO tramites (nombre, descripcion, id_dependencia_responsable, id_estado_inicial, dias_respuesta, prioridad_default, requiere_respuesta, activo, fecha_creacion)
SELECT 'Queja',
       'Manifestación de protesta, censura, descontento o inconformidad por la conducta de un servidor público o la atención brindada',
       2, 1, 15, 'MEDIA', true, true, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tramites WHERE lower(trim(nombre)) = 'queja'
);

-- 3. Insertar 'Reclamo' si no existe previamente
INSERT INTO tramites (nombre, descripcion, id_dependencia_responsable, id_estado_inicial, dias_respuesta, prioridad_default, requiere_respuesta, activo, fecha_creacion)
SELECT 'Reclamo',
       'Manifestación de insatisfacción sobre la prestación indebida o deficiente de un servicio o trámite institucional',
       2, 1, 15, 'MEDIA', true, true, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tramites WHERE lower(trim(nombre)) = 'reclamo'
);

-- 4. Insertar 'Sugerencia' si no existe previamente
INSERT INTO tramites (nombre, descripcion, id_dependencia_responsable, id_estado_inicial, dias_respuesta, prioridad_default, requiere_respuesta, activo, fecha_creacion)
SELECT 'Sugerencia',
       'Proposición, idea o recomendación formulada por un ciudadano para mejorar la prestación de servicios o funciones institucionales',
       2, 1, 15, 'MEDIA', true, true, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tramites WHERE lower(trim(nombre)) = 'sugerencia'
);

COMMIT;
