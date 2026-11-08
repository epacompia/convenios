package org.canalesCMAC.adapter.out.paytoperu;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

abstract class AbstractPayToPeruCodec implements MapeadorOperacion{
    private static final String CONTENT_TYPE = "application/json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected abstract String rutaBase();

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        String operacion = peticion.operador().name();
        Map<String, Object> request = new LinkedHashMap<>();
        String ruta;
        if("CONSULTA_PAGO".equals(operacion)){
            poner(request, "ncodigo_pago", peticion, Canonico.CODIGO_PAGO);
            poner(request, "cnro_documento", peticion, Canonico.DOCUMENTO_CLIENTE);
            ruta = rutaBase() + "/consulta_pago";
        } else if("NOTIFICACION_PAGO".equals(operacion)){
            poner(request,"ncodigo_pago", peticion, Canonico.CODIGO_PAGO);
            poner(request,"ccanal", peticion, Canonico.CANAL);
            poner(request,"cubicacion_canal", peticion, Canonico.UBICACION_CANAL);
            poner(request,"nimporte", peticion, Canonico.MONTO);
            poner(request,"nmoneda", peticion, Canonico.MONEDA);
            poner(request,"dfecha", peticion, Canonico.FECHA_OPERACION);
            ruta = rutaBase() + "/notificacion_pago";
        } else if("CONFIRMACION_PAGO".equals(operacion)){
            poner(request,"ncodigo_pago", peticion, Canonico.CODIGO_PAGO);
            ruta = rutaBase() + "/confirmacion_pago";
        } else if("CONSULTA_TRANSACCIONES".equals(operacion)){
            poner(request,"dfecha", peticion, Canonico.FECHA_OPERACION);
            ruta = rutaBase() + "/consulta_transacciones";
        } else {
            throw new OperadorNoSoportadoException(
                "Operador " + peticion.operador() + " no soportado por " + institucion());            
        }

        try {
            Log.info("::::" + objectMapper.writeValueAsString(request) );
            return new PeticionSalida(objectMapper.writeValueAsString(request), ruta, CONTENT_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error construyendo request de " + institucion(), e);
        }
    }

    @Override
    public RecaudacionResponse interpretar(
        RespuestaCruda respuesta, RecaudacionRequest peticion) {
        String codigo = "00";
        String mensaje = "";
        Map<String, Object> cliente = Mapas.nuevo();
        Map<String, Object> transaccion = Mapas.nuevo();
        try {
            JsonNode root = objectMapper.readTree(respuesta.cuerpo()); 
            
            if (root.isArray()) {
                for (JsonNode item : root) {            
                    Mapas.poner(transaccion, Canonico.CODIGO_PAGO, texto(item, "ncodigo_pago"));
                    Mapas.poner(transaccion, Canonico.CANAL, texto(item, "ccanal"));
                    Mapas.poner(transaccion, Canonico.UBICACION_CANAL, texto(item, "cubicacion_canal"));
                    Mapas.poner(transaccion, Canonico.MONTO, texto(item, "nimporte"));
                    Mapas.poner(transaccion, Canonico.MONEDA, texto(item, "nmoneda"));
                    Mapas.poner(transaccion, Canonico.FECHA_OPERACION, texto(item, "dfecha"));
                    Mapas.poner(transaccion, Canonico.OBSERVACION, texto(item, "cobservacion"));                  
                }
            }else{
                Mapas.poner(transaccion, Canonico.CODIGO_PAGO, texto(root, "ncodigo_pago"));
                Mapas.poner(cliente, Canonico.DOCUMENTO_CLIENTE, texto(root, "cnro_documento"));
                Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, obtenerNombreCliente(root, peticion.operador().name()));

                Mapas.poner(cliente, Canonico.EMAIL, texto(root, "cemail"));
                Mapas.poner(transaccion, Canonico.MONTO, texto(root, "nimporte"));
                Mapas.poner(transaccion, Canonico.MONEDA, texto(root, "nmoneda"));
                Mapas.poner(transaccion, Canonico.CODIGO_CONCEPTO, texto(root, "cconcepto"));
		
            }           
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta inválida de " + institucion(), e);
        }

        return RecaudacionResponse.de(institucion(), peticion.operador(), peticion.cabecera(),
            codigo, mensaje, cliente,
            transaccion.isEmpty() ? List.of() : List.of(transaccion));
    }
    
    private void poner(Map<String, Object> request, String nombre,
                       RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor != null && !valor.toString().isBlank()) {
            request.put(nombre, valor);
        }
    }
    private static String texto(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) && !nodo.get(campo).asText().isBlank()
            ? nodo.get(campo).asText().trim()
            : null;
    }

    private static String obtenerNombreCliente(JsonNode nodo, String operador) {
        return "CONSULTA_PAGO".equals(operador) 
             ?  texto(nodo, "cnombres") + " " + texto(nodo, "capellidos")
             : null;
    }
}
