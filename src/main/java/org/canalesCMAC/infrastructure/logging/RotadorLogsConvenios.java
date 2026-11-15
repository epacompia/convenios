package org.canalesCMAC.infrastructure.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Normaliza el nombre de los historicos que genera la rotacion horaria de Quarkus:
 * convenios-caja-cusco.log-yyyy-MM-dd-HH -> convenios-caja-cusco-yyyy-MM-dd-HH.log
 */
@Startup
@ApplicationScoped
public class RotadorLogsConvenios {

    private static final Logger LOG = Logger.getLogger(RotadorLogsConvenios.class);
    private static final Pattern ROTADO = Pattern.compile(
        "^convenios-caja-cusco\\.log-(\\d{4}-\\d{2}-\\d{2}-\\d{2})$");

    @ConfigProperty(name = "convenios.logs.dir", defaultValue = "logs")
    String directorio;

    @ConfigProperty(name = "convenios.logs.rotation.enabled", defaultValue = "true")
    boolean habilitado;

    private ScheduledExecutorService ejecutor;

    @PostConstruct
    void iniciar() {
        if (!habilitado) {
            return;
        }
        ejecutor = Executors.newSingleThreadScheduledExecutor(tarea -> {
            Thread hilo = new Thread(tarea, "rotador-logs-convenios");
            hilo.setDaemon(true);
            return hilo;
        });
        ejecutor.scheduleWithFixedDelay(this::renombrar, 0, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    void detener() {
        if (ejecutor != null) {
            ejecutor.shutdownNow();
        }
    }

    void renombrar() {
        Path carpeta = Paths.get(directorio);
        if (!Files.isDirectory(carpeta)) {
            return;
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            archivos.filter(Files::isRegularFile).forEach(this::renombrar);
        } catch (IOException e) {
            LOG.warnf("no se pudo revisar la carpeta de logs %s: %s", carpeta, e.getMessage());
        }
    }

    private void renombrar(Path archivo) {
        Matcher coincidencia = ROTADO.matcher(archivo.getFileName().toString());
        if (!coincidencia.matches()) {
            return;
        }
        String destinoNombre = "convenios-caja-cusco-" + coincidencia.group(1) + ".log";
        Path destino = archivo.resolveSibling(destinoNombre);
        try {
            Files.move(archivo, destino, StandardCopyOption.REPLACE_EXISTING);
            LOG.infof("log historico versionado archivo=%s", destinoNombre);
        } catch (IOException e) {
            LOG.warnf("no se pudo renombrar el log %s: %s", archivo, e.getMessage());
        }
    }
}
