package com.segurosbolivar.polizas.dto;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Representacion de salida de una poliza.
 */
public record PolizaResponse(
        Long id,
        TipoPoliza tipo,
        EstadoPoliza estado,
        String tomador,
        String asegurado,
        String beneficiario,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFin,
        Integer mesesVigencia,
        BigDecimal canonMensual,
        BigDecimal prima,
        List<RiesgoResponse> riesgos
) {
    public static PolizaResponse desde(Poliza p) {
        return new PolizaResponse(
                p.getId(),
                p.getTipo(),
                p.getEstado(),
                p.getTomador(),
                p.getAsegurado(),
                p.getBeneficiario(),
                p.getVigenciaInicio(),
                p.getVigenciaFin(),
                p.getMesesVigencia(),
                p.getCanonMensual(),
                p.getPrima(),
                p.getRiesgos().stream().map(RiesgoResponse::desde).toList()
        );
    }
}
