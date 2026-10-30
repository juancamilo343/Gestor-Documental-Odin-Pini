package com.odin.odin.controller;

import com.odin.odin.model.RemitentesExternos;
import com.odin.odin.repository.RemitentesExternosRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/remitentes")
@Tag(name = "Remitentes", description = "Consulta y gestión de remitentes externos")
public class RemitentesExternosController {

    @Autowired
    private RemitentesExternosRepository remitentesRepository;

    @GetMapping("/{numIdentificacion}")
    @Operation(summary = "Consultar remitente por documento")
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
        Optional<RemitentesExternos> opt = (tipo != null && !tipo.trim().isEmpty())
                ? remitentesRepository.findByNumIdentificacionAndTipo(num, tipo.trim())
                : remitentesRepository.findByNumIdentificacion(num);

        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "No se encontró ningún remitente registrado con el número de documento proporcionado."
            ));
        }

        RemitentesExternos r = opt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("id_remitente", r.getId_remitente());
        data.put("tipo_identificacion", r.getTipo_identificacion());
        data.put("num_identificacion", r.getNum_identificacion());
        data.put("nombre", r.getNombre());
        data.put("apellido", r.getApellido());
        data.put("nombre_completo", (r.getNombre() + " " + (r.getApellido() != null ? r.getApellido() : "")).trim());
        data.put("correo", r.getCorreo());
        data.put("telefono", r.getTelefono());
        data.put("direccion", r.getDireccion());

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        resp.put("message", "Remitente externo encontrado");

        return ResponseEntity.ok(resp);
    }
}
