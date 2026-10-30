package com.odin.odin.repository;

import com.odin.odin.model.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuariosRepository extends JpaRepository<Usuarios, Long> {

    Optional<Usuarios> findByCorreoIgnoreCase(String correo);

    /**
     * Búsqueda nativa: evita problemas de JPQL con campos que tienen guión bajo
     * (num_identificacion). Compara correo, identificación o nombre.
     */
    @Query(value = """
            SELECT * FROM usuarios u
            WHERE LOWER(TRIM(COALESCE(u.correo, ''))) = LOWER(TRIM(:login))
               OR TRIM(COALESCE(u.num_identificacion, '')) = TRIM(:login)
               OR LOWER(TRIM(COALESCE(u.nombre, ''))) = LOWER(TRIM(:login))
            LIMIT 1
            """, nativeQuery = true)
    Optional<Usuarios> findByLoginFlexible(@Param("login") String login);

    @Query(value = """
            SELECT * FROM usuarios u
            WHERE u.id_dependencia = :idDep
              AND (LOWER(TRIM(COALESCE(u.estado, ''))) = 'activo' OR u.estado IS NULL)
            ORDER BY u.id_usuario ASC
            """, nativeQuery = true)
    List<Usuarios> findActivosByDependencia(@Param("idDep") Long idDep);
}
