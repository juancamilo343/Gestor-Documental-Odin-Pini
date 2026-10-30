package com.odin.odin.repository;

import com.odin.odin.model.Estados;
import com.odin.odin.model.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadosRepository extends JpaRepository<Estados, Long>
{

}