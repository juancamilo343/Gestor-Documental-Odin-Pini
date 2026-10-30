package com.odin.odin.view;

import com.odin.odin.model.Dependencias;
import com.odin.odin.model.Documentos;
import com.odin.odin.model.Radicados;

import com.odin.odin.repository.DependenciasRepository;
import com.odin.odin.repository.DocumentosRepository;
import com.odin.odin.repository.EstadosRepository;
import com.odin.odin.repository.RadicadosRepository;
import com.odin.odin.repository.SeriesRepository;
import com.odin.odin.repository.SubseriesRepository;
import com.odin.odin.repository.TramitesRepository;
import com.odin.odin.repository.UsuariosRepository;
import com.odin.odin.service.DocumentosService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Controller
public class RadicadosView {

    @Autowired
    private RadicadosRepository radicadosRepository;

    @Autowired
    private DependenciasRepository dependenciasRepository;

    @Autowired
    private SeriesRepository seriesRepository;

    @Autowired
    private SubseriesRepository subseriesRepository;

    @Autowired
    private TramitesRepository tramitesRepository;

    @Autowired
    private EstadosRepository estadosRepository;

    @Autowired
    private UsuariosRepository usuariosRepository;

    @Autowired
    private DocumentosRepository documentosRepository;

    @Autowired
    private DocumentosService documentosService;

    @Autowired
    private com.odin.odin.service.ConsecutivoRadicadoService consecutivoService;

    @Autowired
    private com.odin.odin.repository.RemitentesExternosRepository remitentesRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/view/radicados")
    public String inicio(Model model) {

        model.addAttribute(
                "radicado",
                new Radicados()
        );

        cargarCatalogos(model);

        return "radicados/radicacion_Documental";
    }

    @GetMapping("/view/radicados/documental")
    public String documental(Model model) {

        model.addAttribute(
                "radicado",
                new Radicados()
        );

        cargarCatalogos(model);

        return "radicados/radicacion_Documental";
    }

    private void cargarCatalogos(Model model) {

        model.addAttribute(
                "dependencias",
                dependenciasRepository.findAll()
        );

        model.addAttribute(
                "series",
                seriesRepository.findAll()
        );

        model.addAttribute(
                "subseries",
                subseriesRepository.findAll()
        );

        model.addAttribute(
                "tramites",
                tramitesRepository.findAll()
        );

        model.addAttribute(
                "estados",
                estadosRepository.findAll()
        );

        model.addAttribute(
                "usuarios",
                usuariosRepository.findAll()
        );
    }

    @PostMapping({
            "/view/radicados/save",
            "/view/radicados/documental/save"
    })
    public String save(
            @ModelAttribute Radicados radicado,
            RedirectAttributes ra,
            HttpServletRequest request) {

        boolean isUpdate =
                radicado.getId_radicado() != null
                        && radicado.getId_radicado() > 0;

        prepararRadicado(
                radicado,
                request.getRequestURI()
        );

        Radicados savedRadicado =
                radicadosRepository.save(
                        radicado
                );

        List<String> nombresArchivosGuardados = new java.util.ArrayList<>();
        int archivosGuardados =
                guardarArchivos(
                        savedRadicado,
                        radicado.getArchivos(),
                        nombresArchivosGuardados
                );

        String mensaje =
                construirMensajeGuardado(
                        isUpdate,
                        request.getRequestURI(),
                        savedRadicado
                );

        ra.addFlashAttribute(
                "success",
                mensaje
        );

        ra.addFlashAttribute(
                "mensaje",
                mensaje
        );

        ra.addFlashAttribute(
                "savedRadicado",
                savedRadicado
        );

        ra.addFlashAttribute(
                "archivos",
                archivosGuardados
        );

        ra.addFlashAttribute(
                "nombresArchivos",
                nombresArchivosGuardados
        );

        ra.addFlashAttribute(
                "radicacionExitosa",
                true
        );

        return "redirect:"
                + obtenerDestinoFormulario(
                request.getRequestURI()
        );
    }

