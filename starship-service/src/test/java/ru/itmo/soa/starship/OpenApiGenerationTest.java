package ru.itmo.soa.starship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PostMapping;
import ru.itmo.soa.starship.generated.api.StarshipApi;
import ru.itmo.soa.starship.generated.client.api.SpaceMarinesApi;
import ru.itmo.soa.starship.generated.client.model.WeaponDto;

class OpenApiGenerationTest {
    @Test
    void unknownWeaponIsRejectedByMustacheGeneratedEnum() {
        assertEquals(WeaponDto.MELTAGUN, WeaponDto.fromValue("MELTAGUN"));
        assertThrows(IllegalArgumentException.class, () -> WeaponDto.fromValue("UNKNOWN"));
    }

    @Test
    void generatedContractsExposeExpectedXmlRoutes() throws NoSuchMethodException {
        Method load = StarshipApi.class.getMethod("loadSpaceMarine", Integer.class, Integer.class);
        RequestMapping loadRoute = load.getAnnotation(RequestMapping.class);
        assertEquals(RequestMethod.POST, loadRoute.method()[0]);
        assertEquals("/starship/{starship-id}/load/{space-marine-id}", loadRoute.value()[0]);
        assertEquals("application/xml", loadRoute.produces()[0]);

        Method getMarine = SpaceMarinesApi.class.getMethod("getSpaceMarineById", Integer.class);
        assertEquals(ru.itmo.soa.starship.generated.client.model.SpaceMarineDto.class,
                getMarine.getReturnType());

        RequestMapping base = StarshipController.class.getAnnotation(RequestMapping.class);
        PostMapping implementedLoad = StarshipController.class
                .getDeclaredMethod("load", String.class, String.class)
                .getAnnotation(PostMapping.class);
        PostMapping implementedUnload = StarshipController.class
                .getDeclaredMethod("unloadAll", String.class)
                .getAnnotation(PostMapping.class);
        assertEquals("/api/v1/starship", base.value()[0]);
        assertEquals("/{starship-id}/load/{space-marine-id}", implementedLoad.value()[0]);
        assertEquals("/{starship-id}/unload-all", implementedUnload.value()[0]);
        assertEquals("application/xml", implementedLoad.produces()[0]);
        assertEquals("application/xml", implementedUnload.produces()[0]);
    }

    @Test
    void spaceMarineXmlKeepsContractRootAndRejectsDoctype() throws Exception {
        XmlMapper mapper = new StarshipConfig().xmlMapper();
        String xml = "<spaceMarine><id>7</id><name>Titus</name>"
                + "<coordinates><x>1</x><y>2.5</y></coordinates>"
                + "<creationDate>2026-10-03</creationDate><health>100</health>"
                + "<loyal>true</loyal></spaceMarine>";
        var marine = mapper.readValue(xml,
                ru.itmo.soa.starship.generated.client.model.SpaceMarineDto.class);
        assertEquals(7, marine.getId());
        String serialized = mapper.writeValueAsString(marine);
        assertTrue(serialized.startsWith("<spaceMarine>"));
        assertTrue(serialized.contains("<creationDate>2026-10-03</creationDate>"));
        assertThrows(Exception.class, () -> mapper.readValue(
                "<!DOCTYPE spaceMarine [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]>"
                        + xml.replace("Titus", "&xxe;"),
                ru.itmo.soa.starship.generated.client.model.SpaceMarineDto.class));
    }

    @Test
    void generatedClientFetchesMarineAndHandlesMissingId() throws Exception {
        String xml = "<spaceMarine><id>7</id><name>Titus</name>"
                + "<coordinates><x>1</x><y>2.5</y></coordinates>"
                + "<creationDate>2026-10-03</creationDate><health>100</health>"
                + "<loyal>true</loyal></spaceMarine>";
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/space-marines/7", exchange -> {
            assertEquals("application/xml", exchange.getRequestHeaders().getFirst("Accept"));
            byte[] body = xml.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/api/v1/space-marines/9", exchange -> {
            byte[] body = "<!DOCTYPE spaceMarine><spaceMarine/>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            StarshipConfig config = new StarshipConfig();
            XmlMapper mapper = config.xmlMapper();
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1");
            MarineClient client = new MarineClient(config.spaceMarinesApi(base, mapper), mapper);
            assertTrue(client.get(7).startsWith("<spaceMarine>"));
            assertNull(client.get(8));
            assertEquals(502, assertThrows(ServiceFailure.class, () -> client.get(9)).status());
        } finally {
            server.stop(0);
        }
    }
}
