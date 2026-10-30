package com.odin.odin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estados")
@Getter
@Setter
@NoArgsConstructor

public class Estados
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_estado;

    @NotBlank(message = "El nombre del estado es obligatorio")
    private String nombre;
}
