package com.odin.odin.util;

import com.odin.odin.model.Radicados;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlazoUtilTest {

    @Test
    @DisplayName("Debe calcular 15 días hábiles excluyendo sábados y domingos a partir del día siguiente")
    void calcularFechaLimiteHabiles_15Dias() {
        // Ejemplo: Viernes 30 de Octubre de 2026
        LocalDate viernes = LocalDate.of(2026, 10, 30);
        assertEquals(DayOfWeek.FRIDAY, viernes.getDayOfWeek());

        // 15 días hábiles deben dar exactamente el Viernes 20 de Noviembre de 2026
        LocalDate vencimiento = PlazoUtil.calcularFechaLimiteHabiles(viernes, 15);
        assertEquals(LocalDate.of(2026, 11, 20), vencimiento);
        assertEquals(DayOfWeek.FRIDAY, vencimiento.getDayOfWeek());
    }

    @Test
    @DisplayName("Radicación un viernes con 1 día hábil debe vencer el lunes siguiente")
    void calcularFechaLimiteHabiles_ViernesAUno() {
        LocalDate viernes = LocalDate.of(2026, 10, 30);
        LocalDate vencimiento = PlazoUtil.calcularFechaLimiteHabiles(viernes, 1);
        assertEquals(LocalDate.of(2026, 11, 2), vencimiento);
        assertEquals(DayOfWeek.MONDAY, vencimiento.getDayOfWeek());
    }

    @Test
    @DisplayName("Radicación un miércoles con 1 día hábil debe vencer el jueves")
    void calcularFechaLimiteHabiles_MiercolesAJueves() {
        LocalDate miercoles = LocalDate.of(2026, 10, 28);
        LocalDate vencimiento = PlazoUtil.calcularFechaLimiteHabiles(miercoles, 1);
        assertEquals(LocalDate.of(2026, 10, 29), vencimiento);
        assertEquals(DayOfWeek.THURSDAY, vencimiento.getDayOfWeek());
    }

    @Test
    @DisplayName("Debe contar los días hábiles restantes correctamente")
    void contarDiasHabilesRestantes() {
        LocalDate desde = LocalDate.of(2026, 10, 30); // Viernes
        LocalDate hasta = LocalDate.of(2026, 11, 20); // Viernes (15 días hábiles después)

        int habiles = PlazoUtil.contarDiasHabilesRestantes(desde, hasta);
        assertEquals(15, habiles);
    }

    @Test
    @DisplayName("Aplicar reglas a radicado con trámite calcula fecha y días hábiles")
    void aplicar_RadicadoConDiasTramite() {
        Radicados r = new Radicados();
        r.setFecha_radicado("2026-10-30");

        PlazoUtil.aplicar(r, 15);

        assertNotNull(r.getFecha_vencimiento());
        assertEquals("2026-11-20", r.getFecha_vencimiento());
    }
}
