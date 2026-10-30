package com.odin.odin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuarios
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id_usuario;

    @NotNull(message = "El rol es obligatorio")
    @Column(name = "id_rol")
    private Long id_rol;  // ✅ Long

    @NotNull(message = "La dependencia es obligatoria")
    @Column(name = "id_dependencia")
    private Long id_dependencia;  // ✅ Long

    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre")
    private String nombre;

    @NotBlank(message = "El tipo de identificacion es obligatorio")
    @Column(name = "tipo_identificacion")
    private String tipo_identificacion;

    @NotBlank(message = "El numero de identificacion es obligatorio")
    @Column(name = "num_identificacion")
    private String num_identificacion;

    @NotBlank(message = "El correo es obligatorio")
    @Column(name = "correo")
    private String correo;

    // ✅ CORREGIDO: password en lugar de clave
    @NotBlank(message = "La contraseña es obligatoria")
    @Column(name = "password")  // ← Cambiado de "clave" a "password"
    private String password;

    @NotBlank(message = "La dirección es obligatoria")
    @Column(name = "direccion")
    private String direccion;

    @NotBlank(message = "El telefono es obligatorio")
    @Column(name = "telefono")
    private String telefono;

    @NotBlank(message = "El estado es obligatorio")
    @Column(name = "estado")
    private String estado;

    @Column(name = "fecha_creacion")
    private String fecha_creacion;
}