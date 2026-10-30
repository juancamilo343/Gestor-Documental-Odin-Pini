package com.odin.odin.util;

import com.odin.odin.model.Radicados;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Utilidad transversal para el cálculo de vencimientos, fechas límite y días restantes.
 * Proporciona métodos específicos para conteo en días calendario (por defecto institucional para consultas
 * y gestión de trámites) y métodos dedicados para cálculo en días hábiles (utilizados en radicación documental).
 */
public final class PlazoUtil {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter[] FORMATOS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    /** IDs típicos: finalizado/cerrado (3), rechazado (4), anulado (5) */
    private static final Set<Long> ESTADOS_TERMINALES = Set.of(3L, 4L, 5L);

    private PlazoUtil() {}

    /**
     * Calcula la fecha límite sumando días hábiles a partir de la fecha base.
     * Excluye sábados y domingos.
     * No cuenta el día de radicación como primer día de respuesta (comienza el conteo al día siguiente).
     */
    public static LocalDate calcularFechaLimiteHabiles(LocalDate fechaBase, int diasHabiles) {
        if (fechaBase == null) fechaBase = LocalDate.now();
        if (diasHabiles <= 0) return fechaBase;

        LocalDate cur = fechaBase;
        int count = 0;
        while (count < diasHabiles) {
            cur = cur.plusDays(1);
            DayOfWeek dow = cur.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return cur;
    }

    /**
     * Cuenta los días hábiles restantes entre la fecha actual y la fecha de vencimiento.
     * Si la fecha ya venció, retorna un valor negativo con los días hábiles de mora.
     */
    public static int contarDiasHabilesRestantes(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) return 0;
        if (desde.isEqual(hasta)) return 0;

        boolean esFuturo = hasta.isAfter(desde);
        LocalDate inicio = esFuturo ? desde : hasta;
        LocalDate fin = esFuturo ? hasta : desde;

        int habiles = 0;
        LocalDate cur = inicio.plusDays(1);
        while (!cur.isAfter(fin)) {
            DayOfWeek dow = cur.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                habiles++;
            }
            cur = cur.plusDays(1);
        }
        return esFuturo ? habiles : -habiles;
    }

    public static void aplicar(Radicados r) {
        aplicar(r, null);
    }

    public static void aplicar(Radicados r, Integer diasRespuestaTramite) {
        if (r == null) return;

        if (r.getFecha_cierre() != null) {
            r.setDiasRestantes(null);
            return;
        }
        if (r.getId_estado() != null && ESTADOS_TERMINALES.contains(r.getId_estado())) {
            r.setDiasRestantes(null);
            return;
        }
        // Por nombre de estado (por si los IDs del catálogo difieren)
        try {
            if (r.getEstado() != null && r.getEstado().getNombre() != null) {
                String n = r.getEstado().getNombre().toLowerCase();
                if (n.contains("finaliz") || n.contains("cerrad")
                        || n.contains("rechaz") || n.contains("anul")) {
                    r.setDiasRestantes(null);
                    return;
                }
            }
        } catch (Exception ignored) { }

        LocalDate venc = resolverVencimiento(r, diasRespuestaTramite);
        if (venc == null) {
            r.setDiasRestantes(null);
            return;
        }

        if (r.getFecha_vencimiento() == null || r.getFecha_vencimiento().isBlank()) {
            r.setFecha_vencimiento(venc.format(ISO));
        }

        // Días calendario por defecto para compatibilidad institucional (Gestión de Trámites, Consulta, etc.)
        r.setDiasRestantes((int) ChronoUnit.DAYS.between(LocalDate.now(), venc));
    }

    public static void aplicar(Collection<Radicados> lista) {
        aplicar(lista, null);
    }

    public static void aplicar(Collection<Radicados> lista, Map<Long, Integer> diasPorTramite) {
        if (lista == null) return;
        for (Radicados r : lista) {
            Integer d = null;
            if (diasPorTramite != null && r.getId_tramite() != null) {
                d = diasPorTramite.get(r.getId_tramite());
            }
            aplicar(r, d);
        }
    }

    private static LocalDate resolverVencimiento(Radicados r, Integer diasRespuestaTramite) {
        LocalDate venc = parseFecha(r.getFecha_vencimiento());
        if (venc != null) return venc;

        venc = parseFecha(r.getFecha_limite());
        if (venc != null) return venc;

        if (diasRespuestaTramite == null || diasRespuestaTramite < 0) return null;

        LocalDate base = parseFecha(r.getFecha_radicado());
        if (base == null) base = LocalDate.now();

        return base.plusDays(diasRespuestaTramite);
    }

    private static LocalDate parseFecha(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String t = texto.trim();
        if (t.length() >= 10) t = t.substring(0, 10);
        for (DateTimeFormatter fmt : FORMATOS) {
            try {
                return LocalDate.parse(t, fmt);
            } catch (DateTimeParseException ignored) { }
        }
        return null;
    }
}
