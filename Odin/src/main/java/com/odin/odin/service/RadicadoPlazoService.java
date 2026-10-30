package com.odin.odin.service;

import com.odin.odin.model.Notificaciones;
import com.odin.odin.model.Radicados;
import com.odin.odin.model.Tramites;
import com.odin.odin.repository.NotificacionesRepository;
import com.odin.odin.repository.TramitesRepository;
import com.odin.odin.util.PlazoUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class RadicadoPlazoService {

    private static final DateTimeFormatter F = DateTimeFormatter.ISO_LOCAL_DATE;
    private final TramitesRepository tramites;
    private final NotificacionesRepository notificaciones;

    public RadicadoPlazoService(TramitesRepository tramites, NotificacionesRepository notificaciones) {
        this.tramites = tramites;
        this.notificaciones = notificaciones;
    }

    public void aplicarReglas(Radicados r) {
        if (r.getId_tramite() == null) return;
        Tramites t = tramites.findById(r.getId_tramite()).orElse(null);
        if (t == null) return;

        if ((r.getPrioridad() == null || r.getPrioridad().isBlank()) && t.getPrioridadDefault() != null) {
            r.setPrioridad(t.getPrioridadDefault());
        }

        if ((r.getFecha_vencimiento() == null || r.getFecha_vencimiento().isBlank()) && t.getDiasRespuesta() != null && t.getDiasRespuesta() >= 0) {
            LocalDate base = LocalDate.now();
            try {
                if (r.getFecha_radicado() != null && !r.getFecha_radicado().isBlank()) {
                    base = LocalDate.parse(r.getFecha_radicado().substring(0, 10));
                }
            } catch (Exception ignored) {}

            r.setFecha_vencimiento(base.plusDays(t.getDiasRespuesta()).format(F));
        }

        if (r.getId_usuario() != null && r.getFecha_vencimiento() != null && !r.getFecha_vencimiento().isBlank()) {
            crearAlerta(r);
        }
    }

    private void crearAlerta(Radicados r) {
        try {
            LocalDate venc = LocalDate.parse(r.getFecha_vencimiento().substring(0, 10));
            long dias = LocalDate.now().until(venc).getDays();
            if (dias <= 3 && dias >= 0) {
                Notificaciones n = new Notificaciones();
                n.setId_usuario(r.getId_usuario());
                n.setId_radicado(r.getId_radicado());
                n.setTitulo(dias == 0 ? "Radicado vence hoy" : "Radicado próximo a vencer");
                n.setMensaje("El radicado " + r.getNumero_radicado() + " tiene vencimiento " + venc + " (" + dias + " día(s)).");
                n.setLeida(false);
                n.setFecha(LocalDateTime.now());
                notificaciones.save(n);
            }
        } catch (Exception ignored) {}
    }
}
