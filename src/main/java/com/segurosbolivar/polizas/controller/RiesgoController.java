package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.service.PolizaService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion de riesgos de forma independiente.
 * Requiere el header de seguridad {@code x-api-key}.
 */
@RestController
@RequestMapping("/riesgos")
public class RiesgoController {

    private final PolizaService polizaService;

    public RiesgoController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    /** POST /riesgos/{id}/cancelar */
    @PostMapping("/{id}/cancelar")
    public RiesgoResponse cancelar(@PathVariable Long id) {
        return RiesgoResponse.desde(polizaService.cancelarRiesgo(id));
    }
}
