package com.odin.odin.repository;

import com.odin.odin.model.Series;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Series s WHERE s.codigo_serie = :codigoSerie")
    Optional<Series> findByCodigoSerie(@org.springframework.data.repository.query.Param("codigoSerie") String codigoSerie);
}
