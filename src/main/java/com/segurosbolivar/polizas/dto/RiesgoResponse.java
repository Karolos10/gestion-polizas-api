package com.segurosbolivar.polizas.dto;

import com.segurosbolivar.polizas.domain.EstadoRiesgo;
import com.segurosbolivar.polizas.domain.Riesgo;

/**
 * Representacion de salida de un riesgo.
 */
public record RiesgoResponse(
        Long id,
        String descripcion,
        String arrendatario,
        EstadoRiesgo estado,
        Long polizaId
) {
    public static RiesgoResponse desde(Riesgo riesgo) {
        return new RiesgoResponse(
                riesgo.getId(),
                riesgo.getDescripcion(),
                riesgo.getArrendatario(),
                riesgo.getEstado(),
                riesgo.getPoliza() != null ? riesgo.getPoliza().getId() : null
        );
    }
}
