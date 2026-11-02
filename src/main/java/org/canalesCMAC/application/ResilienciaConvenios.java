package org.canalesCMAC.application;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Institucion;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResilienciaConvenios {

    private static final Logger LOG = Logger.getLogger(ResilienciaConvenios.class);
    private static final String PREFIJO = "convenio.";

    private final Map<Institucion, Politica> politicas;

    @Inject
    public ResilienciaConvenios(Config config) {
        Map<Institucion, Politica> porInstitucion = new EnumMap<>(Institucion.class);
        for (Institucion institucion : Institucion.values()) {
            porInstitucion.put(institucion, new Politica(config, institucion));
        }
        this.politicas = porInstitucion;
        LOG.debugf("resiliencia por convenio cargada convenios=%d", politicas.size());
    }

    public void verificar(Institucion institucion) {
        politicas.get(institucion).circuito.verificar(institucion);
        politicas.get(institucion).limite.verificar(institucion);
    }

    public Permiso entrar(Institucion institucion) {
        Politica politica = politicas.get(institucion);
        politica.bulkhead.adquirir(institucion);
        return new Permiso(politica.bulkhead);
    }

    public void exito(Institucion institucion) {
        politicas.get(institucion).circuito.exito();
    }

    public void fallo(Institucion institucion) {
        politicas.get(institucion).circuito.fallo();
    }

    public void abortar(Institucion institucion) {
        politicas.get(institucion).circuito.abortar();
    }

    public String estado(Institucion institucion) {
        return politicas.get(institucion).circuito.estado();
    }

    public static final class Permiso implements AutoCloseable {

        private final Bulkhead bulkhead;

        private Permiso(Bulkhead bulkhead) {
            this.bulkhead = bulkhead;
        }

        @Override
        public void close() {
            bulkhead.liberar();
        }
    }

    private static final class Politica {

        private final Circuito circuito;
        private final Limitador limite;
        private final Bulkhead bulkhead;

        private Politica(Config config, Institucion institucion) {
            String base = PREFIJO + institucion.name() + ".";
            int volumen = config.getOptionalValue(base + "cb.requestVolume", Integer.class).orElse(20);
            double ratio = config.getOptionalValue(base + "cb.failureRatio", Double.class).orElse(0.5);
            long delay = config.getOptionalValue(base + "cb.delayMs", Long.class).orElse(5000L);
            int concurrentes = config.getOptionalValue(base + "bulkhead.maxConcurrent", Integer.class)
                .orElse(0);
            long espera = config.getOptionalValue(base + "bulkhead.timeoutMs", Long.class)
                .orElse(5000L);
            int porSegundo = config.getOptionalValue(base + "ratelimit.permitsPerSecond", Integer.class)
                .orElse(0);
            this.circuito = new Circuito(volumen, ratio, delay);
            this.limite = new Limitador(porSegundo);
            this.bulkhead = new Bulkhead(concurrentes, espera);
        }
    }

    private static final class Circuito {

        private final int volumen;
        private final double ratio;
        private final long delayMs;
        private int fallos;
        private int total;
        private long abiertoHasta;
        private boolean semiabierto;
        private boolean sondaEnCurso;

        private Circuito(int volumen, double ratio, long delayMs) {
            this.volumen = Math.max(1, volumen);
            this.ratio = ratio <= 0 ? 0.5 : Math.min(1, ratio);
            this.delayMs = Math.max(1, delayMs);
        }

        private synchronized void verificar(Institucion institucion) {
            long ahora = System.currentTimeMillis();
            if (abiertoHasta == 0) {
                return;
            }
            if (ahora < abiertoHasta) {
                throw new ProveedorException(
                    "Circuito abierto para " + institucion + "; reintente más tarde",
                    ProveedorException.SERVICE_UNAVAILABLE, false, null);
            }
            if (!semiabierto) {
                semiabierto = true;
                sondaEnCurso = true;
                return;
            }
            if (!sondaEnCurso) {
                sondaEnCurso = true;
                return;
            }
            throw new ProveedorException(
                "Circuito semiabierto para " + institucion + "; hay una prueba en curso",
                ProveedorException.SERVICE_UNAVAILABLE, false, null);
        }

        private synchronized void exito() {
            fallos = 0;
            total = 0;
            abiertoHasta = 0;
            semiabierto = false;
            sondaEnCurso = false;
        }

        private synchronized void abortar() {
            sondaEnCurso = false;
        }

        private synchronized void fallo() {
            fallos++;
            total++;
            long ahora = System.currentTimeMillis();
            if (semiabierto) {
                abiertoHasta = ahora + delayMs;
                semiabierto = false;
                sondaEnCurso = false;
                return;
            }
            if (total >= volumen && ((double) fallos / total) >= ratio) {
                abiertoHasta = ahora + delayMs;
            }
        }

        private synchronized String estado() {
            if (abiertoHasta == 0) {
                return "CERRADO";
            }
            return System.currentTimeMillis() < abiertoHasta ? "ABIERTO" : "SEMIABIERTO";
        }
    }

    private static final class Limitador {

        private final int permisosPorSegundo;
        private long ventana = -1;
        private int usados;

        private Limitador(int permisosPorSegundo) {
            this.permisosPorSegundo = permisosPorSegundo;
        }

        private synchronized void verificar(Institucion institucion) {
            if (permisosPorSegundo <= 0) {
                return;
            }
            long actual = System.currentTimeMillis() / 1000L;
            if (actual != ventana) {
                ventana = actual;
                usados = 0;
            }
            if (usados >= permisosPorSegundo) {
                throw new ProveedorException(
                    "Límite de peticiones excedido para " + institucion,
                    ProveedorException.TOO_MANY_REQUESTS, true, null);
            }
            usados++;
        }
    }

    private static final class Bulkhead {

        private final Semaphore semaforo;
        private final long esperaMs;

        private Bulkhead(int concurrentes, long esperaMs) {
            this.semaforo = concurrentes <= 0 ? null : new Semaphore(concurrentes);
            this.esperaMs = esperaMs;
        }

        private void adquirir(Institucion institucion) {
            if (semaforo == null) {
                return;
            }
            try {
                if (!semaforo.tryAcquire(esperaMs, TimeUnit.MILLISECONDS)) {
                    throw new ProveedorException(
                        "Capacidad concurrente agotada para " + institucion,
                        ProveedorException.SERVICE_UNAVAILABLE, true, null);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProveedorException(
                    "Espera de capacidad interrumpida para " + institucion,
                    ProveedorException.SERVICE_UNAVAILABLE, false, e);
            }
        }

        private void liberar() {
            if (semaforo != null) {
                semaforo.release();
            }
        }
    }
}
