package com.odin.odin.controller;

import com.odin.odin.model.Estados;
import com.odin.odin.repository.EstadosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/estados")
public class EstadosController
{
    @Autowired
    private EstadosRepository estadosRepository;

    @GetMapping
    public List<Estados> getAll()
    {
        return estadosRepository.findAll();
    }

    @GetMapping("/{id}")
    public Estados getById(@PathVariable Long id)
    {
        return estadosRepository.findById(id).orElse(null);
    }
    @PostMapping
    public Estados create(@RequestBody Estados estados)
    {
        return estadosRepository.save(estados);
    }

    @PutMapping("/{id}")
    public Estados update(@PathVariable long id, @RequestBody Estados estados)
    {
        estados.setId_estado(id);
        return estadosRepository.save(estados);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id)
    {
        estadosRepository.deleteById(id);
    }
}
