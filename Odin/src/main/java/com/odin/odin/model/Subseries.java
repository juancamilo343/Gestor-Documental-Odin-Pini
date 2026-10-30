package com.odin.odin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "ccd_subseries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subseries
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_subserie;

    @NotNull(message = "La serie es obligatorio")
    @Column(name = "id_serie", nullable = false)
    private Long id_serie;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_serie", referencedColumnName = "id_serie", insertable = false, updatable = false)
    private Series serie;

    @NotBlank(message = "El codigo de la subserie es obligatorio")
    @Column(name = "codigo_subserie", nullable = false, unique = true, length = 60)
    private String codigo_subserie;

    @NotBlank(message = "El nombre de la subserie es obligatorio")
    @Column(name = "nombre_subserie", nullable = false, length = 400)
    private String nombre_subserie;

    @Column(name = "tipo_pqrsf", columnDefinition = "text")
    private String tipo_pqrsf;

    @Builder.Default
    @Column(name = "retencion_anios")
    private Integer retencion_anios = 5;

    @Builder.Default
    @Column(name = "disposicion_final", nullable = false, length = 20)
    private String disposicion_final = "CONSERVACION";

    @Builder.Default
    @Column(name = "nivel_acceso", nullable = false, length = 20)
    private String nivel_acceso = "PUBLICO";

    @Builder.Default
    @Column(name = "valor_documental", nullable = false, length = 20)
    private String valor_documental = "ADMINISTRATIVO";

    @Column(name = "observaciones_retencion", columnDefinition = "text")
    private String observaciones_retencion;
}
