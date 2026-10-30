package com.odin.odin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tramites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tramites {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tramite")
    private Long idTramite;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Column(name = "descripcion", nullable = false, columnDefinition = "text")
    private String descripcion;

    // ✅ CORREGIDO: @Column explícito
    @Column(name = "id_dependencia_responsable")
    private Long idDependenciaResponsable;

    // ✅ CORREGIDO: Solo el ID, no la relación completa
    @Column(name = "id_estado_inicial")
    private Long idEstadoInicial;

    @Column(name = "dias_respuesta")
    private Integer diasRespuesta;

    @Column(name = "prioridad_default", length = 20)
    private String prioridadDefault;

    @Column(name = "requiere_respuesta")
    private Boolean requiereRespuesta;

    @Builder.Default
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    // ✅ Relación con Dependencias (solo lectura)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dependencia_responsable", insertable = false, updatable = false)
    private Dependencias dependenciaResponsable;

    // ✅ Relación con Estados (solo lectura)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_inicial", insertable = false, updatable = false)
    private Estados estadoInicial;

    // Métodos helper para mantener compatibilidad con código existente
    public Long getIdEstadoInicial() {
        return idEstadoInicial;
    }

    public void setIdEstadoInicial(Long idEstadoInicial) {
        this.idEstadoInicial = idEstadoInicial;
    }

    public Long getIdDependenciaResponsable() {
        return idDependenciaResponsable;
    }

    public void setIdDependenciaResponsable(Long idDependenciaResponsable) {
        this.idDependenciaResponsable = idDependenciaResponsable;
    }
}