package spacemarine.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import spacemarine.dto.ErrorDto;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        String message = switch (status) {
            case 400 -> "Некорректное тело запроса";
            case 404 -> "Объект не найден";
            case 405 -> "Метод не поддерживается";
            case 406, 415 -> "Поддерживается только application/xml";
            default -> status >= 500 ? "Внутренняя ошибка сервера" : "Ошибка запроса";
        };
        Response.ResponseBuilder builder = Response.status(status)
                .entity(new ErrorDto(status, message, null))
                .type(MediaType.APPLICATION_XML);
        String allow = e.getResponse().getHeaderString("Allow");
        if (allow != null) {
            builder.header("Allow", allow);
        }
        return builder.build();
    }
}
