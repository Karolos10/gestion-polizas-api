package com.segurosbolivar.polizas.service;

import com.segurosbolivar.polizas.core.CoreIntegrationService;
import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.EstadoRiesgo;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.dto.CrearRiesgoRequest;
import com.segurosbolivar.polizas.exception.RecursoNoEncontradoException;
import com.segurosbolivar.polizas.exception.ReglaNegocioException;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import com.segurosbolivar.polizas.repository.RiesgoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Logica de negocio de la gestion de polizas y sus riesgos.
 */
@Service
public class PolizaService {

    /** Incremento del IPC aplicado al renovar (12,5% como valor de ejemplo). */
    private static final BigDecimal IPC = new BigDecimal("0.125");

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final CoreIntegrationService coreIntegrationService;

    public PolizaService(PolizaRepository polizaRepository,
                         RiesgoRepository riesgoRepository,
                         CoreIntegrationService coreIntegrationService) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.coreIntegrationService = coreIntegrationService;
    }

    /** GET /polizas -> lista filtrando por tipo y estado (ambos opcionales). */
    @Transactional(readOnly = true)
    public List<Poliza> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscarPorTipoYEstado(tipo, estado);
    }

    @Transactional(readOnly = true)
    public Poliza obtener(Long id) {
        return polizaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la poliza con id " + id));
    }

    /** GET /polizas/{id}/riesgos */
    @Transactional(readOnly = true)
    public List<Riesgo> listarRiesgos(Long polizaId) {
        // Valida que la poliza exista antes de devolver sus riesgos.
        obtener(polizaId);
        return riesgoRepository.findByPolizaId(polizaId);
    }

    /**
     * POST /polizas/{id}/renovar
     * Incrementa canon y prima en +IPC y deja la poliza en estado RENOVADA.
     * No se puede renovar una poliza cancelada.
     */
    @Transactional
    public Poliza renovar(Long id) {
        Poliza poliza = obtener(id);

        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException(
                    "No se puede renovar una poliza cancelada (id " + id + ")");
        }

        BigDecimal factor = BigDecimal.ONE.add(IPC);
        BigDecimal nuevoCanon = poliza.getCanonMensual()
                .multiply(factor)
                .setScale(2, RoundingMode.HALF_UP);

        // La prima corresponde al canon mensual por el numero de meses de vigencia.
        BigDecimal nuevaPrima = nuevoCanon
                .multiply(BigDecimal.valueOf(poliza.getMesesVigencia()))
                .setScale(2, RoundingMode.HALF_UP);

        poliza.setCanonMensual(nuevoCanon);
        poliza.setPrima(nuevaPrima);
        poliza.setEstado(EstadoPoliza.RENOVADA);

        // Extiende la vigencia el mismo periodo inicial.
        LocalDate nuevoInicio = poliza.getVigenciaFin();
        poliza.setVigenciaInicio(nuevoInicio);
        poliza.setVigenciaFin(nuevoInicio.plusMonths(poliza.getMesesVigencia()));

        Poliza guardada = polizaRepository.save(poliza);
        coreIntegrationService.notificarEvento("RENOVACION", guardada.getId());
        return guardada;
    }

    /**
     * POST /polizas/{id}/cancelar
     * Cancela la poliza y, en cascada, todos sus riesgos activos.
     */
    @Transactional
    public Poliza cancelar(Long id) {
        Poliza poliza = obtener(id);

        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException(
                    "La poliza con id " + id + " ya se encuentra cancelada");
        }

        poliza.setEstado(EstadoPoliza.CANCELADA);
        poliza.getRiesgos().forEach(r -> r.setEstado(EstadoRiesgo.CANCELADO));

        Poliza guardada = polizaRepository.save(poliza);
        coreIntegrationService.notificarEvento("CANCELACION", guardada.getId());
        return guardada;
    }

    /**
     * POST /polizas/{id}/riesgos
     * Agrega un riesgo. Solo permitido para polizas COLECTIVAS.
     * (Una individual solo admite 1 riesgo y ya lo trae desde su creacion.)
     */
    @Transactional
    public Riesgo agregarRiesgo(Long polizaId, CrearRiesgoRequest request) {
        Poliza poliza = obtener(polizaId);

        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException(
                    "No se pueden agregar riesgos a una poliza cancelada (id " + polizaId + ")");
        }

        if (poliza.getTipo() != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException(
                    "Solo las polizas COLECTIVAS admiten agregar riesgos. La poliza "
                            + polizaId + " es " + poliza.getTipo());
        }

        Riesgo riesgo = Riesgo.builder()
                .descripcion(request.descripcion())
                .arrendatario(request.arrendatario())
                .estado(EstadoRiesgo.ACTIVO)
                .poliza(poliza)
                .build();

        // Persiste directamente el riesgo para obtener su id generado.
        Riesgo guardado = riesgoRepository.save(riesgo);

        coreIntegrationService.notificarEvento("ACTUALIZACION", polizaId);
        return guardado;
    }

    /**
     * POST /riesgos/{id}/cancelar
     * Cancela un riesgo puntual.
     */
    @Transactional
    public Riesgo cancelarRiesgo(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el riesgo con id " + riesgoId));

        if (riesgo.getEstado() == EstadoRiesgo.CANCELADO) {
            throw new ReglaNegocioException(
                    "El riesgo con id " + riesgoId + " ya se encuentra cancelado");
        }

        riesgo.setEstado(EstadoRiesgo.CANCELADO);
        Riesgo guardado = riesgoRepository.save(riesgo);

        coreIntegrationService.notificarEvento("ACTUALIZACION", riesgo.getPoliza().getId());
        return guardado;
    }
}
