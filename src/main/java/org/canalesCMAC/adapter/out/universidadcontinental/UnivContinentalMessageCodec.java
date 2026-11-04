package org.canalesCMAC.adapter.out.universidadcontinental;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.canalesCMAC.domain.model.RecaudacionResponse.Respuesta;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

@ApplicationScoped
public class UnivContinentalMessageCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(UnivContinentalMessageCodec.class);
    private static final String CONTENT_TYPE = "application/json";

    @Override
    public Institucion institucion() {
        return Institucion.UNIVCONTINENTAL;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {

        LOG.debugf("construyendo request Pago Idiomas operador=%s",peticion.operador());

        try {
            return switch (peticion.operador()) {
                case UNIVERSIDAD_CONSULTA_DEUDA ->
                    new PeticionSalida(ConsultaDeudaMapper.generarPeticion(peticion),
                        "", CONTENT_TYPE);
                case UNIVERSIDAD_PAGO_DEUDA ->
                    new PeticionSalida(PagoMapper.generarPeticion(peticion),
                        "", CONTENT_TYPE);
                case UNIVERSIDAD_EXTORNO_DEUDA ->
                    new PeticionSalida(ExtornoMapper.generarPeticion(peticion),
                        "", CONTENT_TYPE);
                case CENTRO_IDIOMAS_CONSULTA_DEUDA ->
                    new PeticionSalida(ConsultaDeudaMapper.generarPeticion(peticion),
                        "", CONTENT_TYPE);
                case CENTRO_IDIOMAS_PAGO_DEUDA ->
                    new PeticionSalida(PagoMapper.generarPeticion(peticion),
                        "", CONTENT_TYPE);
                case CENTRO_IDIOMAS_EXTORNO_DEUDA ->
                    new PeticionSalida(ExtornoMapper.generarPeticion(peticion),
                        "", CONTENT_TYPE);
                default -> throw new IllegalStateException("No se pudo procesar la solicitud");
            };

        } catch (JsonProcessingException e) {

            LOG.error("No se pudo construir request Pago Idiomas",e);
            throw new IllegalStateException("Error construyendo request Pago Idiomas",e);
        }
    }


    @Override
    public RecaudacionResponse interpretar(
        RespuestaCruda respuesta,
        RecaudacionRequest peticion) {

        try {

            Respuesta payloadRespuesta = 
                switch (peticion.operador()) {
                    case UNIVERSIDAD_CONSULTA_DEUDA -> 
                        ConsultaDeudaMapper.generarRespuesta(respuesta);
                    case UNIVERSIDAD_PAGO_DEUDA ->
                        PagoMapper.generarRespuesta(respuesta);
                    case UNIVERSIDAD_EXTORNO_DEUDA ->
                        ExtornoMapper.generarRespuesta(respuesta);
                    case CENTRO_IDIOMAS_CONSULTA_DEUDA ->
                        ConsultaDeudaMapper.generarRespuesta(respuesta);
                    case CENTRO_IDIOMAS_PAGO_DEUDA ->
                        PagoMapper.generarRespuesta(respuesta);
                    case CENTRO_IDIOMAS_EXTORNO_DEUDA ->
                        ExtornoMapper.generarRespuesta(respuesta);
                    default -> throw new IllegalStateException("No se pudo procesar la solicitud");
                };
            
            return new RecaudacionResponse(
                Institucion.UNIVCONTINENTAL,
                peticion.operador(),
                null,
                payloadRespuesta
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException("Error construyendo request Pago Idiomas",e);
        }
    }
}
