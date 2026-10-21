package api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorsTest extends ApiTest {

    @Test
    void unknownPathIs404WithXmlError() {
        Api.Response response = Api.get("/nope");
        assertEquals(404, response.status());
        assertEquals("404", response.text("/error/code"));
        assertEquals("Объект не найден", response.text("/error/message"));
        assertTrue(response.text("/error/timestamp").matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    @Test
    void unsupportedMethodIs405() {
        Api.Response response = Api.send("PATCH", "/space-marines/1", null);
        assertEquals(405, response.status());
        assertEquals("405", response.text("/error/code"));
    }

    @Test
    void notXmlAcceptIs406() {
        Api.Response response = Api.get("/space-marines", "Accept", "application/json");
        assertEquals(406, response.status());
        assertEquals("Поддерживается только application/xml", response.text("/error/message"));
    }

    @Test
    void notXmlContentTypeIs415() {
        Api.Response response = Api.post("/space-marines", marine("X"), "Content-Type", "text/plain");
        assertEquals(415, response.status());
        assertEquals("415", response.text("/error/code"));
    }

    @Test
    void brokenXmlIs400() {
        Api.Response response = Api.post("/space-marines", "<spaceMarine><name>broken");
        assertEquals(400, response.status());
        assertEquals("Некорректное тело запроса", response.text("/error/message"));
    }

    @Test
    void emptyBodyIs400() {
        assertEquals(400, Api.post("/space-marines", "").status());
    }

    @Test
    void plainHttpIsRefused() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(Api.HTTP_BASE + "/space-marines")).build();
        assertThrows(IOException.class, () -> client.send(request, HttpResponse.BodyHandlers.ofString()));
    }
}
