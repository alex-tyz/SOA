package spacemarine.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import spacemarine.dto.ErrorDto;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException e) {
        return Response.status(e.getStatus())
                .entity(new ErrorDto(e.getStatus(), e.getMessage(), e.getDetails()))
                .type(MediaType.APPLICATION_XML)
                .build();
    }
}
