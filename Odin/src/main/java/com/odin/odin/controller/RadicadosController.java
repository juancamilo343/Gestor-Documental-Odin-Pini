package com.odin.odin.controller;

import com.odin.odin.model.*;
import com.odin.odin.repository.*;
import com.odin.odin.service.ComprobanteRadicacionService;
import com.odin.odin.service.AuditoriaRadicadosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.odin.odin.service.OdinUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/radicados")
@Tag(name="Radicados", description="Radicación, consulta, seguimiento y trazabilidad de radicados")
public class RadicadosController {

    @Autowired private RadicadosRepository radicadosRepository;
    @Autowired private ReasignacionesRepository reasignacionesRepository;
    @Autowired private HistorialRadicadoRepository historialRepository;
    @Autowired private ObservacionesRepository observacionesRepository;
    @Autowired private RolesRepository rolesRepository;
    @Autowired private UsuariosRepository usuariosRepository;
    @Autowired private DependenciasRepository dependenciasRepository;
    @Autowired private ComprobanteRadicacionService comprobanteService;
    @Autowired private AuditoriaRadicadosService auditoriaService;
    @Autowired private com.odin.odin.service.RadicadoPlazoService plazoService;
    @Autowired private com.odin.odin.service.ConsecutivoRadicadoService consecutivoService;

    @GetMapping
    @Operation(summary="Listar radicados")
    public List<Radicados> getAll() {
        return radicadosRepository.findAll();
    }

