package com.odin.odin.service;

import com.odin.odin.dto.DashboardResumen;
import com.odin.odin.repository.DocumentosRepository;
import com.odin.odin.repository.RadicadosRepository;
import com.odin.odin.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RadicadosRepository radicadosRepository;
    private final UsuariosRepository usuariosRepository;
    private final DocumentosRepository documentosRepository;

    public DashboardResumen obtenerResumen() {
        DashboardResumen resumen = new DashboardResumen();

        resumen.setTotalRadicados(valor(radicadosRepository.count()));
        resumen.setPendientes(valor(radicadosRepository.countPendientes()));
        resumen.setEnTramite(valor(radicadosRepository.countEnTramite()));
        resumen.setFinalizados(valor(radicadosRepository.countFinalizados()));
        resumen.setRechazados(valor(radicadosRepository.countRechazados()));
        resumen.setVencidos(valor(radicadosRepository.countVencidos()));
        resumen.setProximosAVencer(valor(radicadosRepository.countProximosAVencer()));
        resumen.setSinAsignar(valor(radicadosRepository.countSinAsignar()));
        resumen.setFinalizadosHoy(valor(radicadosRepository.countFinalizadosHoy()));
        resumen.setUsuariosActivos(valor(usuariosRepository.count()));
        resumen.setDocumentosCargados(valor(documentosRepository.count()));

        // No se calcula hasta que exista una tabla/repositorio de anexos pendiente.
        resumen.setAnexosPendientes(0L);

        return resumen;
    }

    private Long valor(Long valor) {
        return valor == null ? 0L : valor;
    }
}
