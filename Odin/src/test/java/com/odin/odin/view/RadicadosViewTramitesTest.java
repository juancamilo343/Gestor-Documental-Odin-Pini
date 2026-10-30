package com.odin.odin.view;

import com.odin.odin.model.Radicados;
import com.odin.odin.model.Tramites;
import com.odin.odin.repository.*;
import com.odin.odin.service.ConsecutivoRadicadoService;
import com.odin.odin.service.DocumentosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RadicadosViewTramitesTest {

    @Mock private RadicadosRepository radicadosRepository;
    @Mock private DependenciasRepository dependenciasRepository;
    @Mock private SeriesRepository seriesRepository;
    @Mock private SubseriesRepository subseriesRepository;
    @Mock private TramitesRepository tramitesRepository;
    @Mock private EstadosRepository estadosRepository;
    @Mock private UsuariosRepository usuariosRepository;
    @Mock private DocumentosRepository documentosRepository;
    @Mock private DocumentosService documentosService;
    @Mock private ConsecutivoRadicadoService consecutivoService;
    @Mock private RemitentesExternosRepository remitentesRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RadicadosView radicadosView;

    @Test
    @DisplayName("cargarCatalogos debe consultar findByActivoTrueOrderByNombreAsc directamente de la base de datos")
    void cargarCatalogos_ConsultaDirectaBD() {
        when(dependenciasRepository.findAll()).thenReturn(Collections.emptyList());
        when(seriesRepository.findAll()).thenReturn(Collections.emptyList());
        when(subseriesRepository.findAll()).thenReturn(Collections.emptyList());
        when(estadosRepository.findAll()).thenReturn(Collections.emptyList());
        when(usuariosRepository.findAll()).thenReturn(Collections.emptyList());

        List<Tramites> catalogoActivo = new ArrayList<>();
        catalogoActivo.add(Tramites.builder().idTramite(5L).nombre("Petición").diasRespuesta(15).activo(true).build());
        catalogoActivo.add(Tramites.builder().idTramite(24L).nombre("Queja").diasRespuesta(15).activo(true).build());
        catalogoActivo.add(Tramites.builder().idTramite(25L).nombre("Reclamo").diasRespuesta(15).activo(true).build());
        catalogoActivo.add(Tramites.builder().idTramite(26L).nombre("Sugerencia").diasRespuesta(15).activo(true).build());

        when(tramitesRepository.findByActivoTrueOrderByNombreAsc()).thenReturn(catalogoActivo);

        Model model = new ConcurrentModel();
        ReflectionTestUtils.invokeMethod(radicadosView, "cargarCatalogos", model);

        @SuppressWarnings("unchecked")
        List<Tramites> tramitesEnModelo = (List<Tramites>) model.getAttribute("tramites");
        assertNotNull(tramitesEnModelo);
        assertEquals(4, tramitesEnModelo.size());

        // Verificar que ningún trámite sea 'PQRS' agrupado
        boolean contienePqrs = tramitesEnModelo.stream().anyMatch(t -> "PQRS".equalsIgnoreCase(t.getNombre().trim()));
        assertFalse(contienePqrs, "El trámite agrupado PQRS no debe estar presente");

        // Verificar los 4 trámites reales
        assertTrue(tramitesEnModelo.stream().anyMatch(t -> t.getIdTramite() == 5L && t.getNombre().equals("Petición")));
        assertTrue(tramitesEnModelo.stream().anyMatch(t -> t.getIdTramite() == 24L && t.getNombre().equals("Queja")));
        assertTrue(tramitesEnModelo.stream().anyMatch(t -> t.getIdTramite() == 25L && t.getNombre().equals("Reclamo")));
        assertTrue(tramitesEnModelo.stream().anyMatch(t -> t.getIdTramite() == 26L && t.getNombre().equals("Sugerencia")));
    }

    @Test
    @DisplayName("prepararRadicado debe preservar el ID real del trámite y calcular fecha límite en días hábiles")
    void prepararRadicado_PreservaIdRealYCalculaDiasHabiles() {
        when(consecutivoService.siguienteNumeroRadicado(any())).thenReturn("RAD-2026-00001");

        Tramites queja = Tramites.builder()
                .idTramite(24L)
                .nombre("Queja")
                .diasRespuesta(15)
                .prioridadDefault("MEDIA")
                .idDependenciaResponsable(2L)
                .activo(true)
                .build();

        when(tramitesRepository.findById(24L)).thenReturn(Optional.of(queja));

        Radicados r = new Radicados();
        r.setId_tramite(24L);
        r.setFecha_radicado("2026-10-30"); // Viernes
        r.setAsunto("Inconformidad con la atención recibida");

        ReflectionTestUtils.invokeMethod(radicadosView, "prepararRadicado", r, "/view/radicados/save");

        // Verificar que el ID real de la BD se mantiene id_tramite = 24 (sin mutar a 1 ni inventar IDs)
        assertEquals(24L, r.getId_tramite());

        // Verificar que el asunto no fue alterado con prefijos artificiales
        assertEquals("Inconformidad con la atención recibida", r.getAsunto());

        // Verificar cálculo en días hábiles: 2026-10-30 + 15 días hábiles = 2026-11-20
        assertEquals("2026-11-20", r.getFecha_vencimiento());
        assertEquals("2026-11-20", r.getFecha_limite());
    }

    @Test
    @DisplayName("prepararRadicado respeta fecha límite ya calculada desde la interfaz si está presente")
    void prepararRadicado_RespetaFechaLimiteFront() {
        when(consecutivoService.siguienteNumeroRadicado(any())).thenReturn("RAD-2026-00002");

        Radicados r = new Radicados();
        r.setId_tramite(25L); // Reclamo
        r.setFecha_radicado("2026-10-30");
        r.setFecha_vencimiento("2026-11-20");

        ReflectionTestUtils.invokeMethod(radicadosView, "prepararRadicado", r, "/view/radicados/save");

        assertEquals(25L, r.getId_tramite());
        assertEquals("2026-11-20", r.getFecha_vencimiento());
        assertEquals("2026-11-20", r.getFecha_limite());
    }
}
