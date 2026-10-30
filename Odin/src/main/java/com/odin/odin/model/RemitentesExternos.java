package com.odin.odin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "remitentes_externos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemitentesExternos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_remitente")
    private Long id_remitente;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Column(name = "apellido", nullable = false)
    private String apellido;

    @NotBlank(message = "El tipo de identificación es obligatorio")
    @Column(name = "tipo_identificacion", nullable = false)
    private String tipo_identificacion;

    @NotBlank(message = "El número de identificación es obligatorio")
    @Column(name = "num_identificacion", nullable = false, unique = true)
    private String num_identificacion;

    @Column(name = "correo")
    private String correo;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "fecha_registro")
    private String fecha_registro;
}