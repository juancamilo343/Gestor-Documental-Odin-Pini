package com.odin.odin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "ccd_series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Series
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_serie;

    @NotBlank(message = "El codigo de la serie es obligatorio")
    @Column(name = "codigo_serie", nullable = false, unique = true, length = 50)
    private String codigo_serie;

    @NotBlank(message = "El nombre de la serie es obligatorio")
    @Column(name = "nombre_serie", nullable = false, length = 300)
    private String nombre_serie;

    @NotBlank(message = "El codigo de unidad es obligatorio")
    @Column(name = "codigo_unidad", nullable = false, length = 30)
    private String codigo_unidad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "codigo_unidad", referencedColumnName = "codigo_unidad", insertable = false, updatable = false)
    private CcdUnidad unidad;

    @Column(name = "codigo_seccion", length = 30)
    private String codigo_seccion;

    @Column(name = "codigo_subseccion", length = 30)
    private String codigo_subseccion;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "informacion_publica", length = 50)
    private String informacion_publica;
}
