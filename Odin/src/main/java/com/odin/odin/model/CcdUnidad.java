package com.odin.odin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ccd_unidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CcdUnidad
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_unidad;

    @Column(name = "codigo_unidad", nullable = false, unique = true, length = 30)
    private String codigo_unidad;

    @Column(name = "nombre_unidad", nullable = false, length = 255)
    private String nombre_unidad;

    @Column(name = "codigo_padre", length = 30)
    private String codigo_padre;

    @Column(name = "nivel", nullable = false)
    private Integer nivel;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;
}
