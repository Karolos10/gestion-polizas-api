package com.segurosbolivar.polizas.exception;

/**
 * Se lanza cuando una operacion viola una regla de negocio
 * (por ejemplo, renovar una poliza cancelada o agregar un segundo riesgo
 * a una poliza individual).
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
