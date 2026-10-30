package com.odin.odin.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Alineado 1:1 con public.reasignaciones en Supabase:
 *
 *   id_reasignacion      bigserial PRIMARY KEY
 *   id_radicado          bigint NULL
 *   id_usuario_anterior  bigint NULL
 *   id_usuario_nuevo     bigint NULL
 *   id_dependencia_nueva bigint NULL
 *   fecha                timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
 */
@Entity
@Table(name = "reasignaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reasignaciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reasignacion")
    private Long id_reasignacion;

    @Column(name = "id_radicado")
    private Long id_radicado;

    @Column(name = "id_usuario_anterior")
    private Long id_usuario_anterior;

    @Column(name = "id_usuario_nuevo")
    private Long id_usuario_nuevo;

    @Column(name = "id_dependencia_nueva")
    private Long id_dependencia_nueva;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "fecha", nullable = false, columnDefinition = "timestamp without time zone")
    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}