package org.canalesCMAC.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.infrastructure.persistence.Convenio;
import org.canalesCMAC.infrastructure.persistence.ConvenioRepository;
import org.canalesCMAC.infrastructure.persistence.OperacionRecaudacion;
import org.canalesCMAC.infrastructure.persistence.OperacionRecaudacionRepository;
import org.canalesCMAC.infrastructure.persistence.RecaudacionConciliacion;
import org.canalesCMAC.infrastructure.persistence.RecaudacionConciliacionRepository;
import org.canalesCMAC.infrastructure.persistence.RecaudacionTransaccion;
import org.canalesCMAC.infrastructure.persistence.RecaudacionTransaccionRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ConciliacionService {

    @Inject
    OperacionRecaudacionRepository operaciones;

    @Inject
    RecaudacionTransaccionRepository transacciones;

    @Inject
    ConvenioRepository convenios;

    @Inject
    RecaudacionConciliacionRepository conciliaciones;

    @Transactional
    public RecaudacionConciliacion cerrar(LocalDate fecha, Institucion institucion) {
        Convenio convenio = convenios.porCodigo(institucion.name())
            .orElseThrow(() -> new IllegalArgumentException(
                "Convenio no encontrado: " + institucion));

        OffsetDateTime inicio = fecha.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime fin = inicio.plusDays(1);
        List<OperacionRecaudacion> operacionesDia = operaciones.find(
            "institucion = ?1 and fechaRegistro >= ?2 and fechaRegistro < ?3",
            institucion.name(), inicio, fin).list();

        long total = operacionesDia.size();
        long aprobadas = operacionesDia.stream()
            .filter(operacion -> "OK".equals(operacion.estado))
            .count();
        long rechazadas = operacionesDia.stream()
            .filter(operacion -> "ERROR".equals(operacion.estado))
            .count();

        BigDecimal montoTotal = BigDecimal.ZERO;
        for (OperacionRecaudacion operacion : operacionesDia) {
            for (RecaudacionTransaccion transaccion : transacciones.porOperacion(operacion.id)) {
                if (transaccion.monto != null) {
                    montoTotal = montoTotal.add(transaccion.monto);
                }
            }
        }

        RecaudacionConciliacion conciliacion = conciliaciones
            .porFechaYConvenio(fecha, convenio.id)
            .orElseGet(RecaudacionConciliacion::new);
        conciliacion.fechaConciliacion = fecha;
        conciliacion.convenioId = convenio.id;
        conciliacion.totalOperaciones = total;
        conciliacion.totalAprobadas = aprobadas;
        conciliacion.totalRechazadas = rechazadas;
        conciliacion.montoTotal = montoTotal;
        conciliacion.moneda = "PEN";
        conciliacion.estado = rechazadas == 0 ? "CUADRADO" : "DIFERENCIA";
        if (conciliacion.id == null) {
            conciliaciones.persist(conciliacion);
        }
        return conciliacion;
    }
}
