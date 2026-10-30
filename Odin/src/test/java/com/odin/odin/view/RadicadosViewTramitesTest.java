package com.odin.odin.view;

import com.odin.odin.model.Tramites;
import com.odin.odin.repository.TramitesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RadicadosViewTramitesTest {

    @Mock
    private TramitesRepository tramitesRepository;

    @InjectMocks
    private RadicadosView radicadosView;

    @Test
    @DisplayName("Debe separar la opción agrupada PQRS en cuatro trámites individuales con 15 días hábiles")
    void obtenerTramitesSeparados_DesagregaPQRS() {
        // Simular catálogo que solo tiene el trámite agrupado 'PQRS'
        List<Tramites> mockCatalog = new ArrayList<>();
        mockCatalog.add(Tramites.builder()
                .idTramite(1L)
                .nombre("PQRS")
                .descripcion("Peticiones Quejas Reclamos Sugerencias")
                .diasRespuesta(15)
                .prioridadDefault("MEDIA")
                .idDependenciaResponsable(2L)
                .activo(true)
                .build());
        mockCatalog.add(Tramites.builder()
                .idTramite(2L)
                .nombre("OFICIO")
                .descripcion("Comunicación oficial")
                .diasRespuesta(15)
                .prioridadDefault("MEDIA")
                .idDependenciaResponsable(1L)
                .activo(true)
                .build());

        when(tramitesRepository.findByActivoTrueOrderByNombreAsc()).thenReturn(mockCatalog);

        @SuppressWarnings("unchecked")
        List<Tramites> resultado = (List<Tramites>) ReflectionTestUtils.invokeMethod(radicadosView, "obtenerTramitesSeparados");

        assertNotNull(resultado);

        // 1. Verificar que 'PQRS' agrupado no aparezca
        boolean tienePqrsAgrupado = resultado.stream()
                .anyMatch(t -> "PQRS".equalsIgnoreCase(t.getNombre().trim()));
        assertFalse(tienePqrsAgrupado, "No debe incluirse la opción agrupada PQRS");

        // 2. Verificar que aparezcan los cuatro trámites independientes
        Tramites peticion = resultado.stream()
                .filter(t -> t.getNombre().toLowerCase().contains("petición") || t.getNombre().toLowerCase().contains("peticion"))
                .findFirst().orElse(null);
        Tramites queja = resultado.stream()
                .filter(t -> t.getNombre().equalsIgnoreCase("Queja"))
                .findFirst().orElse(null);
        Tramites reclamo = resultado.stream()
                .filter(t -> t.getNombre().equalsIgnoreCase("Reclamo"))
                .findFirst().orElse(null);
        Tramites sugerencia = resultado.stream()
                .filter(t -> t.getNombre().equalsIgnoreCase("Sugerencia"))
                .findFirst().orElse(null);

        assertNotNull(peticion, "Debe existir Petición");
        assertNotNull(queja, "Debe existir Queja");
        assertNotNull(reclamo, "Debe existir Reclamo");
        assertNotNull(sugerencia, "Debe existir Sugerencia");

        // 3. Verificar que cada uno tenga 15 días hábiles
        assertEquals(15, peticion.getDiasRespuesta());
        assertEquals(15, queja.getDiasRespuesta());
        assertEquals(15, reclamo.getDiasRespuesta());
        assertEquals(15, sugerencia.getDiasRespuesta());

        // 4. Verificar que los demás trámites sigan intactos
        boolean tieneOficio = resultado.stream()
                .anyMatch(t -> "OFICIO".equalsIgnoreCase(t.getNombre()));
        assertTrue(tieneOficio, "Los demás trámites deben permanecer intactos");
    }
}
