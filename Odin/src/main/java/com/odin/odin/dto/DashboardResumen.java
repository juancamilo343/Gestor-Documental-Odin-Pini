package com.odin.odin.dto;

import lombok.Data;

@Data
public class DashboardResumen {
    private Long totalRadicados;
    private Long pendientes;
    private Long enTramite;
    private Long finalizados;
    private Long rechazados;
    private Long vencidos;
    private Long proximosAVencer;
    private Long sinAsignar;
    private Long finalizadosHoy;
    private Long usuariosActivos;
    private Long documentosCargados;
    private Long anexosPendientes;
}
