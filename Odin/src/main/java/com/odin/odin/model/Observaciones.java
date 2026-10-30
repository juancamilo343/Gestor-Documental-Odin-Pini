package com.odin.odin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "observaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Observaciones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_observacion")
    private Long id_observacion;

    @Column(name = "id_radicado")
    private Long id_radicado;

    @Column(name = "id_usuario")
    private Long id_usuario;

    @Column(name = "comentario", nullable = false, length = 255)
    private String comentario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
