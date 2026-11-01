package org.canalesCMAC.adapter.out.paytoperu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Datos;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import org.junit.jupiter.api.Test;

class PayToPeruCodecTest {

    private final PayToPeruCodec codec = new PayToPeruCodec();

    @Test
    void interpretaConsultaDeTransaccionesComoLista() {
        String json = "[{\"ncodigo_pago\":100200,\"ccanal\":\"01\","
            + "\"cubicacion_canal\":\"Av. de la Cultura 1624\",\"nimporte\":55.00,"
            + "\"nmoneda\":1,\"dfecha\":\"21-06-2019 13:42:05\",\"cobservacion\":\"---\"}]";

        RecaudacionResponse respuesta = codec.interpretar(new RespuestaCruda(200, json),
            new RecaudacionRequest(Institucion.PAYTOPERU, Operador.CONSULTA_TRANSACCIONES,
                Cabecera.generar(), Datos.vacio()));

        assertEquals("00", respuesta.codigo());
        assertEquals(1, respuesta.transacciones().size());
        Map<String, Object> transaccion = respuesta.transacciones().get(0);
        assertEquals("100200", transaccion.get("numeroOperacion").toString());
        assertEquals("01", transaccion.get("canal"));
        assertEquals("---", transaccion.get("observacion"));
    }

    @Test
    void construyeNotificacionConCamposDelContrato() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.PAYTOPERU, Operador.NOTIFICACION_PAGO, Cabecera.generar(),
            new Datos(Map.of("codigoPago", 100200), Map.of(), List.of(Map.of(
                "canal", "01",
                "ubicacionCanal", "Av. de la Cultura 1624",
                "monto", 55.00,
                "moneda", 1,
                "fechaOperacion", "21-06-2019 13:42:05"))));

        var salida = codec.construir(peticion);

        assertEquals("/notificacion_pago", salida.ruta());
        assertEquals(true, salida.contenido().contains("\"ccanal\":\"01\""));
        assertEquals(true, salida.contenido().contains("\"cubicacion_canal\":\"Av. de la Cultura 1624\""));
        assertEquals(true, salida.contenido().contains("\"nimporte\":55.0"));
    }
}
