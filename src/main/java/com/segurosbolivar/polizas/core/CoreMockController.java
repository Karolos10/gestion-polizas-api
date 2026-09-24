package com.segurosbolivar.polizas.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Mock externo del CORE transaccional. Su unico proposito es registrar en logs
 * que la operacion se intento enviar al CORE.
 *
 * <p>Ejemplo de cuerpo esperado:</p>
 * <pre>
 * { "evento": "ACTUALIZACION", "polizaId": 555 }
 * </pre>
 */
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/evento")
    public ResponseEntity<Map<String, Object>> recibirEvento(@RequestBody CoreEventoRequest request) {
        log.info("[CORE-MOCK] Evento recibido -> evento='{}', polizaId={}",
                request.evento(), request.polizaId());

        return ResponseEntity.ok(Map.of(
                "status", "RECIBIDO",
                "evento", request.evento(),
                "polizaId", request.polizaId()
        ));
    }
}
