package com.odin.odin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_radicado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialRadicado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id_historial;

    @Column(name = "id_radicado", nullable = false)
    private Long id_radicado;

    @Column(name = "id_usuario", nullable = false)
    private Long id_usuario;

    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    @Column(name = "descripcion", nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