    @GetMapping("/buscar")
    @Operation(summary="Buscar y filtrar radicados")
    public List<Radicados> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long estado,
            @RequestParam(required = false) Long dependencia,
            @RequestParam(required = false) Long tramite) {

        if (texto != null && !texto.isBlank()) {
            return radicadosRepository.buscarTextoCompleto(texto.trim(), estado, dependencia, tramite);
        }
        return radicadosRepository.buscar(texto, estado, dependencia, tramite);
    }

    @GetMapping("/vencidos")
    public List<Radicados> vencidos() {
        return radicadosRepository.findVencidos();
    }

    @GetMapping("/proximos-a-vencer")
    public List<Radicados> proximosAVencer(
            @RequestParam(defaultValue = "3") Integer dias) {

        if (dias == null || dias < 0 || dias > 365) {
            dias = 3;
        }

        return radicadosRepository.findProximosAVencer(dias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Radicados> getById(
            @PathVariable Long id) {

        return radicadosRepository
                .findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/comprobante")
    public ResponseEntity<byte[]> comprobante(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        var opt = radicadosRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Radicados r = opt.get();
            String baseUrl = request.getScheme() + "://" + request.getServerName() +
                    (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
            byte[] pdf = comprobanteService.generar(r, baseUrl);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Comprobante-" + r.getNumero_radicado() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/{id}/expediente")
    public ResponseEntity<?> asociarExpediente(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {
        Long actor = usuarioAutenticado(authentication);
        if (!puedeModificar(actor)) return prohibido("asociar expediente");
        Long expediente = numeroLong(payload.get("id_expediente"));
        if (expediente == null || !radicadosRepository.existsById(id)) return ResponseEntity.badRequest().body(Map.of("error", "Radicado y expediente son obligatorios"));
        return radicadosRepository.findById(id).map(r -> {
            Long anterior = r.getId_expediente();
            r.setId_expediente(expediente);
            Radicados saved = radicadosRepository.save(r);
            registrar(id, actor, "asociacion_expediente", "id_expediente", anterior == null ? null : anterior.toString(), expediente.toString(), "Expediente asociado al radicado");
            return ResponseEntity.ok(resumenRadicado(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialRadicado>> historial(
            @PathVariable Long id) {

        if (!radicadosRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                historialRepository.findByRadicadoOrderByFechaDesc(id)
        );
    }

    @GetMapping("/{id}/reasignaciones")
    public ResponseEntity<List<Reasignaciones>> reasignaciones(@PathVariable Long id) {
        if (!radicadosRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(
                reasignacionesRepository.findByRadicadoOrderByFechaDesc(id)
        );
    }

    @GetMapping("/{id}/observaciones")
    public ResponseEntity<List<Observaciones>> observaciones(
            @PathVariable Long id) {

        if (!radicadosRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                observacionesRepository
                        .findByRadicadoOrderByFechaDesc(id)
        );
    }

    @PostMapping("/{id}/observaciones")
    public ResponseEntity<?> agregarObservacion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(
                    value = "X-User-Id",
                    required = false
            ) Long actor) {

        if (!puedeModificar(actor)) {
            return prohibido("agregar observaciones");
        }

        if (!radicadosRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        String comentario = texto(
                payload.get("comentario")
        );

        Long usuario = actor != null
                ? actor
                : numeroLong(payload.get("usuario"));

        if (comentario.isBlank()
                || usuario == null
                || !usuariosRepository.existsById(usuario)) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    "Comentario y usuario válido son obligatorios"
                            )
                    );
        }

        Observaciones saved =
                observacionesRepository.save(
                        Observaciones.builder()
                                .id_radicado(id)
                                .id_usuario(usuario)
                                .comentario(comentario)
                                .fecha(LocalDateTime.now())
                                .build()
                );

        registrar(
                id,
                usuario,
                "observacion",
                comentario
        );

        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id_observacion", saved.getId_observacion());
        resp.put("id_radicado", id);
        resp.put("comentario", comentario);
        return ResponseEntity.ok(resp);
    }

    @PostMapping
    @Operation(summary="Crear radicado")
    public Radicados create(
            @RequestBody Radicados radicado) {

        if (radicado.getNumero_radicado() == null || radicado.getNumero_radicado().trim().isEmpty()) {
            String tipo = radicado.getTipo_radicado() != null ? radicado.getTipo_radicado() : radicado.getTipoRadicacion();
            if (tipo == null || tipo.trim().isEmpty()) tipo = "ENTRADA";
            radicado.setNumero_radicado(consecutivoService.siguienteNumeroRadicado(tipo));
            radicado.setTipo_radicado(tipo);
        }

        if (radicado.getId_estado() == null) {
            radicado.setId_estado(1L); // 1 = Pendiente
        }

        if (radicado.getFecha_radicado() == null || radicado.getFecha_radicado().trim().isEmpty()) {
            radicado.setFecha_radicado(java.time.LocalDate.now().toString());
        }

        if (Boolean.TRUE.equals(radicado.getAnonimo())) {
            radicado.setRemitente("ANÓNIMO (Ley de Transparencia)");
            radicado.setId_remitente(null);
        } else if (radicado.getRemitente() == null || radicado.getRemitente().trim().isEmpty()) {
            radicado.setRemitente("Anónimo");
        }

        plazoService.aplicarReglas(radicado);
        Radicados saved =
                radicadosRepository.save(radicado);

        registrar(
                saved.getId_radicado(),
                usuario(saved),
                "radicacion",
                "Radicado creado"
        );

        return saved;
    }

    @PutMapping("/{id}")
    @Operation(summary="Actualizar radicado")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Radicados radicado) {

        return radicadosRepository
                .findById(id)
                .map(existing -> {

                    radicado.setId_radicado(id);

                    Radicados saved =
                            radicadosRepository.save(radicado);

                    registrar(
                            id,
                            usuario(saved),
                            "modificacion",
                            "Radicado actualizado"
                    );

                    return ResponseEntity.ok(resumenRadicado(saved));

                })
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(
                    value = "X-User-Id",
                    required = false
            ) Long actor) {

        if (!puedeModificar(actor)) {
            return prohibido("cambiar estados");
        }

        Long nuevo =
                numeroLong(payload.get("estado"));

        if (nuevo == null || nuevo <= 0) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    "Estado inválido"
                            )
                    );
        }

        return radicadosRepository
                .findById(id)
                .map(r -> {

                    Long anterior =
                            r.getId_estado();

                    if (!transicionPermitida(
                            anterior,
                            nuevo
                    )) {

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        Map.of(
                                                "error",
                                                "Transición de estado no permitida: "
                                                        + anterior
                                                        + " -> "
                                                        + nuevo
                                        )
                                );
                    }

                    r.setId_estado(nuevo);

                    Radicados saved =
                            radicadosRepository.save(r);

                    registrar(
                            id,
                            actor != null
                                    ? actor
                                    : usuario(saved),
                            "cambio_estado",
                            "Estado "
                                    + anterior
                                    + " -> "
                                    + nuevo
                    );

                    return ResponseEntity.ok(resumenRadicado(saved));

                })
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<?> asignar(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(
                    value = "X-User-Id",
                    required = false
            ) Long actor) {

        if (!puedeModificar(actor)) {
            return prohibido("asignar radicados");
        }

        Long nuevo =
                numeroLong(payload.get("usuario"));

        if (nuevo == null
                || nuevo <= 0
                || !usuariosRepository
                .existsById(nuevo.longValue())) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    "Usuario responsable inexistente"
                            )
                    );
        }

        return radicadosRepository
                .findById(id)
                .map(r -> {

                    Long anterior =
                            r.getId_usuario();

                    r.setId_usuario(nuevo);

                    Radicados saved =
                            radicadosRepository.save(r);

                    registrar(
                            id,
                            actor != null
                                    ? actor
                                    : nuevo.longValue(),
                            "asignacion",
                            "Responsable "
                                    + anterior
                                    + " -> "
                                    + nuevo
                    );

                    return ResponseEntity.ok(resumenRadicado(saved));

                })
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    @PostMapping("/{id}/reasignar")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> reasignar(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-User-Id", required = false) Long actor,
            Authentication authentication) {

        Long actorId = actor;
        if (actorId == null && authentication != null
                && authentication.getPrincipal() instanceof OdinUserDetails u) {
            actorId = u.getIdUsuario();
        }

        if (!puedeModificar(actorId)) {
            return prohibido("reasignar radicados");
        }

        Long nuevo = numeroLong(payload.get("usuarioNuevo"));
        if (nuevo == null) nuevo = numeroLong(payload.get("usuario"));
        Long dependencia = numeroLong(payload.get("dependenciaNueva"));
        if (dependencia == null) dependencia = numeroLong(payload.get("dependencia"));

        if (nuevo == null
                || dependencia == null
                || nuevo <= 0
                || dependencia <= 0
                || !usuariosRepository.existsById(nuevo)
                || !dependenciasRepository.existsById(dependencia)) {

            return ResponseEntity.badRequest().body(
                    Map.of("error", "Usuario o dependencia inválidos")
            );
        }

        var opt = radicadosRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Radicados r = opt.get();
        Long anterior = r.getId_usuario();

        try {
            int actualizados = radicadosRepository.actualizarAsignacion(id, nuevo, dependencia);
            if (actualizados == 0) {
                // Fallback JPA por si el nativo no afectó filas
                r.setId_usuario(nuevo);
                r.setId_dependencia(dependencia);
                radicadosRepository.save(r);
            }

            try {
                Reasignaciones re = new Reasignaciones();
                re.setId_radicado(id);
                // Si no había responsable previo, registrar el mismo nuevo para no violar NOT NULL
                re.setId_usuario_anterior(anterior != null ? anterior : nuevo);
                re.setId_usuario_nuevo(nuevo);
                re.setId_dependencia_nueva(dependencia);
                re.setFecha(LocalDateTime.now());
                reasignacionesRepository.save(re);
            } catch (Exception ex) {
                // La reasignación principal ya se aplicó; el historial de reasignaciones no debe tumbar la operación
                System.err.println("Aviso: no se pudo guardar fila reasignaciones: " + ex.getMessage());
            }

            Long quien = actorId != null ? actorId : nuevo;
            registrar(
                    id,
                    quien,
                    "reasignacion",
                    "Responsable " + anterior + " -> " + nuevo
                    + " / dependencia " + dependencia
            );

            java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
            resp.put("ok", true);
            resp.put("id_radicado", id);
            resp.put("id_usuario", nuevo);
            resp.put("id_dependencia", dependencia);
            resp.put("mensaje", "Reasignación realizada");
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Error al reasignar: " + ex.getMessage())
            );
        }
    }

    @PatchMapping("/{id}/cerrar")
    public ResponseEntity<?> cerrar(
            @PathVariable Long id,
            @RequestBody(required = false)
            Map<String, Object> payload,
            @RequestHeader(
                    value = "X-User-Id",
                    required = false
            ) Long actor) {

        if (!puedeModificar(actor)) {
            return prohibido("cerrar radicados");
        }

        return radicadosRepository
                .findById(id)
                .map(r -> {

                    Long anterior =
                            r.getId_estado();

                    Long estado =
                            payload == null
                                    ? null
                                    : numeroLong(
                                    payload.get("estado")
                            );

                    if (estado == null) {
                        estado = 3L;
                    }

                    if (!transicionPermitida(
                            anterior,
                            estado
                    )) {

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        Map.of(
                                                "error",
                                                "El radicado no puede cerrarse desde el estado actual"
                                        )
                                );
                    }

                    Long usuarioCierre =
                            actor != null
                                    ? actor
                                    : usuario(r);

                    if (usuarioCierre == null
                            || !usuariosRepository
                            .existsById(usuarioCierre)) {

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        Map.of(
                                                "error",
                                                "Usuario de cierre inválido"
                                        )
                                );
                    }

                    r.setId_estado(estado);
                    r.setFecha_cierre(
                            LocalDateTime.now()
                    );
                    r.setId_usuario_cierre(
                            usuarioCierre
                    );

                    Radicados saved =
                            radicadosRepository.save(r);

                    String obs =
                            texto(
                                    payload == null
                                            ? null
                                            : payload.get(
                                            "observacion"
                                    )
                            );

                    registrar(
                            id,
                            usuarioCierre,
                            "cierre",
                            obs.isBlank()
                                    ? "Radicado cerrado"
                                    : obs
                    );

                    if (!obs.isBlank()) {

                        observacionesRepository.save(
                                Observaciones.builder()
                                        .id_radicado(id)
                                        .id_usuario(
                                                usuarioCierre
                                        )
                                        .comentario(obs)
                                        .fecha(
                                                LocalDateTime.now()
                                        )
                                        .build()
                        );
                    }

                    return ResponseEntity.ok(resumenRadicado(saved));

                })
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/{id}/permisos")
    public ResponseEntity<Map<String, Object>> permisos(
            @PathVariable Long id,
            @RequestParam Long usuario) {

        if (!radicadosRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                Map.of(
                        "usuario",
                        usuario,
                        "puedeModificar",
                        puedeModificar(usuario)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        if (!radicadosRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        Radicados r =
                radicadosRepository
                        .findById(id)
                        .orElse(null);

        Long actor =
                usuario(r);

        registrar(
                id,
                actor,
                "eliminacion",
                "Radicado eliminado"
        );

        radicadosRepository.deleteById(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    private boolean transicionPermitida(
            Long actual,
            Long nuevo) {

        if (nuevo == null) {
            return false;
        }
        // mismo estado: no-op (permitimos para evitar error en UI)
        if (actual != null && actual.equals(nuevo)) {
            return true;
        }
        // sin estado previo: cualquier transición válida
        if (actual == null) {
            return nuevo >= 1 && nuevo <= 20;
        }
        // permitir reapertura desde finalizado/rechazado hacia estados operativos
        // y cierre desde cualquier estado operativo (admin / gestor)
        return nuevo >= 1 && nuevo <= 20;
    }

    private void registrar(
            Long id,
            Long usuario,
            String accion,
            String descripcion) {
        registrar(id, usuario, accion, null, null, null, descripcion);
    }

    private void registrar(
            Long id,
            Long usuario,
            String accion,
            String campo,
            String anterior,
            String nuevo,
            String descripcion) {

        if (id == null
                || usuario == null
                || !usuariosRepository
                .existsById(usuario)) {

            return;
        }

        historialRepository.save(
                HistorialRadicado.builder()
                        .id_radicado(id)
                        .id_usuario(usuario)
                        .accion(accion)
                        .descripcion(descripcion)
                        .fecha(LocalDateTime.now())
                        .build()
        );

        auditoriaService.registrar(
                id, usuario, accion, campo, anterior, nuevo, descripcion
        );
    }


    private Map<String, Object> resumenRadicado(Radicados r) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        if (r == null) return m;
        m.put("id_radicado", r.getId_radicado());
        m.put("numero_radicado", r.getNumero_radicado());
        m.put("id_estado", r.getId_estado());
        m.put("id_usuario", r.getId_usuario());
        m.put("id_dependencia", r.getId_dependencia());
        m.put("id_tramite", r.getId_tramite());
        m.put("asunto", r.getAsunto());
        m.put("fecha_cierre", r.getFecha_cierre() != null ? r.getFecha_cierre().toString() : null);
        m.put("ok", true);
        return m;
    }

    private boolean puedeModificar(Long id) {

        if (id == null) {
            return true;
        }

        return usuariosRepository
                .findById(id)
                .map(Usuarios::getId_rol)
                .flatMap(rolesRepository::findById)
                .map(
                        r ->
                                rolPermitido(r.getRol())
                                        || rolPermitido(
                                        r.getNombre()
                                )
                )
                .orElse(false);
    }

    private boolean rolPermitido(String r) {

        if (r == null) {
            return false;
        }

        String v =
                r.toLowerCase();

        return v.contains("admin")
                || v.contains("coordin")
                || v.contains("gestor")
                || v.contains("oper")
                || v.contains("recep");
    }

    private Long usuarioAutenticado(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OdinUserDetails u)) return null;
        return u.getIdUsuario();
    }

    private Long usuario(Radicados r) {

        return r == null
                || r.getId_usuario() == null
                ? null
                : r.getId_usuario();
    }

    private Integer numero(Object v) {

        try {

            return v == null
                    ? null
                    : v instanceof Number
                    ? ((Number) v).intValue()
                    : Integer.valueOf(
                    v.toString().trim()
            );

        } catch (Exception e) {

            return null;
        }
    }

    private Long numeroLong(Object v) {

        try {

            return v == null
                    ? null
                    : v instanceof Number
                    ? ((Number) v).longValue()
                    : Long.valueOf(
                    v.toString().trim()
            );

        } catch (Exception e) {

            return null;
        }
    }

    private String texto(Object v) {

        return v == null
                ? ""
                : v.toString().trim();
    }

    private ResponseEntity<Map<String, String>>
    prohibido(String accion) {

        return ResponseEntity
                .status(403)
                .body(
                        Map.of(
                                "error",
                                "El rol no tiene permiso para "
                                        + accion
                        )
                );
    }
}