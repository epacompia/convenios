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
                + "\"datos\":{\"datoConsulta\":\"0010681504\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("ELSE"))
            .body("codigo", is("00"))
            .body("mensaje", is("CORRECTO"))
            .body("datos.identificadorTransaccion", is("TX-100"))
            .body("datos.nombreCliente", is("JUAN PEREZ"))
            .body("datos.numeroSuministro", is("10010681504"))
            .body("datos.numeroDocumento", is("20123456789"))
            .body("datos.montoPagar", is(23.10f))
            .body("datos.fechaVencimientoRsp", is("2026-10-10"));
    }

    @Test
    void elsePagoDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELSE\",\"operador\":\"PAGO_DEUDA\",\"datos\":{"
                + "\"identificadorTransaccion\":\"TX-100\",\"numeroComprobante\":\"2018001000000294383\","
                + "\"numeroSuministro\":\"10010681504\",\"monto\":\"23.10\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("codigo", is("00"))
            .body("datos.identificadorPago", is("20180425001000003"))
            .body("datos.numeroComprobante", is("2018001000000294383"))
            .body("datos.monto", is(23.10f));
    }

    @Test
    void elseExtornoPagoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELSE\",\"operador\":\"EXTORNO_PAGO\",\"datos\":{"
                + "\"identificadorTransaccion\":\"TX-100\",\"identificadorPago\":\"20180425001000003\","
                + "\"numeroComprobante\":\"2018001000000294383\",\"numeroSuministro\":\"10010681504\","
                + "\"monto\":\"23.10\",\"motivoExtorno\":\"1\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("codigo", is("00"))
            .body("datos.identificadorPago", is("201804250010000037"))
            .body("datos.monto", is(23.10f));
    }

    @Test
    void electroUcayaliConsultaDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELECTRO_UCAYALI\",\"operador\":\"CONSULTA_DEUDA\","
                + "\"datos\":{\"numeroSuministro\":\"800006812\",\"traceConsulta\":\"TRC-001\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("ELECTRO_UCAYALI"))
            .body("codigo", is("00"))
            .body("datos.numeroSuministro", is("800006812"))
            .body("datos.nombreCliente", is("JUAN PEREZ"))
            .body("datos.listaDeudas[0].numeroComprobante", is("201800100000"))
            .body("datos.listaDeudas[0].monto", is(19423.00f))
            .body("datos.listaDeudas[0].fechaVencimiento", is("2026-10-10"));
    }

    @Test
    void electroUcayaliPagoDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELECTRO_UCAYALI\",\"operador\":\"PAGO_DEUDA\",\"datos\":{"
                + "\"numeroSuministro\":\"800006812\",\"numeroComprobante\":\"201800100000\","
                + "\"monto\":\"19423.00\",\"traceConsulta\":\"TRC-001\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("codigo", is("00"))
            .body("datos.numOperacion", is("654321"))
            .body("datos.numeroSuministro", is("800006812"));
    }

    @Test
    void sealConsultaDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"SEAL\",\"operador\":\"CONSULTA_DEUDA\","
                + "\"datos\":{\"contrato\":\"98787\",\"identificadorTransaccion\":\"369571\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("SEAL"))
            .body("codigo", is("00"))
            .body("mensaje", is("TRANSACCION CORRECTA"))
            .body("datos.contrato", is("98787"))
            .body("datos.nombreCliente", is("DELGADO DE VIZCARRA NORA"))
            .body("datos.direccionCliente", is("CALLE NICOLAS DE PIEROLA NRO 117-2"))
            .body("datos.montoPagar", is(1500.00f));
    }

    @Test
    void electroUcayaliErrorDelProveedorDevuelve502() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELECTRO_UCAYALI\",\"operador\":\"EXTORNO_PAGO\",\"datos\":{"
                + "\"numeroSuministro\":\"800006812\",\"numeroComprobante\":\"201800100000\","
                + "\"monto\":\"19423.00\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(502)
            .body("codigo", is("ER"));
    }

    @Test
    void electroUcayaliExtornoAutomaticoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELECTRO_UCAYALI\",\"operador\":\"EXTORNO_AUTO\",\"datos\":{"
                + "\"numeroSuministro\":\"800006812\",\"numeroComprobante\":\"201800100000\","
                + "\"monto\":\"19423.00\",\"identificadorPago\":\"TRP-999\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("codigo", is("00"));
    }

    @Test
    void claroPagoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"CLARO\",\"operador\":\"PAGO\",\"datos\":{\"monto\":\"10.00\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("CLARO"))
            .body("codigo", is("00"));
    }

    @Test
    void universidadCuscoConsultaDeudaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"UNIVCUSCO\",\"operador\":\"CONSULTA_DEUDA\",\"datos\":{"
                + "\"numeroReferencialDeuda\":\"123456\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("UNIVCUSCO"))
            .body("codigo", is("00"));
    }

    @Test
    void yaGanasteConsultaFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"YAGANASTE\",\"operador\":\"CONSULTA\",\"datos\":{"
                + "\"tpv\":\"1\",\"referenciaOperacion\":\"REF-1\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("YAGANASTE"))
            .body("codigo", is("00"));
    }

    @Test
    void municipalidadCuscoRecuperarOrdenPagoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"MUNICIPALIDAD_CUSCO\",\"operador\":\"RECUPERAR_ORDEN_PAGO\",\"datos\":{"
                + "\"ordenpago\":\"123\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("MUNICIPALIDAD_CUSCO"))
            .body("codigo", is("00"))
            .body("mensaje", is("OK"));
    }

    @Test
    void payToPeruConsultaPagoFlujoCompleto() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"PAYTOPERU\",\"operador\":\"CONSULTA_PAGO\",\"datos\":{"
                + "\"ncodigo_pago\":123,\"cnro_documento\":\"12345678\"}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(200)
            .body("institucion", is("PAYTOPERU"))
            .body("codigo", is("00"))
            .body("datos.cnombres", is("JUAN"));
    }
}