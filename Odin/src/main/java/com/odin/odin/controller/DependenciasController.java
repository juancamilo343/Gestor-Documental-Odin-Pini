package com.odin.odin.controller;

import com.odin.odin.model.Dependencias;
import com.odin.odin.model.Usuarios;
import com.odin.odin.repository.DependenciasRepository;
import com.odin.odin.repository.UsuariosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dependencias")
public class DependenciasController
{
    @Autowired
    private DependenciasRepository dependenciasRepository;

    @Autowired
    private UsuariosRepository usuariosRepository;

    @GetMapping
    public List<Dependencias> getAll()
    {
        return dependenciasRepository.findAll();
    }

    @GetMapping("/{id}")
    public Dependencias getById(@PathVariable Long id) {
        return dependenciasRepository.findById(id).orElse(null);
    }

    @GetMapping("/{id}/responsable")
    public ResponseEntity<?> getResponsable(@PathVariable Long id) {
        List<Usuarios> usuarios = usuariosRepository.findActivosByDependencia(id);
        if (usuarios.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "No se encontró ningún responsable activo para la dependencia especificada"
            ));
        }

        Usuarios u = usuarios.get(0);
        Map<String, Object> data = new HashMap<>();
        data.put("id_usuario", u.getId_usuario());
        data.put("nombre", u.getNombre());
        data.put("correo", u.getCorreo());
        data.put("id_dependencia", u.getId_dependencia());

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        resp.put("message", "Encargado de la dependencia obtenido exitosamente");
        return ResponseEntity.ok(resp);
    }

    @PostMapping
    public Dependencias create(@RequestBody Dependencias dependencias)
    {
        return dependenciasRepository.save(dependencias);
    }

    @PutMapping("/{id}")
    public Dependencias update(@PathVariable long id, @RequestBody Dependencias dependencias)
    {
        dependencias.setId_dependencia(id);
        return dependenciasRepository.save(dependencias);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id)
    {
        dependenciasRepository.deleteById(id);
    }
}
