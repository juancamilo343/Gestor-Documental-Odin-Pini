package com.odin.odin.repository;

import com.odin.odin.model.RemitentesExternos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RemitentesExternosRepository extends JpaRepository<RemitentesExternos, Long> {

    @Query(value = """
            SELECT * FROM remitentes_externos r
            WHERE TRIM(COALESCE(r.num_identificacion, '')) = TRIM(:num)
            ORDER BY r.id_remitente DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<RemitentesExternos> findByNumIdentificacion(@Param("num") String num);

    @Query(value = """
            SELECT * FROM remitentes_externos r
            WHERE TRIM(COALESCE(r.num_identificacion, '')) = TRIM(:num)
              AND UPPER(TRIM(COALESCE(r.tipo_identificacion, ''))) = UPPER(TRIM(:tipo))
            ORDER BY r.id_remitente DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<RemitentesExternos> findByNumIdentificacionAndTipo(
            @Param("num") String num,
            @Param("tipo") String tipo
    );
}
