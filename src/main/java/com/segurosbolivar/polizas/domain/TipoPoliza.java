package com.segurosbolivar.polizas.domain;

/**
 * Tipo de poliza de arrendamiento.
 *
 * <ul>
 *   <li>INDIVIDUAL: el tomador y asegurado es el arrendatario, el beneficiario es el arrendador.
 *       Solo puede tener 1 riesgo.</li>
 *   <li>COLECTIVA: orientada a inmobiliarias y administraciones de copropiedades.
 *       Puede tener uno o muchos riesgos.</li>
 * </ul>
 */
public enum TipoPoliza {
    INDIVIDUAL,
    COLECTIVA
}
