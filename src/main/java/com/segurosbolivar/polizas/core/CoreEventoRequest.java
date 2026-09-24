package com.segurosbolivar.polizas.core;

/**
 * Evento que se envia al CORE transaccional legado a traves de la capa media.
 */
public record CoreEventoRequest(
        String evento,
        Long polizaId
) {
}
