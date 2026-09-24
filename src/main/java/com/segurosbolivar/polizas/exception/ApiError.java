package com.segurosbolivar.polizas.exception;

import java.time.LocalDateTime;

/**
 * Cuerpo estandar de respuesta ante un error.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje
) {
    public static ApiError de(int status, String error, String mensaje) {
        return new ApiError(LocalDateTime.now(), status, error, mensaje);
    }
}
