package ru.itmo.soa.starship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import ru.itmo.soa.starship.generated.client.api.SpaceMarinesApi;
import ru.itmo.soa.starship.generated.client.model.SpaceMarineDto;

@Component
class MarineClient {
    private final SpaceMarinesApi api;
    private final XmlMapper xml;

    MarineClient(SpaceMarinesApi api, XmlMapper xml) {
        this.api = api;
        this.xml = xml;
    }

    // null означает, что десантник удалён из первого сервиса.
    String get(int id) {
        try {
            SpaceMarineDto marine = api.getSpaceMarineById(id);
            if (marine == null || !valid(marine, id)) {
                throw new ServiceFailure(502, "Сервис SpaceMarine вернул некорректный XML");
            }
            return xml.writeValueAsString(marine);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientResponseException e) {
            throw new ServiceFailure(502, "Сервис SpaceMarine вернул ошибку");
        } catch (ResourceAccessException e) {
            throw new ServiceFailure(isTimeout(e) ? 504 : 503,
                    isTimeout(e) ? "Сервис SpaceMarine не ответил вовремя" : "Сервис SpaceMarine недоступен");
        } catch (HttpMessageConversionException | JsonProcessingException e) {
            throw new ServiceFailure(502, "Сервис SpaceMarine вернул некорректный XML");
        }
    }

    private static boolean valid(SpaceMarineDto marine, int expectedId) {
        return marine.getId() != null && marine.getId() == expectedId
                && marine.getName() != null && !marine.getName().isBlank()
                && marine.getCoordinates() != null && marine.getCoordinates().getY() != null
                && marine.getCreationDate() != null && marine.getHealth() != null
                && marine.getHealth() > 0 && marine.getLoyal() != null;
    }

    private static boolean isTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
