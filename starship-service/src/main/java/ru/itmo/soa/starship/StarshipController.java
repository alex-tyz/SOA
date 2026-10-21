package ru.itmo.soa.starship;

import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/starship")
class StarshipController {
    private final StarshipService service;

    StarshipController(StarshipService service) {
        this.service = service;
    }

    @PostMapping(value = "/{starship-id}/load/{space-marine-id}", produces = MediaType.APPLICATION_XML_VALUE)
    String load(@PathVariable("starship-id") String starshipId,
                @PathVariable("space-marine-id") String marineId) {
        return service.load(positiveId(starshipId), positiveId(marineId));
    }

    @PostMapping(value = "/{starship-id}/unload-all", produces = MediaType.APPLICATION_XML_VALUE)
    String unloadAll(@PathVariable("starship-id") String starshipId) {
        return service.unloadAll(positiveId(starshipId));
    }

    @ExceptionHandler(ServiceFailure.class)
    ResponseEntity<String> error(ServiceFailure failure) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(failure.status())
                .contentType(MediaType.APPLICATION_XML);
        if (failure.status() == 503) {
            response.header("Retry-After", "30");
        }
        return response.body(errorXml(failure.status(), failure.getMessage()));
    }

    static String errorXml(int status, String message) {
        return "<error><code>" + status + "</code><message>"
                + escape(message) + "</message><timestamp>" + Instant.now()
                + "</timestamp></error>";
    }

    private static int positiveId(String raw) {
        try {
            int id = Integer.parseInt(raw);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Обрабатывается одинаково с нулём и отрицательными значениями.
        }
        throw new ServiceFailure(400, "Некорректный path-параметр");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
