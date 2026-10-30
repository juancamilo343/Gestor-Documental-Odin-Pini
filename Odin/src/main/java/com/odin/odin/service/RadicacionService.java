package com.odin.odin.service;

import com.odin.odin.model.Dependencias;
import com.odin.odin.model.Documentos;
import com.odin.odin.model.Radicados;
import com.odin.odin.model.Subseries;
import com.odin.odin.model.Tramites;
import com.odin.odin.repository.DependenciasRepository;
import com.odin.odin.repository.DocumentosRepository;
import com.odin.odin.repository.RadicadosRepository;
import com.odin.odin.repository.SubseriesRepository;
import com.odin.odin.repository.SeriesRepository;
import com.odin.odin.repository.TramitesRepository;
import com.odin.odin.repository.UsuariosRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RadicacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final List<String> MIME_PERMITIDOS = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png"
    );

    private final RadicadosRepository radicadosRepository;
    private final TramitesRepository tramitesRepository;
    private final DependenciasRepository dependenciasRepository;
    private final UsuariosRepository usuariosRepository;
    private final SubseriesRepository subseriesRepository;
    private final SeriesRepository seriesRepository;
    private final DocumentosRepository documentosRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public RadicacionService(
            RadicadosRepository radicadosRepository,
            TramitesRepository tramitesRepository,
            DependenciasRepository dependenciasRepository,
            UsuariosRepository usuariosRepository,
            SubseriesRepository subseriesRepository,
            SeriesRepository seriesRepository,
            DocumentosRepository documentosRepository) {
        this.radicadosRepository = radicadosRepository;
        this.tramitesRepository = tramitesRepository;
        this.dependenciasRepository = dependenciasRepository;
        this.usuariosRepository = usuariosRepository;
        this.subseriesRepository = subseriesRepository;
        this.seriesRepository = seriesRepository;
        this.documentosRepository = documentosRepository;
    }

    @Transactional
    public ResultadoRadicacion guardar(Radicados radicado, MultipartFile[] archivos) {
        validarArchivos(archivos);

        boolean actualizacion = radicado.getId_radicado() != null
                && radicado.getId_radicado() > 0;

        Radicados existente = actualizacion
                ? radicadosRepository.findById(radicado.getId_radicado())
                    .orElseThrow(() -> new IllegalArgumentException("El radicado no existe"))
                : null;

        Tramites tramite = obtenerTramiteActivo(radicado.getId_tramite());
        validarDatos(radicado, tramite);

        if (actualizacion) {
            preservarDatosInmutables(radicado, existente);
        } else {
            radicado.setNumero_radicado(generarNumeroRadicado());
            radicado.setFecha_radicado(LocalDateTime.now().format(FECHA_HORA));
        }

        aplicarReglasDelTramite(radicado, tramite);
        aplicarClasificacion(radicado);

        Radicados guardado = radicadosRepository.save(radicado);

        List<String> archivosGuardados = new ArrayList<>();
        try {
            for (MultipartFile archivo : archivos == null ? new MultipartFile[0] : archivos) {
                if (archivo == null || archivo.isEmpty()) {
                    continue;
                }
                archivosGuardados.add(guardarArchivo(guardado, archivo));
            }
        } catch (RuntimeException ex) {
            eliminarArchivosFisicos(archivosGuardados);
            throw ex;
        }

        return new ResultadoRadicacion(guardado, archivosGuardados.size());
    }

    private Tramites obtenerTramiteActivo(Long idTramite) {
        if (idTramite == null || idTramite <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un trámite");
        }
        Tramites tramite = tramitesRepository.findById(idTramite)
                .orElseThrow(() -> new IllegalArgumentException("El trámite seleccionado no existe"));
        if (!Boolean.TRUE.equals(tramite.getActivo())) {
            throw new IllegalArgumentException("El trámite seleccionado está inactivo");
        }
        return tramite;
    }

    private void validarDatos(Radicados r, Tramites tramite) {
        if (!texto(r.getRemitente())) {
            throw new IllegalArgumentException("El remitente es obligatorio");
        }
        if (!texto(r.getAsunto())) {
            throw new IllegalArgumentException("El asunto es obligatorio");
        }
        if (r.getId_usuario() == null || r.getId_usuario() <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un responsable");
        }
        if (!usuariosRepository.existsById(r.getId_usuario())) {
            throw new IllegalArgumentException("El responsable seleccionado no existe");
        }
        if (!texto(r.getTipoRadicacion())) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de radicación");
        }
        if (texto(r.getTipoRadicacion())
                && !r.getTipoRadicacion().matches("(?i)ENTRADA|SALIDA|INTERNA")) {
            throw new IllegalArgumentException("El tipo de radicación no es válido");
        }
        if (texto(r.getPrioridad())
                && !r.getPrioridad().matches("(?i)BAJA|MEDIA|ALTA|URGENTE")) {
            throw new IllegalArgumentException("La prioridad no es válida");
        }

        if (tramite.getIdDependenciaResponsable() == null) {
            throw new IllegalArgumentException("El trámite no tiene dependencia responsable configurada");
        }
        if (!dependenciasRepository.existsById(tramite.getIdDependenciaResponsable())) {
            throw new IllegalArgumentException("La dependencia responsable del trámite no existe");
        }

        if (tramite.getIdEstadoInicial() == null) {
            throw new IllegalArgumentException("El trámite no tiene estado inicial configurado");
        }

        if (tramite.getDiasRespuesta() == null || tramite.getDiasRespuesta() < 0) {
            throw new IllegalArgumentException("El trámite tiene un plazo de respuesta inválido");
        }

        if (texto(r.getCodigo_subserie())) {
            Subseries subserie = subseriesRepository.findByCodigoSubserie(r.getCodigo_subserie())
                    .orElseThrow(() -> new IllegalArgumentException("La subserie seleccionada no existe"));
            if (!texto(r.getCodigo_serie())) {
                throw new IllegalArgumentException("La subserie requiere una serie documental");
            }
            var serie = seriesRepository.findByCodigoSerie(r.getCodigo_serie())
                    .orElseThrow(() -> new IllegalArgumentException("La serie documental seleccionada no existe"));
            if (!subserie.getId_serie().equals(serie.getId_serie())) {
                throw new IllegalArgumentException("La subserie no pertenece a la serie seleccionada");
            }
        }
    }

    private void aplicarReglasDelTramite(Radicados r, Tramites tramite) {
        r.setId_estado(tramite.getIdEstadoInicial() != null ? tramite.getIdEstadoInicial().longValue() : null);
        r.setId_dependencia(tramite.getIdDependenciaResponsable());
        Dependencias dependencia = dependenciasRepository.findById(tramite.getIdDependenciaResponsable())
                .orElseThrow(() -> new IllegalArgumentException("La dependencia responsable no existe"));
        r.setDependencias(dependencia);

        if (!texto(r.getPrioridad()) && texto(tramite.getPrioridadDefault())) {
            r.setPrioridad(tramite.getPrioridadDefault());
        }
        if (!texto(r.getPrioridad())) {
            r.setPrioridad("MEDIA");
        }

        LocalDate fechaBase = parseFecha(r.getFecha_radicado());
        LocalDate fechaLimite = fechaBase.plusDays(tramite.getDiasRespuesta());
        r.setFecha_limite(fechaLimite.format(FECHA));
        r.setFecha_vencimiento(fechaLimite.format(FECHA));

        if (texto(r.getTipoRadicacion())) {
            r.setTipo_radicado(r.getTipoRadicacion().trim().toUpperCase());
        }
        if (!texto(r.getCanalRecepcion())) {
            throw new IllegalArgumentException("Debe seleccionar el canal de recepción");
        }
        r.setMedio_recepcion(r.getCanalRecepcion().trim());
        if (texto(r.getConfidencialidad())) {
            r.setConfidencialidad(normalizarConfidencialidad(r.getConfidencialidad()));
        } else {
            r.setConfidencialidad("PUBLICO");
        }
    }

    private void aplicarClasificacion(Radicados r) {
        if (texto(r.getCodigo_serie())) {
            var serie = seriesRepository.findByCodigoSerie(r.getCodigo_serie())
                    .orElseThrow(() -> new IllegalArgumentException("La serie documental seleccionada no existe"));
            r.setCodigo_serie(serie.getCodigo_serie());
        } else {
            r.setCodigo_serie(null);
        }

        if (texto(r.getCodigo_subserie())) {
            var subserie = subseriesRepository.findByCodigoSubserie(r.getCodigo_subserie())
                    .orElseThrow(() -> new IllegalArgumentException("La subserie documental seleccionada no existe"));
            r.setCodigo_subserie(subserie.getCodigo_subserie());
        } else {
            r.setCodigo_subserie(null);
        }
    }

    private String generarNumeroRadicado() {
        Long consecutivo = radicadosRepository.nextNumeroRadicado();
        return "RAD-" + LocalDate.now().getYear() + "-" + String.format("%06d", consecutivo);
    }

    private String guardarArchivo(Radicados radicado, MultipartFile archivo) {
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String original = StringUtils.cleanPath(
                    archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename());
            String almacenado = radicado.getId_radicado() + "_" + UUID.randomUUID() + "_" + original;
            Path destino = dir.resolve(almacenado).normalize();

            if (!destino.startsWith(dir)) {
                throw new IllegalArgumentException("Nombre de archivo no permitido");
            }

            archivo.transferTo(destino);

            Documentos documento = new Documentos();
            documento.setId_radicado(radicado.getId_radicado());
            documento.setTamano((int) archivo.getSize());
            documento.setNombre(original);
            documento.setNombre_archivo(almacenado);
            documento.setRuta_archivo(destino.toString());
            documento.setTipo(archivo.getContentType());
            documento.setMime_type(archivo.getContentType());
            documento.setChecksum(DigestUtils.md5DigestAsHex(archivo.getBytes()));
            documento.setVersion_actual(1);
            documento.setFecha_subida(LocalDateTime.now().toString());
            documentosRepository.save(documento);

            return destino.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible almacenar el archivo " + archivo.getOriginalFilename(), ex);
        }
    }

    private void validarArchivos(MultipartFile[] archivos) {
        if (archivos == null) {
            return;
        }
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }
            if (archivo.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Cada archivo debe pesar máximo 10 MB");
            }
            String mime = archivo.getContentType();
            if (mime == null || !MIME_PERMITIDOS.contains(mime)) {
                throw new IllegalArgumentException("Tipo de archivo no permitido: " + archivo.getOriginalFilename());
            }
        }
    }

    private void preservarDatosInmutables(Radicados r, Radicados existente) {
        r.setNumero_radicado(existente.getNumero_radicado());
        r.setFecha_radicado(existente.getFecha_radicado());
    }

    private void eliminarArchivosFisicos(List<String> archivos) {
        for (String archivo : archivos) {
            try {
                Files.deleteIfExists(Paths.get(archivo));
            } catch (IOException ignored) {
            }
        }
    }

    private LocalDate parseFecha(String fecha) {
        try {
            return LocalDate.parse(fecha.substring(0, Math.min(10, fecha.length())), FECHA);
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }

    private String normalizarConfidencialidad(String valor) {
        String v = valor.trim().toUpperCase();
        if (v.equals("PÚBLICO") || v.equals("PUBLICO")) return "PUBLICO";
        if (v.equals("INTERNO")) return "INTERNO";
        if (v.equals("RESERVADO")) return "RESERVADO";
        throw new IllegalArgumentException("Nivel de confidencialidad inválido");
    }

    private boolean texto(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record ResultadoRadicacion(Radicados radicado, int archivosGuardados) {}
}
