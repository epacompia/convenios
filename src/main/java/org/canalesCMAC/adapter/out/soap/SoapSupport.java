package org.canalesCMAC.adapter.out.soap;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public final class SoapSupport {

    private static final String ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String PREFIJO_SOAP = "soap";
    private static final String PREFIJO_ELEMENTO = "els";

    private SoapSupport() {
    }

    public static String sobre(String namespace, String operacion, Map<String, String> parametros) {
        try {
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            StringWriter salida = new StringWriter();
            XMLStreamWriter writer = factory.createXMLStreamWriter(salida);
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement(PREFIJO_SOAP, "Envelope", ENVELOPE_NS);
            writer.writeNamespace(PREFIJO_SOAP, ENVELOPE_NS);
            writer.writeNamespace(PREFIJO_ELEMENTO, namespace);
            writer.writeStartElement(PREFIJO_SOAP, "Body", ENVELOPE_NS);
            writer.writeStartElement(PREFIJO_ELEMENTO, operacion, namespace);
            for (Map.Entry<String, String> campo : parametros.entrySet()) {
                writer.writeStartElement(PREFIJO_ELEMENTO, campo.getKey(), namespace);
                writer.writeCharacters(campo.getValue() == null ? "" : campo.getValue());
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
            return salida.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo construir el sobre SOAP", e);
        }
    }

    public static NodoXml cuerpo(String xml) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT && "Body".equals(reader.getLocalName())) {
                    break;
                }
            }
            while (reader.hasNext()) {
                int evento = reader.next();
                if (evento == XMLStreamConstants.START_ELEMENT) {
                    return nodo(reader);
                }
                if (evento == XMLStreamConstants.END_ELEMENT) {
                    break;
                }
            }
            throw new IllegalArgumentException("SOAP sin operación de respuesta");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Respuesta SOAP no válida: " + e.getMessage(), e);
        }
    }

    private static NodoXml nodo(XMLStreamReader reader) throws Exception {
        String nombre = reader.getLocalName();
        StringBuilder texto = new StringBuilder();
        List<NodoXml> hijos = new ArrayList<>();
        while (reader.hasNext()) {
            int evento = reader.next();
            if (evento == XMLStreamConstants.START_ELEMENT) {
                hijos.add(nodo(reader));
            } else if (evento == XMLStreamConstants.CHARACTERS || evento == XMLStreamConstants.CDATA) {
                texto.append(reader.getText());
            } else if (evento == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return new NodoXml(nombre, texto.toString(), List.copyOf(hijos));
    }
}
