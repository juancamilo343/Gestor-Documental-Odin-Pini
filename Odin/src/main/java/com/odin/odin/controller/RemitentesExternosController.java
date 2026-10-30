package com.odin.odin.controller;

import com.odin.odin.model.RemitentesExternos;
import com.odin.odin.model.Usuarios;
import com.odin.odin.repository.RemitentesExternosRepository;
import com.odin.odin.repository.UsuariosRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/remitentes")
@Tag(name = "Remitentes", description = "Consulta y autocompletado de remitentes y solicitantes externos")
public class RemitentesExternosController {

    @Autowired
    private RemitentesExternosRepository remitentesRepository;

    @Autowired(required = false)
    private UsuariosRepository usuariosRepository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{numIdentificacion}")
    @Operation(summary = "Consultar persona/remitente por número de documento para autocompletado en radicación")
    public ResponseEntity<?> buscarPorDocumento(
            @PathVariable String numIdentificacion,
            @RequestParam(required = false) String tipo) {

        if (numIdentificacion == null || numIdentificacion.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El número de documento es obligatorio"
            ));
        }

        String num = numIdentificacion.trim();

        try {
            // 1. PRIORIDAD: Consultar en Gestión de Usuarios (acceso exclusivo en modo lectura)
            Usuarios usuarioEncontrado = null;

            if (usuariosRepository != null) {
                // Aprovecha el método existente findByLoginFlexible en UsuariosRepository
                Optional<Usuarios> optFlex = usuariosRepository.findByLoginFlexible(num);
                if (optFlex.isPresent()) {
                    Usuarios u = optFlex.get();
                    if (u.getNum_identificacion() != null && u.getNum_identificacion().trim().equalsIgnoreCase(num)) {
                        usuarioEncontrado = u;
                    }
                }
            }

            // Respaldo de solo lectura con JdbcTemplate por si existen diferencias de formato
            if (usuarioEncontrado == null && jdbcTemplate != null) {
                try {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                            "SELECT id_usuario, nombre, tipo_identificacion, num_identificacion, correo, direccion, telefono, estado " +
                            "FROM usuarios WHERE TRIM(num_identificacion) = ? LIMIT 1",
                            num
                    );
                    if (!rows.isEmpty()) {
                        Map<String, Object> row = rows.get(0);
                        usuarioEncontrado = new Usuarios();
                        usuarioEncontrado.setId_usuario(row.get("id_usuario") != null ? ((Number) row.get("id_usuario")).longValue() : null);
                        usuarioEncontrado.setNombre((String) row.get("nombre"));
                        usuarioEncontrado.setTipo_identificacion((String) row.get("tipo_identificacion"));
                        usuarioEncontrado.setNum_identificacion((String) row.get("num_identificacion"));
                        usuarioEncontrado.setCorreo((String) row.get("correo"));
                        usuarioEncontrado.setDireccion((String) row.get("direccion"));
                        usuarioEncontrado.setTelefono((String) row.get("telefono"));
                        usuarioEncontrado.setEstado((String) row.get("estado"));
                    }
                } catch (Exception ignored) {
                    // Si la consulta directa no procede, continúa la búsqueda en remitentes
                }
            }

            if (usuarioEncontrado != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("origen", "USUARIO");
                data.put("id_usuario", usuarioEncontrado.getId_usuario());
                data.put("tipo_identificacion", usuarioEncontrado.getTipo_identificacion());
                data.put("num_identificacion", usuarioEncontrado.getNum_identificacion());
                data.put("nombre", usuarioEncontrado.getNombre() != null ? usuarioEncontrado.getNombre().trim() : "");
                data.put("apellido", "");
                data.put("nombre_completo", usuarioEncontrado.getNombre() != null ? usuarioEncontrado.getNombre().trim() : "");
                data.put("correo", usuarioEncontrado.getCorreo() != null ? usuarioEncontrado.getCorreo().trim() : "");
                data.put("telefono", usuarioEncontrado.getTelefono() != null ? usuarioEncontrado.getTelefono().trim() : "");
                data.put("direccion", usuarioEncontrado.getDireccion() != null ? usuarioEncontrado.getDireccion().trim() : "");

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("origen", "USUARIO");
                resp.put("data", data);
                resp.put("message", "Persona registrada encontrada en Gestión de Usuarios");
                return ResponseEntity.ok(resp);
            }

            // 2. Consulta en histórico de Remitentes Externos frecuentes
            Optional<RemitentesExternos> opt = (tipo != null && !tipo.trim().isEmpty())
                    ? remitentesRepository.findByNumIdentificacionAndTipo(num, tipo.trim())
                    : remitentesRepository.findByNumIdentificacion(num);

            if (opt.isPresent()) {
                RemitentesExternos r = opt.get();
                Map<String, Object> data = new HashMap<>();
                data.put("origen", "REMITENTE_EXTERNO");
                data.put("id_remitente", r.getId_remitente());
                data.put("tipo_identificacion", r.getTipo_identificacion());
                data.put("num_identificacion", r.getNum_identificacion());
                data.put("nombre", r.getNombre() != null ? r.getNombre().trim() : "");
                data.put("apellido", r.getApellido() != null ? r.getApellido().trim() : "");
                String nombreCompleto = ((r.getNombre() != null ? r.getNombre() : "") + " " + (r.getApellido() != null ? r.getApellido() : "")).trim();
                data.put("nombre_completo", nombreCompleto);
                data.put("correo", r.getCorreo() != null ? r.getCorreo().trim() : "");
                data.put("telefono", r.getTelefono() != null ? r.getTelefono().trim() : "");
                data.put("direccion", r.getDireccion() != null ? r.getDireccion().trim() : "");

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("origen", "REMITENTE_EXTERNO");
                resp.put("data", data);
                resp.put("message", "Remitente externo frecuente encontrado");
                return ResponseEntity.ok(resp);
            }

            // 3. Si no existe en Gestión de Usuarios ni en remitentes externos
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "origen", "NO_ENCONTRADO",
                    "message", "Persona no registrada en el sistema. Puede continuar ingresando los datos manualmente."
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "origen", "ERROR",
                    "message", "Inconveniente al consultar la identificación: " + e.getMessage()
            ));
        }
    }
}
