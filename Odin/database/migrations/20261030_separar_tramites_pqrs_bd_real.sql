-- ============================================================================
-- ODIN - MIGRACIÓN DEFINITIVA: SEPARACIÓN DE TRÁMITES PQRS EN CATÁLOGO REAL BD
-- Fecha de ejecución: 2026-10-30
-- Base de Datos: PostgreSQL (Supabase)
-- Autorizado por: Administrador de Base de Datos
-- ============================================================================
-- OBJETIVOS:
-- 1. Separar los trámites PQRS en 4 registros independientes:
--    - Petición (ID 5): 15 días hábiles, activo = true
--    - Queja (ID 24): 15 días hábiles, activo = true
--    - Reclamo (ID 25): 15 días hábiles, activo = true
--    - Sugerencia (ID 26): 15 días hábiles, activo = true
-- 2. Preservar intacta la integridad referencial de los 30 radicados preexistentes
--    vinculados a id_tramite = 1 (PQRS), marcándolo con activo = false para que
--    no aparezca en el desplegable de nuevas radicaciones pero siga existiendo.
-- ============================================================================

BEGIN;

-- 1. Desactivar trámite agrupado PQRS (ID 1) preservando histórico intacto
UPDATE tramites
SET activo = false,
    dias_respuesta = 15,
    fecha_actualizacion = NOW()
WHERE id_tramite = 1;

-- 2. Reutilizar y actualizar ID 5 para 'Petición' con 15 días de respuesta
UPDATE tramites
SET nombre = 'Petición',
    descripcion = 'Petición general o solicitud ciudadana en ejercicio del derecho fundamental',
    dias_respuesta = 15,
    activo = true,
    fecha_actualizacion = NOW()
WHERE id_tramite = 5;

-- 3. Crear trámite individual 'Queja' (15 días de respuesta)
INSERT INTO tramites (
    nombre,
    descripcion,
    id_dependencia_responsable,
    id_estado_inicial,
    dias_respuesta,
    prioridad_default,
    requiere_respuesta,
    activo,
    fecha_creacion
)
SELECT
    'Queja',
    'Manifestación de protesta, censura o inconformidad por la conducta de un servidor o atención brindada',
    2,
    1,
    15,
    'MEDIA',
    true,
    true,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tramites WHERE lower(trim(nombre)) = 'queja'
);

-- 4. Crear trámite individual 'Reclamo' (15 días de respuesta)
INSERT INTO tramites (
    nombre,
    descripcion,
    id_dependencia_responsable,
    id_estado_inicial,
    dias_respuesta,
    prioridad_default,
    requiere_respuesta,
    activo,
    fecha_creacion
)
SELECT
    'Reclamo',
    'Manifestación de insatisfacción sobre la prestación indebida o deficiente de un servicio o trámite institucional',
    2,
    1,
    15,
    'MEDIA',
    true,
    true,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tramites WHERE lower(trim(nombre)) = 'reclamo'
);

-- 5. Crear trámite individual 'Sugerencia' (15 días de respuesta)
INSERT INTO tramites (
    nombre,
    descripcion,
    id_dependencia_responsable,
    id_estado_inicial,
    dias_respuesta,
    prioridad_default,
    requiere_respuesta,
    activo,
    fecha_creacion
)
SELECT
    'Sugerencia',
    'Proposición, idea o recomendación formulada por un ciudadano para mejorar la prestación de servicios institucionales',
    2,
    1,
    15,
    'MEDIA',
    true,
    true,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tramites WHERE lower(trim(nombre)) = 'sugerencia'
);

COMMIT;
