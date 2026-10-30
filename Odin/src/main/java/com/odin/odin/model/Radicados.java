package com.odin.odin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Entity
@Table(name = "radicados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Radicados {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_radicado")
    private Long id_radicado;

    @Column(name = "id_remitente")
    private Long id_remitente;

    @Column(name = "numero_radicado")
    private String numero_radicado;

    /** Long para coincidir con Tramites.idTramite */
    @NotNull(message = "el tramite es obligatorio")
    @Column(name = "id_tramite")
    private Long id_tramite;

    /** Long para coincidir con Estados.id_estado (evita error Integer vs Long en EAGER) */
    @NotNull(message = "el estado es obligatorio")
    @Column(name = "id_estado")
    private Long id_estado;

    @Column(name = "id_dependencia", insertable = false, updatable = false)
    private Long id_dependencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dependencia")
    private Dependencias dependencias;

    /** Long para coincidir con Usuarios.id_usuario */
    @NotNull(message = "el usuario es obligatorio")
    @Column(name = "id_usuario")
    private Long id_usuario;

    @Column(name = "codigo_serie", length = 50)
    private String codigo_serie;

    @Column(name = "codigo_subserie", length = 60)
    private String codigo_subserie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_serie", referencedColumnName = "codigo_serie", insertable = false, updatable = false)
    private Series serie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_subserie", referencedColumnName = "codigo_subserie", insertable = false, updatable = false)
    private Subseries subserie;

    @Column(name = "remitente")
    private String remitente;

    @NotBlank(message = "el asunto es obligatorio")
    @Column(name = "asunto")
    private String asunto;

    @NotBlank(message = "la fecha de radicado es obligatoria")
    @Column(name = "fecha_radicado")
    private String fecha_radicado;

    @Column(name = "tipo_radicado")
    private String tipo_radicado;

    @Column(name = "medio_recepcion")
    private String medio_recepcion;

    @Column(name = "prioridad")
    private String prioridad;

    @Column(name = "confidencialidad")
    private String confidencialidad;

    @Column(name = "id_expediente")
    private Long id_expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_expediente", insertable = false, updatable = false)
    private Expedientes expediente;

    @Column(name = "fecha_vencimiento")
    private String fecha_vencimiento;

    @Column(name = "fecha_limite")
    private String fecha_limite;

    @Column(name = "fecha_cierre")
    private LocalDateTime fecha_cierre;

    @Column(name = "id_usuario_cierre")
    private Long id_usuario_cierre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tramite", insertable = false, updatable = false)
    private Tramites tramite;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuarios usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", insertable = false, updatable = false)
    private Estados estado;

    @Transient private String numeroIdentificacion;
    @Transient private String contacto;
    @Transient private String direccion;
    @Transient private String tipoDocumento;
    @Transient private String fechaDocumento;
    @Transient private String canalRecepcion;
    @Transient private String dependencia;
    @Transient private MultipartFile[] archivos;
    @Transient private String dependenciaOrigen;
    @Transient private String dependenciaDestino;
    @Transient private String responsable;
    @Transient private String observaciones;
    @Transient private String tipoPQRS;
    @Transient private String tipoRadicacion;
    @Transient private String correo;
    @Transient private String telefono;
    @Transient private String ciudad;
    @Transient private String numeroFolios;
    @Transient private String soporte;
    @Transient private String etiquetas;
    @Transient private String fechaLimite;
    @Transient private String descripcion;
    @Transient private String dependenciaResponsable;
    @Transient private String tipoDocumental;
    @Transient private Boolean anonimo;

    /** Días restantes hasta fecha límite/vencimiento (calculado en vista, no persistido). */
    @Transient private Integer diasRestantes;
}
