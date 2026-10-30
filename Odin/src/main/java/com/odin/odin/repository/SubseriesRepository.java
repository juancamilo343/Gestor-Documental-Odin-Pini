package com.odin.odin.repository;

import com.odin.odin.model.Subseries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubseriesRepository extends JpaRepository<Subseries, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Subseries s WHERE s.codigo_subserie = :codigoSubserie")
    Optional<Subseries> findByCodigoSubserie(@org.springframework.data.repository.query.Param("codigoSubserie") String codigoSubserie);
}