    @GetMapping("/view/radicados/edit/{id}")
    public String edit(
            @PathVariable Long id,
            Model model) {

        Radicados radicado =
                radicadosRepository
                        .findById(id)
                        .orElse(
                                new Radicados()
                        );

        model.addAttribute(
                "radicado",
                radicado
        );

        cargarCatalogos(model);

        return "radicados/radicacion_Documental";
    }

    @PostMapping("/view/radicados/delete/{id}")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes ra) {

        radicadosRepository.deleteById(id);

        ra.addFlashAttribute(
                "success",
                "Radicacion eliminada con exito"
        );

        ra.addFlashAttribute(
                "mensaje",
                "Radicacion eliminada con exito"
        );

        return "redirect:/view/radicados/documental";
    }

    private String obtenerDestinoFormulario(
            String uri) {

        return "/view/radicados/documental";
    }

    private String construirMensajeGuardado(
            boolean isUpdate,
            String uri,
            Radicados radicado) {

        String accion =
                isUpdate
                        ? "actualizada"
                        : "guardada";

        return "Radicacion documental "
                + accion
                + " con exito";
    }

    private void prepararRadicado(
            Radicados radicado,
            String uri) {

        /*
         * Generamos número atómicamente con ConsecutivoRadicadoService (E-YYYY-XXXXXX).
         */
        if (!tieneTexto(radicado.getNumero_radicado())) {
            String tipo = tieneTexto(radicado.getTipoRadicacion())
                    ? radicado.getTipoRadicacion()
                    : radicado.getTipo_radicado();
            if (!tieneTexto(tipo)) {
                tipo = "ENTRADA";
            }
            String numero = consecutivoService.siguienteNumeroRadicado(tipo);
            radicado.setNumero_radicado(numero);
        }

        // Persistimos los campos que antes solo llegaban como @Transient.
        if (tieneTexto(radicado.getTipoRadicacion())) {
            radicado.setTipo_radicado(radicado.getTipoRadicacion());
        }
        if (tieneTexto(radicado.getCanalRecepcion())) {
            radicado.setMedio_recepcion(radicado.getCanalRecepcion());
        }

        /*
         * Normalización estricta de prioridad según la restricción 'radicados_prioridad_check':
         * Valores válidos en PostgreSQL: 'BAJA', 'MEDIA', 'ALTA', 'URGENTE'.
         * Si llega nula, vacía o 'NORMAL', se normaliza a 'MEDIA'.
         */
        String prio = radicado.getPrioridad();
        if (!tieneTexto(prio) || "NORMAL".equalsIgnoreCase(prio)) {
            prio = "MEDIA";
        } else {
            prio = prio.trim().toUpperCase();
            if (!prio.matches("BAJA|MEDIA|ALTA|URGENTE")) {
                prio = "MEDIA";
            }
        }
        radicado.setPrioridad(prio);

        /*
         * Fecha de radicación oficial inmutable (se genera automáticamente con la fecha y hora actual).
         */
        radicado.setFecha_radicado(LocalDateTime.now().toLocalDate().toString());

        /*
         * Evita mandar "" hacia las FK
         * de serie y subserie.
         */
        if (!tieneTexto(
                radicado.getCodigo_serie()
        )) {

            radicado.setCodigo_serie(null);
        }

        if (!tieneTexto(
                radicado.getCodigo_subserie()
        )) {

            radicado.setCodigo_subserie(null);
        }

        Integer tipoPQRSId =
                convertirAEnteroSeguro(
                        radicado.getTipoPQRS()
                );

        Integer tipoDocId =
                convertirAEnteroSeguro(
                        radicado.getTipoDocumento()
                );

        Integer prioridadId =
                convertirAEnteroSeguro(
                        radicado.getPrioridad()
                );

        Integer canalRecepcionId =
                convertirAEnteroSeguro(
                        radicado.getCanalRecepcion()
                );

        Integer dependenciaId =
                radicado.getId_dependencia() != null
                        ? radicado
                        .getId_dependencia()
                        .intValue()
                        : convertirAEnteroSeguro(
                        radicado.getDependencia()
                );

        Integer depeDestinoId =
                convertirAEnteroSeguro(
                        radicado.getDependenciaDestino()
                );

        Integer depeOrigenId =
                convertirAEnteroSeguro(
                        radicado.getDependenciaOrigen()
                );

        Integer responsableId =
                convertirAEnteroSeguro(
                        radicado.getResponsable()
                );

        /*
         * TRÁMITE
         */
        if (!tieneNumero(
                radicado.getId_tramite()
        )) {

            radicado.setId_tramite(
                    primerId(
                            tipoPQRSId,
                            tipoDocId,
                            prioridadId,
                            1
                    )
            );
        }

        /*
         * ESTADO
         */
        if (!tieneNumero(
                radicado.getId_estado()
        )) {

            radicado.setId_estado(
                    idValorODefecto(
                            canalRecepcionId,
                            1
                    )
            );
        }

        /*
         * DEPENDENCIA
         *
         * Esta es una de las correcciones
         * más importantes.
         */
        Long idDepElegido =
                primerId(
                        dependenciaId,
                        depeDestinoId,
                        depeOrigenId,
                        null
                );

        Dependencias depBaseDatos =
                idDepElegido == null
                        ? null
                        : dependenciasRepository
                        .findById(
                                idDepElegido
                        )
                        .orElse(null);

        /*
         * Persistimos la relación real.
         */
        radicado.setDependencias(
                depBaseDatos
        );

        /*
         * USUARIO RESPONSABLE
         */
        if (!tieneNumero(
                radicado.getId_usuario()
        )) {

            radicado.setId_usuario(
                    idValorODefecto(
                            responsableId,
                            2
                    )
            );
        }

        /*
         * REMITENTE Y RADICACIÓN ANÓNIMA (RF-09)
         */
        boolean esAnonimo = Boolean.TRUE.equals(radicado.getAnonimo())
                || "ANONIMO".equalsIgnoreCase(radicado.getRemitente())
                || "ANÓNIMO".equalsIgnoreCase(radicado.getRemitente());

        if (esAnonimo) {
            radicado.setRemitente("ANÓNIMO (Ley de Transparencia)");
            radicado.setId_remitente(null);
        } else {
            if (!tieneTexto(radicado.getRemitente())) {
                radicado.setRemitente(valorODefecto(radicado.getResponsable(), "Anónimo"));
            }

            // Gestionar o sincronizar con tabla remitentes_externos si trae documento
            if (tieneTexto(radicado.getNumeroIdentificacion())) {
                try {
                    String numDoc = radicado.getNumeroIdentificacion().trim();
                    var optRem = remitentesRepository.findByNumIdentificacion(numDoc);
                    if (optRem.isPresent()) {
                        radicado.setId_remitente(optRem.get().getId_remitente());
                    } else {
                        com.odin.odin.model.RemitentesExternos nuevoRem = com.odin.odin.model.RemitentesExternos.builder()
                                .num_identificacion(numDoc)
                                .tipo_identificacion(tieneTexto(radicado.getTipoDocumento()) ? radicado.getTipoDocumento() : "CC")
                                .nombre(radicado.getRemitente())
                                .apellido("")
                                .correo(radicado.getCorreo())
                                .telefono(radicado.getTelefono())
                                .direccion(radicado.getCiudad())
                                .fecha_registro(LocalDateTime.now().toString())
                                .build();
                        var guardado = remitentesRepository.save(nuevoRem);
                        radicado.setId_remitente(guardado.getId_remitente());
                    }
                } catch (Exception e) {
                    // Si ocurre un error al persistir remitente externo, no bloquear la radicación
                }
            }
        }

        /*
         * ASUNTO
         */
        if (!tieneTexto(
                radicado.getAsunto()
        )) {

            radicado.setAsunto(
                    valorODefecto(
                            radicado.getObservaciones(),
                            "Sin asunto"
                    )
            );
        }
    }

    private int guardarArchivos(
            Radicados radicado,
            MultipartFile[] archivos,
            List<String> nombresGuardados) {

        if (archivos == null
                || archivos.length == 0) {

            return 0;
        }

        int guardados = 0;
        int maxArchivos = 5;

        try {

            Path dir =
                    Paths.get(uploadDir)
                            .toAbsolutePath();

            Files.createDirectories(dir);

            String fechaSubida =
                    LocalDateTime.now()
                            .toString();

            for (MultipartFile archivo
                    : archivos) {

                if (guardados >= maxArchivos) {
                    break;
                }

                if (archivo == null
                        || archivo.isEmpty()) {

                    continue;
                }

                String nombreOriginal =
                        StringUtils.cleanPath(
                                archivo
                                        .getOriginalFilename()
                                        == null
                                        ? "archivo"
                                        : archivo
                                        .getOriginalFilename()
                        );

                String nombreAlmacenado =
                        radicado.getId_radicado()
                                + "_"
                                + UUID.randomUUID()
                                + "_"
                                + nombreOriginal;

                Path destino =
                        dir.resolve(
                                nombreAlmacenado
                        );

                archivo.transferTo(destino);

                Documentos documento =
                        new Documentos();

                documento.setId_radicado(
                        (long)
                                radicado
                                        .getId_radicado()
                );

                documento.setTamano(
                        (int)
                                archivo.getSize()
                );

                documento.setNombre(
                        nombreOriginal
                );

                documento.setNombre_archivo(
                        nombreAlmacenado
                );

                documento.setRuta_archivo(
                        destino.toString()
                );

                documento.setTipo(
                        obtenerTipoArchivo(
                                archivo,
                                nombreOriginal
                        )
                );

                documento.setFecha_subida(
                        fechaSubida
                );

                documento.setMime_type(archivo.getContentType());
                documento.setVersion_actual(1);
                documentosService.guardar(documento, radicado.getId_usuario(), "Versión inicial");

                if (nombresGuardados != null) {
                    nombresGuardados.add(nombreOriginal);
                }

                guardados++;
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "No se pudieron guardar los archivos adjuntos: "
                            + e.getMessage(),
                    e
            );
        }

        return guardados;
    }

    private String obtenerTipoArchivo(
            MultipartFile archivo,
            String nombre) {

        if (tieneTexto(
                archivo.getContentType()
        )) {

            return archivo.getContentType();
        }

        int punto =
                nombre.lastIndexOf('.');

        return punto >= 0
                ? nombre.substring(
                punto + 1
        )
                : "desconocido";
    }

    private String valorODefecto(
            String valor,
            String valorPorDefecto) {

        return tieneTexto(valor)
                ? valor
                : valorPorDefecto;
    }

    private boolean tieneTexto(
            String valor) {

        return valor != null
                && !valor.trim().isEmpty();
    }

    private boolean tieneNumero(Number valor) {
        return valor != null && valor.longValue() > 0;
    }

    private Long convertirALongSeguro(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Compatibilidad con código existente que parsea enteros */
    private Integer convertirAEnteroSeguro(String texto) {
        Long v = convertirALongSeguro(texto);
        return v == null ? null : v.intValue();
    }

    private Long primerId(Number id1, Number id2, Number id3, Number idPorDefecto) {
        if (id1 != null && id1.longValue() > 0) return id1.longValue();
        if (id2 != null && id2.longValue() > 0) return id2.longValue();
        if (id3 != null && id3.longValue() > 0) return id3.longValue();
        return idPorDefecto == null ? null : idPorDefecto.longValue();
    }

    private Long idValorODefecto(Number valor, Number idPorDefecto) {
        if (valor != null && valor.longValue() > 0) return valor.longValue();
        return idPorDefecto == null ? null : idPorDefecto.longValue();
    }
}
