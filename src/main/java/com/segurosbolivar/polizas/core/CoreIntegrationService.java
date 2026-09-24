package com.segurosbolivar.polizas.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio agnostico de edicion. Toda accion que modifique el estado de una
 * poliza o de un riesgo debe invocar este servicio, que en produccion estaria
 * respaldado por la capa media en WebLogic encargada de mantener actualizado el
 * sistema CORE de seguros.
 *
 * <p>En esta prueba la integracion se simula: se registra en logs el intento de
 * envio al CORE (equivalente a lo que hace el endpoint POST /core-mock/evento).</p>
 */
@Service
public class CoreIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(CoreIntegrationService.class);

    /**
     * Notifica al CORE un cambio de estado sobre una poliza.
     *
     * @param evento   tipo de evento (ej. ACTUALIZACION, CREACION, CANCELACION)
     * @param polizaId identificador de la poliza afectada
     */
    public void notificarEvento(String evento, Long polizaId) {
        // En un entorno real aqui se invocaria el servicio SOAP/REST expuesto por
        // la capa media en WebLogic. Para la prueba solo dejamos traza.
        log.info("[CORE] Enviando evento al CORE de seguros -> evento='{}', polizaId={}", evento, polizaId);
    }
}
