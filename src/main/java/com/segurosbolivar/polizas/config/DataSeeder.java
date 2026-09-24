package com.segurosbolivar.polizas.config;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.EstadoRiesgo;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carga datos de ejemplo al arrancar para facilitar las pruebas manuales.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seed(PolizaRepository polizaRepository) {
        return args -> {
            if (polizaRepository.count() > 0) {
                return;
            }

            // Poliza INDIVIDUAL: tomador y asegurado = arrendatario, beneficiario = arrendador.
            Poliza individual = Poliza.builder()
                    .tipo(TipoPoliza.INDIVIDUAL)
                    .estado(EstadoPoliza.VIGENTE)
                    .tomador("Juan Perez (arrendatario)")
                    .asegurado("Juan Perez (arrendatario)")
                    .beneficiario("Maria Gomez (arrendador)")
                    .vigenciaInicio(LocalDate.of(2026, 1, 1))
                    .vigenciaFin(LocalDate.of(2026, 12, 31))
                    .mesesVigencia(12)
                    .canonMensual(new BigDecimal("1500000.00"))
                    .prima(new BigDecimal("18000000.00"))
                    .build();
            Riesgo riesgoInd = Riesgo.builder()
                    .descripcion("Apartamento 302, Torre A")
                    .arrendatario("Juan Perez")
                    .estado(EstadoRiesgo.ACTIVO)
                    .build();
            individual.agregarRiesgo(riesgoInd);

            // Poliza COLECTIVA: inmobiliaria, varios riesgos (arrendatarios).
            Poliza colectiva = Poliza.builder()
                    .tipo(TipoPoliza.COLECTIVA)
                    .estado(EstadoPoliza.VIGENTE)
                    .tomador("Inmobiliaria XYZ")
                    .asegurado("Arrendatarios varios")
                    .beneficiario("Arrendadores varios")
                    .vigenciaInicio(LocalDate.of(2026, 3, 1))
                    .vigenciaFin(LocalDate.of(2027, 2, 28))
                    .mesesVigencia(12)
                    .canonMensual(new BigDecimal("2000000.00"))
                    .prima(new BigDecimal("24000000.00"))
                    .build();
            colectiva.agregarRiesgo(Riesgo.builder()
                    .descripcion("Local comercial 12")
                    .arrendatario("Cafeteria El Sol")
                    .estado(EstadoRiesgo.ACTIVO)
                    .build());
            colectiva.agregarRiesgo(Riesgo.builder()
                    .descripcion("Oficina 405")
                    .arrendatario("Contadores Asociados")
                    .estado(EstadoRiesgo.ACTIVO)
                    .build());

            polizaRepository.save(individual);
            polizaRepository.save(colectiva);

            log.info("[SEED] Cargadas {} polizas de ejemplo", polizaRepository.count());
        };
    }
}
