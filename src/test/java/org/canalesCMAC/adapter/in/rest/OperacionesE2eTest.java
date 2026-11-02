package org.canalesCMAC.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.canalesCMAC.infrastructure.ConveniosWireMockResource;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(ConveniosWireMockResource.class)
class OperacionesE2eTest {

    @Test
    void elseConsultaDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELSE\",\"operador\":\"CONSULTA_DEUDA\","
                + "\"datos\":{\"filtro\":{\"datoConsulta\":\"0010681504\"}}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("ELSE"))
            .body("respuesta.codigo", is("00"))
            .body("respuesta.mensaje", is("CORRECTO"))
            .body("respuesta.cliente.nombreCliente", is("JUAN PEREZ"))
            .body("respuesta.cliente.numeroSuministro", is("10010681504"))
            .body("respuesta.cliente.numeroDocumento", is("20123456789"))
            .body("respuesta.transaccion[0].identificadorTransaccion", is("TX-100"))
            .body("respuesta.transaccion[0].monto", is(23.10f))
            .body("respuesta.transaccion[0].fechaVencimientoRsp", is("2026-10-10"));
    }

    @Test
    void devuelveLaCabeceraRecibida() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELSE\",\"operador\":\"CONSULTA_DEUDA\","
                + "\"cabecera\":{\"idTransaccion\":\"20260218000123000042\",\"idSesion\":\"7\","
                + "\"codAgencia\":\"15\",\"codUsuario\":\"CAJERO01\",\"ip\":\"10.0.0.1\","
                + "\"fecha\":\"2026-02-18T10:19:47-05:00\",\"idCanal\":\"21\",\"idAplicacion\":\"56\"},"
                + "\"datos\":{\"filtro\":{\"datoConsulta\":\"0010681504\"}}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("cabecera.idTransaccion", is("20260218000123000042"))
            .body("cabecera.codAgencia", is("15"))
            .body("respuesta.codigo", is("00"));
    }

    @Test
    void elsePagoConDosTransaccionesDevuelveDosResultados() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELSE\",\"operador\":\"PAGO_DEUDA\",\"datos\":{"
                + "\"filtro\":{\"numeroComprobante\":\"2018001000000294383\","
                + "\"numeroSuministro\":\"10010681504\"},"
                + "\"transaccion\":[{\"identificadorTransaccion\":\"TX-1\",\"monto\":\"10.00\"},"
                + "{\"identificadorTransaccion\":\"TX-2\",\"monto\":\"20.00\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("respuesta.codigo", is("00"))
            .body("respuesta.transaccion.size()", is(2))
            .body("respuesta.transaccion[1].identificadorPago", is("20180425001000003"));
    }

    @Test
    void elsePagoDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELSE\",\"operador\":\"PAGO_DEUDA\",\"datos\":{"
                + "\"filtro\":{\"numeroComprobante\":\"2018001000000294383\","
                + "\"numeroSuministro\":\"10010681504\"},"
                + "\"transaccion\":[{\"identificadorTransaccion\":\"TX-100\",\"monto\":\"23.10\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("respuesta.codigo", is("00"))
            .body("respuesta.transaccion[0].identificadorPago", is("20180425001000003"))
            .body("respuesta.transaccion[0].numeroComprobante", is("2018001000000294383"))
            .body("respuesta.transaccion[0].monto", is(23.10f));
    }

    @Test
    void elseExtornoPagoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELSE\",\"operador\":\"EXTORNO_PAGO\",\"datos\":{"
                + "\"filtro\":{\"numeroComprobante\":\"2018001000000294383\","
                + "\"numeroSuministro\":\"10010681504\"},"
                + "\"transaccion\":[{\"identificadorTransaccion\":\"TX-100\","
                + "\"identificadorPago\":\"20180425001000003\",\"monto\":\"23.10\","
                + "\"motivoExtorno\":\"1\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("respuesta.codigo", is("00"))
            .body("respuesta.transaccion[0].identificadorPago", is("201804250010000037"))
            .body("respuesta.transaccion[0].monto", is(23.10f));
    }

    @Test
    void electroUcayaliConsultaDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELECTRO_UCAYALI\",\"operador\":\"CONSULTA_DEUDA\","
                + "\"datos\":{\"filtro\":{\"numeroSuministro\":\"800006812\","
                + "\"traceConsulta\":\"TRC-001\"},"
                + "\"transaccion\":[{\"fechaOperacion\":\"2026-10-14T10:30:00\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("ELECTRO_UCAYALI"))
            .body("respuesta.codigo", is("00"))
            .body("respuesta.cliente.numeroSuministro", is("800006812"))
            .body("respuesta.cliente.nombreCliente", is("JUAN PEREZ"))
            .body("respuesta.transaccion[0].deudas[0].numeroComprobante", is("201800100000"))
            .body("respuesta.transaccion[0].deudas[0].monto", is(19423.00f))
            .body("respuesta.transaccion[0].deudas[0].fechaVencimiento", is("2026-10-10"));
    }

    @Test
    void electroUcayaliPagoDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELECTRO_UCAYALI\",\"operador\":\"PAGO_DEUDA\",\"datos\":{"
                + "\"filtro\":{\"numeroSuministro\":\"800006812\","
                + "\"numeroComprobante\":\"201800100000\",\"traceConsulta\":\"TRC-001\"},"
                + "\"transaccion\":[{\"monto\":\"19423.00\","
                + "\"fechaOperacion\":\"2026-10-14T10:30:00\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("respuesta.codigo", is("00"))
            .body("respuesta.transaccion[0].numeroOperacion", is("654321"))
            .body("respuesta.cliente.numeroSuministro", is("800006812"));
    }

    @Test
    void sealConsultaDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"SEAL\",\"operador\":\"CONSULTA_DEUDA\","
                + "\"datos\":{\"filtro\":{\"contrato\":\"98787\"},"
                + "\"transaccion\":[{\"identificadorTransaccion\":\"369571\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("SEAL"))
            .body("respuesta.codigo", is("00"))
            .body("respuesta.mensaje", is("TRANSACCION CORRECTA"))
            .body("respuesta.cliente.nombreCliente", is("DELGADO DE VIZCARRA NORA"))
            .body("respuesta.cliente.direccionCliente", is("CALLE NICOLAS DE PIEROLA NRO 117-2"))
            .body("respuesta.cliente.numeroSuministro", is("98787"))
            .body("respuesta.transaccion[0].monto", is(1500.00f));
    }

    @Test
    void electroUcayaliErrorDelProveedorDevuelve502() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELECTRO_UCAYALI\",\"operador\":\"EXTORNO_PAGO\",\"datos\":{"
                + "\"filtro\":{\"numeroSuministro\":\"800006812\","
                + "\"numeroComprobante\":\"201800100000\"},"
                + "\"transaccion\":[{\"monto\":\"19423.00\","
                + "\"fechaOperacion\":\"2026-10-14T10:30:00\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(502)
            .body("respuesta.codigo", is("ER"));
    }

    @Test
    void electroUcayaliExtornoAutomaticoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELECTRO_UCAYALI\",\"operador\":\"EXTORNO_AUTO\",\"datos\":{"
                + "\"filtro\":{\"numeroSuministro\":\"800006812\","
                + "\"numeroComprobante\":\"201800100000\"},"
                + "\"transaccion\":[{\"monto\":\"19423.00\","
                + "\"identificadorPago\":\"TRP-999\","
                + "\"fechaOperacion\":\"2026-10-14T10:30:00\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("respuesta.codigo", is("00"));
    }

    @Test
    void claroPagoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"CLARO\",\"operador\":\"PAGO\",\"datos\":{"
                + "\"transaccion\":[{\"monto\":\"10.00\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("CLARO"))
            .body("respuesta.codigo", is("00"))
            .body("respuesta.transaccion[0].codigoAprobacion", is("AUTH-123"));
    }

    @Test
    void universidadCuscoConsultaDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"UNIVCUSCO\",\"operador\":\"CONSULTA_DEUDA\",\"datos\":{"
                + "\"filtro\":{\"numeroReferencialDeuda\":\"123456\"}}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("UNIVCUSCO"))
            .body("respuesta.codigo", is("00"))
            .body("respuesta.cliente.nombreCliente", is("ALUMNO UAC"))
            .body("respuesta.transaccion[0].numeroComprobante", is("123456"))
            .body("respuesta.transaccion[0].deudas[0].numeroComprobante", is("T-012-145230"))
            .body("respuesta.transaccion[0].deudas[0].monto", is(150.50f));
    }

    @Test
    void yaGanasteConsultaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"YAGANASTE\",\"operador\":\"CONSULTA\",\"datos\":{"
                + "\"filtro\":{\"codigoProducto\":\"P1\"},"
                + "\"transaccion\":[{\"tpv\":\"1\",\"claveTpv\":\"clave\","
                + "\"referenciaOperacion\":\"REF-1\"}]}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("YAGANASTE"))
            .body("respuesta.codigo", is("00"))
            .body("respuesta.transaccion[0].referenciaProveedor", is("REF-PROV-1"))
            .body("respuesta.transaccion[0].referenciaOperador", is("REF-OP-1"));
    }

    @Test
    void municipalidadCuscoRecuperarOrdenPagoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"MUNICIPALIDAD_CUSCO\",\"operador\":\"RECUPERAR_ORDEN_PAGO\","
                + "\"datos\":{\"filtro\":{\"numeroOrdenPago\":\"123\"}}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("MUNICIPALIDAD_CUSCO"))
            .body("respuesta.codigo", is("00"))
            .body("respuesta.mensaje", is("OK"));
    }

    @Test
    void aceptaAliasOrdenPagoDelContratoAnterior() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"MUNICIPALIDAD_CUSCO\",\"operador\":\"RECUPERAR_ORDEN_PAGO\","
                + "\"datos\":{\"filtro\":{\"ordenpago\":\"123\"}}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("respuesta.codigo", is("00"));
    }

    @Test
    void payToPeruConsultaPagoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"PAYTOPERU\",\"operador\":\"CONSULTA_PAGO\",\"datos\":{"
                + "\"filtro\":{\"codigoPago\":123,\"numeroDocumento\":\"12345678\"}}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("PAYTOPERU"))
            .body("respuesta.codigo", is("00"))
            .body("respuesta.cliente.nombreCliente", is("JUAN PEREZ"));
    }
}
