package com.odin.odin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ConsecutivoRadicadoService {

    private static final Logger log = LoggerFactory.getLogger(ConsecutivoRadicadoService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Genera de forma segura y atómica el siguiente número de radicado.
     * Prefijos soportados:
     * - E : Entrada
     * - S : Salida
     * - INT : Interno
     * Formato resultante: PREFIJO-AÑO-CONSECUTIVO (ej. E-2026-000001)
     */
    @Transactional
    public synchronized String siguienteNumeroRadicado(String tipoRadicado) {
        int anio = LocalDate.now().getYear();
        String prefijo = determinarPrefijo(tipoRadicado);

        try {
            // Intentar usar la tabla radicado_consecutivos de manera concurrente segura (estilo PostgreSQL / Supabase)
            String upsertSql = """
                INSERT INTO public.radicado_consecutivos (anio, prefijo, consecutivo)
                VALUES (?, ?, 1)
                ON CONFLICT (anio, prefijo)
                DO UPDATE SET consecutivo = public.radicado_consecutivos.consecutivo + 1
                RETURNING consecutivo
            """;
            Integer consecutivo = jdbcTemplate.queryForObject(upsertSql, Integer.class, anio, prefijo);
            if (consecutivo == null) {
                consecutivo = 1;
            }
            return String.format("%s-%d-%06d", prefijo, anio, consecutivo);
        } catch (Exception e) {
            log.warn("No se pudo usar la tabla radicado_consecutivos ({}), recurriendo a cálculo por histórico en radicados", e.getMessage());
            return calcularPorHistorico(prefijo, anio);
        }
    }

    private String calcularPorHistorico(String prefijo, int anio) {
        String patron = prefijo + "-" + anio + "-%";
        try {
            String sql = """
                SELECT COALESCE(MAX(
                    NULLIF(regexp_replace(numero_radicado, '^.*-', ''), '')::INTEGER
                ), 0) + 1
                FROM public.radicados
                WHERE numero_radicado LIKE ?
            """;
            Integer siguiente = jdbcTemplate.queryForObject(sql, Integer.class, patron);
            if (siguiente == null || siguiente < 1) {
                siguiente = 1;
            }
            return String.format("%s-%d-%06d", prefijo, anio, siguiente);
        } catch (Exception ex) {
            log.error("Error al calcular consecutivo por histórico: {}", ex.getMessage());
            // Fallback timestamp en caso extremo de fallo en consulta
            return String.format("%s-%d-%06d", prefijo, anio, System.currentTimeMillis() % 1000000);
        }
    }

    public static String determinarPrefijo(String tipoRadicado) {
        if (tipoRadicado == null) return "E";
        String t = tipoRadicado.trim().toUpperCase();
        if (t.contains("SALIDA") || t.equals("S")) {
            return "S";
        } else if (t.contains("INTERN") || t.equals("INT")) {
            return "INT";
        }
        return "E"; // Por defecto Entrada
    }
}
