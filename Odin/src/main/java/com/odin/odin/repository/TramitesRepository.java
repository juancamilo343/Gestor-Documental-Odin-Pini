package com.odin.odin.repository;

import com.odin.odin.model.Tramites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TramitesRepository extends JpaRepository<Tramites, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdTramiteNot(String nombre, Long idTramite);

    List<Tramites> findByActivoTrueOrderByNombreAsc();

    Optional<Tramites> findByIdTramiteAndActivoTrue(Long idTramite);
}
