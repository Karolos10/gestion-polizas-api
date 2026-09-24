package com.segurosbolivar.polizas.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos requeridos para agregar un riesgo a una poliza colectiva.
 */
public record CrearRiesgoRequest(
        @NotBlank(message = "La descripcion del riesgo es obligatoria")
        String descripcion,

        @NotBlank(message = "El arrendatario es obligatorio")
        String arrendatario
) {
}
