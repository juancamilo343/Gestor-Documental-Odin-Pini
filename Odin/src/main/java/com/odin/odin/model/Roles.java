package com.odin.odin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
//nombre de la tabla con la que queremos hacer la conexion
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roles

{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_rol;

    @NotBlank(message = "el nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "el estado es obligatorio")
    private String estado;

    @NotBlank(message = "el rol es obligatorio")
    private String rol;


}
