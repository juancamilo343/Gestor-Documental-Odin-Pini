package com.odin.odin.controller;

import com.odin.odin.dto.DashboardResumen;
import com.odin.odin.model.Radicados;
import com.odin.odin.repository.RadicadosRepository;
import com.odin.odin.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/view")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RadicadosRepository radicadosRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardResumen resumen = dashboardService.obtenerResumen();

        model.addAttribute("totalRadicados", resumen.getTotalRadicados());
        model.addAttribute("pendientes", resumen.getPendientes());
        model.addAttribute("enTramite", resumen.getEnTramite());
        model.addAttribute("finalizados", resumen.getFinalizados());
        model.addAttribute("rechazados", resumen.getRechazados());
        model.addAttribute("vencidos", resumen.getVencidos());
        model.addAttribute("proximosAVencer", resumen.getProximosAVencer());
        model.addAttribute("sinAsignar", resumen.getSinAsignar());
        model.addAttribute("finalizadosHoy", resumen.getFinalizadosHoy());
        model.addAttribute("usuariosActivos", resumen.getUsuariosActivos());
        model.addAttribute("documentosCargados", resumen.getDocumentosCargados());
        model.addAttribute("anexosPendientes", resumen.getAnexosPendientes());

        List<Radicados> ultimosRadicados = radicadosRepository.findTop5UltimosRadicados();
        model.addAttribute("ultimosRadicados", ultimosRadicados);

        return "dashboard/dashboard";
    }
}
