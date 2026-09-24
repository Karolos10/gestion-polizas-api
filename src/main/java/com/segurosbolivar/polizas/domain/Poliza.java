package com.segurosbolivar.polizas.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Poliza de arrendamiento de inmuebles.
 *
 * <p>Reglas de dominio relevantes:</p>
 * <ul>
 *   <li>La prima = canon mensual * numero de meses de la vigencia.</li>
 *   <li>Una poliza individual solo puede tener un riesgo activo.</li>
 *   <li>Una poliza colectiva puede tener uno o muchos riesgos.</li>
 *   <li>La renovacion extiende la vigencia el mismo periodo y ajusta el canon por IPC.</li>
 * </ul>
 */
@Entity
@Table(name = "polizas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPoliza estado;

    /** Datos de las partes involucradas. */
    @Column(length = 150)
    private String tomador;

    @Column(length = 150)
    private String asegurado;

    @Column(length = 150)
    private String beneficiario;

    /** Periodo de vigencia. */
    @Column(nullable = false)
    private LocalDate vigenciaInicio;

    @Column(nullable = false)
    private LocalDate vigenciaFin;

    /** Numero de meses de la vigencia (usado para calcular la prima). */
    @Column(nullable = false)
    private Integer mesesVigencia;

    /** Valor del canon mensual de arrendamiento. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal canonMensual;

    /** Prima = canon mensual * meses de vigencia. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal prima;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Riesgo> riesgos = new ArrayList<>();

    /** Helper para mantener la relacion bidireccional consistente. */
    public void agregarRiesgo(Riesgo riesgo) {
        riesgo.setPoliza(this);
        this.riesgos.add(riesgo);
    }

    /** Cantidad de riesgos que aun estan activos. */
    public long cantidadRiesgosActivos() {
        return riesgos.stream()
                .filter(r -> r.getEstado() == EstadoRiesgo.ACTIVO)
                .count();
    }
}
