package org.canalesCMAC.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class OperacionesResourceTest {

    @Test
    void rechazaOperadorNoSoportadoParaLaInstitucion() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELECTRO_UCAYALI\",\"operador\":\"CONSULTA_PAGO\",\"datos\":{}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(400)
            .body("respuesta.codigo", is("ER"));
    }

    @Test
    void rechazaPagoConCamposObligatoriosAusentes() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"institucion\":\"ELSE\",\"operador\":\"PAGO_DEUDA\",\"datos\":{}}")
            .when().post("/api/v1/operaciones")
            .then()
            .statusCode(400)
            .body("respuesta.codigo", is("ER"));
    }
}
