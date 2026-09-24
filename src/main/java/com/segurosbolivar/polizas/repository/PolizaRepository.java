package com.segurosbolivar.polizas.repository;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolizaRepository extends JpaRepository<Poliza, Long> {

    /**
     * Lista polizas filtrando opcionalmente por tipo y/o estado.
     * Si un parametro llega en null, ese filtro se ignora.
     */
    @Query("""
            SELECT p FROM Poliza p
            WHERE (:tipo IS NULL OR p.tipo = :tipo)
              AND (:estado IS NULL OR p.estado = :estado)
            """)
    List<Poliza> buscarPorTipoYEstado(@Param("tipo") TipoPoliza tipo,
                                      @Param("estado") EstadoPoliza estado);
}
