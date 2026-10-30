package com.odin.odin.repository;

import com.odin.odin.model.Radicados;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RadicadosRepository extends JpaRepository<Radicados, Long> {

    @Query("SELECT r FROM Radicados r ORDER BY r.id_radicado DESC")
    List<Radicados> findTop5UltimosRadicados();

    @Query("SELECT COUNT(r) FROM Radicados r WHERE r.id_estado = 1")
    Long countPendientes();

    @Query("SELECT COUNT(r) FROM Radicados r WHERE r.id_estado = 2")
    Long countEnTramite();

    @Query("SELECT COUNT(r) FROM Radicados r WHERE r.id_estado = 3")
    Long countFinalizados();

    @Query("SELECT COUNT(r) FROM Radicados r WHERE r.id_estado = 4")
    Long countRechazados();

    /**
     * Vencidos: fecha_vencimiento &lt; hoy
     *   O sin fecha_vencimiento y (fecha_radicado + dias_respuesta del trámite) &lt; hoy.
     * Excluye finalizados/rechazados y cerrados.
     */
    @Query(value = """
            SELECT COUNT(*) FROM radicados r
            LEFT JOIN tramites t ON t.id_tramite = r.id_tramite
            WHERE (r.id_estado IS NULL OR r.id_estado NOT IN (3, 4))
              AND r.fecha_cierre IS NULL
              AND (
                    (NULLIF(TRIM(r.fecha_vencimiento), '') IS NOT NULL
                     AND NULLIF(TRIM(r.fecha_vencimiento), '')::date < CURRENT_DATE)
                 OR ( (r.fecha_vencimiento IS NULL OR TRIM(COALESCE(r.fecha_vencimiento,'')) = '')
                      AND t.dias_respuesta IS NOT NULL
                      AND (COALESCE(
                             NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')), 1, 10), '')::date,
                             CURRENT_DATE
                           ) + (t.dias_respuesta * INTERVAL '1 day'))::date < CURRENT_DATE )
              )
            """, nativeQuery = true)
    Long countVencidos();

    @Query(value = """
            SELECT r.* FROM radicados r
            LEFT JOIN tramites t ON t.id_tramite = r.id_tramite
            WHERE (r.id_estado IS NULL OR r.id_estado NOT IN (3, 4))
              AND r.fecha_cierre IS NULL
              AND (
                    (NULLIF(TRIM(r.fecha_vencimiento), '') IS NOT NULL
                     AND NULLIF(TRIM(r.fecha_vencimiento), '')::date < CURRENT_DATE)
                 OR ( (r.fecha_vencimiento IS NULL OR TRIM(COALESCE(r.fecha_vencimiento,'')) = '')
                      AND t.dias_respuesta IS NOT NULL
                      AND (COALESCE(
                             NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')), 1, 10), '')::date,
                             CURRENT_DATE
                           ) + (t.dias_respuesta * INTERVAL '1 day'))::date < CURRENT_DATE )
              )
            ORDER BY COALESCE(
                NULLIF(TRIM(r.fecha_vencimiento), '')::date,
                (COALESCE(NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')),1,10),'')::date, CURRENT_DATE)
                 + (COALESCE(t.dias_respuesta,0) * INTERVAL '1 day'))::date
            ) ASC
            """, nativeQuery = true)
    List<Radicados> findVencidos();

    @Query(value = """
            SELECT r.* FROM radicados r
            LEFT JOIN tramites t ON t.id_tramite = r.id_tramite
            WHERE (r.id_estado IS NULL OR r.id_estado NOT IN (3, 4))
              AND r.fecha_cierre IS NULL
              AND (
                    (NULLIF(TRIM(r.fecha_vencimiento), '') IS NOT NULL
                     AND NULLIF(TRIM(r.fecha_vencimiento), '')::date < CURRENT_DATE)
                 OR ( (r.fecha_vencimiento IS NULL OR TRIM(COALESCE(r.fecha_vencimiento,'')) = '')
                      AND t.dias_respuesta IS NOT NULL
                      AND (COALESCE(
                             NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')), 1, 10), '')::date,
                             CURRENT_DATE
                           ) + (t.dias_respuesta * INTERVAL '1 day'))::date < CURRENT_DATE )
              )
            ORDER BY COALESCE(
                NULLIF(TRIM(r.fecha_vencimiento), '')::date,
                (COALESCE(NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')),1,10),'')::date, CURRENT_DATE)
                 + (COALESCE(t.dias_respuesta,0) * INTERVAL '1 day'))::date
            ) ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM radicados r
            LEFT JOIN tramites t ON t.id_tramite = r.id_tramite
            WHERE (r.id_estado IS NULL OR r.id_estado NOT IN (3, 4))
              AND r.fecha_cierre IS NULL
              AND (
                    (NULLIF(TRIM(r.fecha_vencimiento), '') IS NOT NULL
                     AND NULLIF(TRIM(r.fecha_vencimiento), '')::date < CURRENT_DATE)
                 OR ( (r.fecha_vencimiento IS NULL OR TRIM(COALESCE(r.fecha_vencimiento,'')) = '')
                      AND t.dias_respuesta IS NOT NULL
                      AND (COALESCE(
                             NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')), 1, 10), '')::date,
                             CURRENT_DATE
                           ) + (t.dias_respuesta * INTERVAL '1 day'))::date < CURRENT_DATE )
              )
            """,
            nativeQuery = true)
    Page<Radicados> findVencidos(Pageable pageable);

    @Query(value = """
            SELECT r.* FROM radicados r
            LEFT JOIN tramites t ON t.id_tramite = r.id_tramite
            WHERE (r.id_estado IS NULL OR r.id_estado NOT IN (3, 4))
              AND r.fecha_cierre IS NULL
              AND (
                    (NULLIF(TRIM(r.fecha_vencimiento), '') IS NOT NULL
                     AND NULLIF(TRIM(r.fecha_vencimiento), '')::date < CURRENT_DATE)
                 OR ( (r.fecha_vencimiento IS NULL OR TRIM(COALESCE(r.fecha_vencimiento,'')) = '')
                      AND t.dias_respuesta IS NOT NULL
                      AND (COALESCE(
                             NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')), 1, 10), '')::date,
                             CURRENT_DATE
                           ) + (t.dias_respuesta * INTERVAL '1 day'))::date < CURRENT_DATE )
              )
            ORDER BY COALESCE(
                NULLIF(TRIM(r.fecha_vencimiento), '')::date,
                (COALESCE(NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')),1,10),'')::date, CURRENT_DATE)
                 + (COALESCE(t.dias_respuesta,0) * INTERVAL '1 day'))::date
            ) ASC
            LIMIT 5
            """, nativeQuery = true)
    List<Radicados> findVencidosTop5();

    @Query(value = """
            SELECT r.* FROM radicados r
            LEFT JOIN tramites t ON t.id_tramite = r.id_tramite
            WHERE (r.id_estado IS NULL OR r.id_estado NOT IN (3, 4))
              AND r.fecha_cierre IS NULL
              AND (
                    (NULLIF(TRIM(r.fecha_vencimiento), '') IS NOT NULL
                     AND NULLIF(TRIM(r.fecha_vencimiento), '')::date
                         BETWEEN CURRENT_DATE AND (CURRENT_DATE + CAST(:dias AS integer)))
                 OR ( (r.fecha_vencimiento IS NULL OR TRIM(COALESCE(r.fecha_vencimiento,'')) = '')
                      AND t.dias_respuesta IS NOT NULL
                      AND (COALESCE(
                             NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')), 1, 10), '')::date,
                             CURRENT_DATE
                           ) + (t.dias_respuesta * INTERVAL '1 day'))::date
                          BETWEEN CURRENT_DATE AND (CURRENT_DATE + CAST(:dias AS integer)) )
              )
            ORDER BY COALESCE(
                NULLIF(TRIM(r.fecha_vencimiento), '')::date,
                (COALESCE(NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')),1,10),'')::date, CURRENT_DATE)
                 + (COALESCE(t.dias_respuesta,0) * INTERVAL '1 day'))::date
            ) ASC
            """, nativeQuery = true)
    List<Radicados> findProximosAVencer(@Param("dias") Integer dias);

    /** Usado por DashboardService — próximos 7 días (fecha_vencimiento o plazo del trámite) */
    @Query(value = """
            SELECT COUNT(*) FROM radicados r
            LEFT JOIN tramites t ON t.id_tramite = r.id_tramite
            WHERE (r.id_estado IS NULL OR r.id_estado NOT IN (3, 4))
              AND r.fecha_cierre IS NULL
              AND (
                    (NULLIF(TRIM(r.fecha_vencimiento), '') IS NOT NULL
                     AND NULLIF(TRIM(r.fecha_vencimiento), '')::date
                         BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL '7 days'))
                 OR ( (r.fecha_vencimiento IS NULL OR TRIM(COALESCE(r.fecha_vencimiento,'')) = '')
                      AND t.dias_respuesta IS NOT NULL
                      AND (COALESCE(
                             NULLIF(substring(TRIM(COALESCE(r.fecha_radicado,'')), 1, 10), '')::date,
                             CURRENT_DATE
                           ) + (t.dias_respuesta * INTERVAL '1 day'))::date
                          BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL '7 days') )
              )
            """, nativeQuery = true)
    Long countProximosAVencer();

    @Query("SELECT COUNT(r) FROM Radicados r WHERE r.id_estado = :estadoId")
    Long countByEstado(@Param("estadoId") Long estadoId);

    @Query(value = "SELECT COUNT(*) FROM radicados WHERE id_usuario IS NULL OR id_usuario = 0", nativeQuery = true)
    Long countSinAsignar();

    @Query(value = "SELECT COUNT(*) FROM radicados " +
            "WHERE id_estado = 3 AND fecha_cierre IS NOT NULL AND DATE(fecha_cierre) = CURRENT_DATE",
            nativeQuery = true)
    Long countFinalizadosHoy();

    @Query("SELECT r FROM Radicados r WHERE " +
            "(:texto IS NULL OR :texto = '' OR LOWER(r.numero_radicado) LIKE LOWER(CONCAT('%', :texto, '%')) OR LOWER(r.asunto) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
            "AND (:estado IS NULL OR r.id_estado = :estado) " +
            "AND (:dependencia IS NULL OR r.id_dependencia = :dependencia) " +
            "AND (:tramite IS NULL OR r.id_tramite = :tramite) " +
            "ORDER BY r.id_radicado DESC")
    List<Radicados> buscar(@Param("texto") String texto,
                           @Param("estado") Long estado,
                           @Param("dependencia") Long dependencia,
                           @Param("tramite") Long tramite);

    @Query("SELECT r FROM Radicados r WHERE " +
            "(:texto IS NULL OR :texto = '' OR LOWER(r.numero_radicado) LIKE LOWER(CONCAT('%', :texto, '%')) OR LOWER(r.asunto) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
            "AND (:estado IS NULL OR r.id_estado = :estado) " +
            "AND (:dependencia IS NULL OR r.id_dependencia = :dependencia) " +
            "AND (:tramite IS NULL OR r.id_tramite = :tramite) " +
            "ORDER BY r.id_radicado DESC")
    Page<Radicados> buscar(@Param("texto") String texto,
                           @Param("estado") Long estado,
                           @Param("dependencia") Long dependencia,
                           @Param("tramite") Long tramite,
                           Pageable pageable);

    @Query(value = "SELECT * FROM radicados WHERE (:texto IS NULL OR :texto = '' OR busqueda_tsv @@ websearch_to_tsquery('spanish', :texto)) AND (:estado IS NULL OR id_estado = :estado) AND (:dependencia IS NULL OR id_dependencia = :dependencia) AND (:tramite IS NULL OR id_tramite = :tramite) ORDER BY id_radicado DESC", nativeQuery = true)
    List<Radicados> buscarTextoCompleto(@Param("texto") String texto, @Param("estado") Long estado, @Param("dependencia") Long dependencia, @Param("tramite") Long tramite);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE radicados SET id_usuario=:usuario, id_dependencia=:dependencia WHERE id_radicado=:radicado",
            nativeQuery = true)
    int actualizarAsignacion(@Param("radicado") Long radicado,
                             @Param("usuario") Long usuario,
                             @Param("dependencia") Long dependencia);

    @org.springframework.data.jpa.repository.Query(value = "SELECT COALESCE(MAX(id_radicado), 0) + 1 FROM radicados", nativeQuery = true)
    Long nextNumeroRadicado();
}
