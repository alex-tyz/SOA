package spacemarine.exception;

import java.util.List;

public class ApiException extends RuntimeException {
    private final int status;
    private final List<String> details;
    public ApiException( int status,String message,  List<String> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public static ApiException invalidPath(List<String> details) {
        return new ApiException(400, "Некорректный path-параметр", details);
    }

    public static ApiException invalidQuery(List<String> details) {
        return new ApiException(400, "Некорректные query-параметры", details);
    }

    public static ApiException invalidBody(List<String> details) {
        return new ApiException(400, "Некорректное тело запроса", details);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(404, "Объект не найден", List.of(detail));
    }

    public static ApiException unprocessable(List<String> details) {
        return new ApiException(422, "Нарушены ограничения целостности", details);
    }

    public int getStatus() {
        return status;
    }
    public List<String> getDetails() {
        return details;
    }
}
