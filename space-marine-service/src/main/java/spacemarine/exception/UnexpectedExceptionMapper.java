package spacemarine.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import spacemarine.dto.ErrorDto;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        LOG.log(Level.SEVERE, "Непредвиденная ошибка", e);
        return Response.status(500)
                .entity(new ErrorDto(500, "Внутренняя ошибка сервера", null))
                .type(MediaType.APPLICATION_XML)
                .build();
    }
}
