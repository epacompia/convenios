package org.canalesCMAC.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.canalesCMAC.application.RecaudacionUseCase;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.Cabecera;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OperacionesResourceTest {

    private static final String ENDPOINT = "/api/v1/operaciones";

    @Test
    void rechazaOperadorNoSoportadoParaLaInstitucion() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "institucion": "ELECTRO_UCAYALI",
                    "operador": "CONSULTA_PAGO",
                    "datos": {}
                }
                """)
        .when()
            .post(ENDPOINT)
        .then()
            .statusCode(400)
            .body("respuesta.codigo", is("ER"));
    }

    @Test
    void rechazaPagoConCamposObligatoriosAusentes() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "institucion": "ELSE",
                    "operador": "PAGO_DEUDA",
                    "datos": {}
                }
                """)
        .when()
            .post(ENDPOINT)
        .then()
            .statusCode(400)
            .body("respuesta.codigo", is("ER"));
    }

    @Test
    void rechazaCabeceraConIdTransaccionInvalido() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "institucion": "ELSE",
                    "operador": "CONSULTA_DEUDA",
                    "cabecera": {
                        "idTransaccion": "123"
                    },
                    "datos": {
                        "filtro": {
                            "datoConsulta": "0010681504"
                        }
                    }
                }
                """)
        .when()
            .post(ENDPOINT)
        .then()
            .statusCode(400)
            .body("respuesta.codigo", is("ER"));
    }

    @Test
    void rechazaParametrosOperacionVacios() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "institucion": "WESTERN_UNION",
                    "operador": "CONSULTAR_TRANSACCION",
                    "datos": {
                        "filtro": {
                            "idProducto": "P1",
                            "parametrosOperacion": []
                        }
                    }
                }
                """)
        .when()
            .post(ENDPOINT)
        .then()
            .statusCode(400)
            .body("respuesta.codigo", is("ER"));
    }

    @Test
    void rechazaSolicitudJsonMalformada() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "institucion": "ELSE",
                    "operador":
                """)
        .when()
            .post(ENDPOINT)
        .then()
            .statusCode(400);
    }

    @Test
    void rechazaSolicitudSinInstitucion() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "operador": "CONSULTA_DEUDA",
                    "datos": {}
                }
                """)
        .when()
            .post(ENDPOINT)
        .then()
            .statusCode(400);
    }

    @Test
    void ejecutaCasoDeUsoCuandoLaSolicitudEsProcesable() throws Exception {

        // Arrange
        OperacionesResource resource = new OperacionesResource();

        AtomicBoolean casoDeUsoEjecutado = new AtomicBoolean(false);

        RecaudacionResponse respuestaEsperada = RecaudacionResponse.error(
            Institucion.ELSE,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            "Respuesta de prueba"
        );

        RecaudacionUseCase useCaseControlado = new RecaudacionUseCase() {

            @Override
            public RecaudacionResponse procesar(RecaudacionRequest solicitud) {

                assertNotNull(solicitud);

                casoDeUsoEjecutado.set(true);

                return respuestaEsperada;
            }
        };

        asignarCampo(resource, "useCase", useCaseControlado);

        SolicitudContexto contextoControlado = new SolicitudContexto();

        asignarCampo(resource, "contexto", contextoControlado);

        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.ELSE,
            Operador.CONSULTA_DEUDA,
            Map.of()
        );

        // Act
        RecaudacionResponse respuesta = resource.procesar(solicitud);

        // Assert
        assertNotNull(respuesta);
        assertSame(respuestaEsperada, respuesta);
        assertTrue(casoDeUsoEjecutado.get(),
            "El método useCase.procesar() debería haberse ejecutado");
    }

    private void asignarCampo(
        Object instancia,
        String nombreCampo,
        Object valor
    ) throws Exception {

        Field campo = instancia.getClass().getDeclaredField(nombreCampo);
        campo.setAccessible(true);
        campo.set(instancia, valor);
    }
}