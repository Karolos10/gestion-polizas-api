package com.segurosbolivar.polizas.exception;

/**
 * Se lanza cuando no se encuentra una poliza o riesgo por su identificador.
 */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
