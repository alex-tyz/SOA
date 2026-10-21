package api;

import org.junit.jupiter.api.AfterAll;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class ApiTest {

    private static final List<Integer> created = new ArrayList<>();

    static String uniqueTag() {
        return "Tag" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    static String marine(String name, String health, String extra) {
        return "<spaceMarine><name>" + name + "</name><coordinates><x>1</x><y>2</y></coordinates>"
                + "<health>" + health + "</health><loyal>true</loyal>" + extra + "</spaceMarine>";
    }

    static String marine(String name) {
        return marine(name, "100", "");
    }

    static int create(String xml) {
        Api.Response response = Api.post("/space-marines", xml);
        assertEquals(201, response.status(), response.body());
        int id = Integer.parseInt(response.text("/spaceMarine/id"));
        track(id);
        return id;
    }

    static void track(int id) {
        created.add(id);
    }

    @AfterAll
    static void cleanup() {
        created.forEach(id -> Api.delete("/space-marines/" + id));
        created.clear();
    }
}
