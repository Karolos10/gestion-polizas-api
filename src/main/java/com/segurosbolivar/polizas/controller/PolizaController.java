package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.dto.CrearRiesgoRequest;
import com.segurosbolivar.polizas.dto.PolizaResponse;
import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.service.PolizaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de gestion de polizas.
 * Todos requieren el header de seguridad {@code x-api-key}.
 */
@RestController
@RequestMapping("/polizas")
public class PolizaController {

    private final PolizaService polizaService;

    public PolizaController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    /** GET /polizas?tipo=COLECTIVA&estado=VIGENTE (ambos opcionales). */
    @GetMapping
    public List<PolizaResponse> listar(
            @RequestParam(required = false) TipoPoliza tipo,
            @RequestParam(required = false) EstadoPoliza estado) {
        return polizaService.listar(tipo, estado).stream()
                .map(PolizaResponse::desde)
                .toList();
    }

    /** GET /polizas/{id} */
    @GetMapping("/{id}")
    public PolizaResponse obtener(@PathVariable Long id) {
        return PolizaResponse.desde(polizaService.obtener(id));
    }

    /** GET /polizas/{id}/riesgos */
    @GetMapping("/{id}/riesgos")
    public List<RiesgoResponse> listarRiesgos(@PathVariable Long id) {
        return polizaService.listarRiesgos(id).stream()
                .map(RiesgoResponse::desde)
                .toList();
    }

    /** POST /polizas/{id}/renovar */
    @PostMapping("/{id}/renovar")
    public PolizaResponse renovar(@PathVariable Long id) {
        return PolizaResponse.desde(polizaService.renovar(id));
    }

    /** POST /polizas/{id}/cancelar */
    @PostMapping("/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable Long id) {
        return PolizaResponse.desde(polizaService.cancelar(id));
    }

    /** POST /polizas/{id}/riesgos (solo COLECTIVA). */
    @PostMapping("/{id}/riesgos")
    public ResponseEntity<RiesgoResponse> agregarRiesgo(
            @PathVariable Long id,
            @Valid @RequestBody CrearRiesgoRequest request) {
        RiesgoResponse creado = RiesgoResponse.desde(polizaService.agregarRiesgo(id, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
