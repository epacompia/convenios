package org.canalesCMAC.adapter.out.soap;

import java.util.List;

public record NodoXml(String nombre, String texto, List<NodoXml> hijos) {

    public NodoXml hijo(String nombre) {
        for (NodoXml hijo : hijos) {
            if (hijo.nombre().equals(nombre)) {
                return hijo;
            }
        }
        return null;
    }

    public String textoDe(String nombre) {
        NodoXml hijo = hijo(nombre);
        return hijo == null ? null : hijo.texto();
    }

    public List<NodoXml> hijos(String nombre) {
        return hijos.stream().filter(hijo -> hijo.nombre().equals(nombre)).toList();
    }
}
