package org.canalesCMAC.application.port;

public record PeticionSalida(String contenido, String ruta, String contentType) {

    public PeticionSalida(String contenido, String contentType) {
        this(contenido, "", contentType);
    }
}
