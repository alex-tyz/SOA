package api;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrudTest extends ApiTest {

    private static final String FULL_EXTRA = "<achievements>Hero</achievements><weaponType>MELTAGUN</weaponType>"
            + "<chapter><name>Ultramarines</name><parentLegion>XIII</parentLegion></chapter>";

    @Test
    void createReturns201LocationAndBody() {
        Api.Response response = Api.post("/space-marines", marine("Titus", "100", FULL_EXTRA));
        int id = Integer.parseInt(response.text("/spaceMarine/id"));
        track(id);

        assertEquals(201, response.status());
        assertEquals("/api/v1/space-marines/" + id, response.header("Location"));
        assertEquals("Titus", response.text("/spaceMarine/name"));
        assertEquals("1", response.text("/spaceMarine/coordinates/x"));
        assertEquals("2", response.text("/spaceMarine/coordinates/y"));
        assertEquals(LocalDate.now().toString(), response.text("/spaceMarine/creationDate"));
        assertEquals("100", response.text("/spaceMarine/health"));
        assertEquals("true", response.text("/spaceMarine/loyal"));
        assertEquals("Hero", response.text("/spaceMarine/achievements"));
        assertEquals("MELTAGUN", response.text("/spaceMarine/weaponType"));
        assertEquals("Ultramarines", response.text("/spaceMarine/chapter/name"));
        assertEquals("XIII", response.text("/spaceMarine/chapter/parentLegion"));
    }

    @Test
    void createWithoutOptionalFieldsOmitsThem() {
        int id = create(marine("Cato"));
        Api.Response response = Api.get("/space-marines/" + id);
        assertEquals(200, response.status());
        assertEquals(0, response.count("/spaceMarine/achievements"));
        assertEquals(0, response.count("/spaceMarine/weaponType"));
        assertEquals(0, response.count("/spaceMarine/chapter"));
    }

    @Test
    void wrongTypesIs400WithAllDetails() {
        String xml = "<spaceMarine><name>X</name><coordinates><x>a</x><y>2</y></coordinates>"
                + "<health>abc</health><loyal>maybe</loyal></spaceMarine>";
        Api.Response response = Api.post("/space-marines", xml);
        assertEquals(400, response.status());
        assertEquals("Некорректное тело запроса", response.text("/error/message"));
        assertEquals(3, response.count("/error/details/detail"));
    }

    @Test
    void constraintViolationsAre422WithAllDetails() {
        String xml = "<spaceMarine><name>  </name><coordinates><x>1</x><y>2</y></coordinates><health>0</health>"
                + "<loyal>true</loyal><weaponType>LASER</weaponType><chapter><parentLegion>L</parentLegion></chapter></spaceMarine>";
        Api.Response response = Api.post("/space-marines", xml);
        assertEquals(422, response.status());
        assertEquals("Нарушены ограничения целостности", response.text("/error/message"));
        assertEquals(4, response.count("/error/details/detail"));
    }

    @Test
    void missingRequiredFieldsAre422() {
        Api.Response response = Api.post("/space-marines", "<spaceMarine><name>X</name></spaceMarine>");
        assertEquals(422, response.status());
        assertEquals(3, response.count("/error/details/detail"));
    }

    @Test
    void getByIdValidatesPathAndExistence() {
        for (String bad : new String[]{"abc", "0", "-1", "99999999999"}) {
            Api.Response response = Api.get("/space-marines/" + bad);
            assertEquals(400, response.status(), bad);
            assertEquals("Некорректный path-параметр", response.text("/error/message"));
        }
        assertEquals(404, Api.get("/space-marines/" + Integer.MAX_VALUE).status());
    }

    @Test
    void putReplacesFieldsAndKeepsIdAndCreationDate() {
        int id = create(marine("Titus", "100", FULL_EXTRA));
        String creationDate = Api.get("/space-marines/" + id).text("/spaceMarine/creationDate");

        Api.Response response = Api.put("/space-marines/" + id, marine("Titus II", "77", ""));

        assertEquals(200, response.status());
        assertEquals(String.valueOf(id), response.text("/spaceMarine/id"));
        assertEquals(creationDate, response.text("/spaceMarine/creationDate"));
        assertEquals("Titus II", response.text("/spaceMarine/name"));
        assertEquals("77", response.text("/spaceMarine/health"));
        assertEquals(0, response.count("/spaceMarine/achievements"));
        assertEquals(0, response.count("/spaceMarine/chapter"));
    }

    @Test
    void putErrors() {
        int id = create(marine("Titus"));
        assertEquals(404, Api.put("/space-marines/" + Integer.MAX_VALUE, marine("X")).status());
        assertEquals(400, Api.put("/space-marines/abc", marine("X")).status());
        assertEquals(400, Api.put("/space-marines/" + id, "<spaceMarine><name>X</name><health>abc</health></spaceMarine>").status());
        assertEquals(422, Api.put("/space-marines/" + id, marine("", "0", "")).status());
        assertEquals("Titus", Api.get("/space-marines/" + id).text("/spaceMarine/name"));
    }

    @Test
    void deleteReturns204ThenMissingIs404() {
        int id = create(marine("Titus"));
        assertEquals(204, Api.delete("/space-marines/" + id, "Accept", "application/json").status());
        assertEquals(404, Api.delete("/space-marines/" + id).status());
        assertEquals(404, Api.get("/space-marines/" + id).status());
        assertEquals(400, Api.delete("/space-marines/abc").status());
    }
}
