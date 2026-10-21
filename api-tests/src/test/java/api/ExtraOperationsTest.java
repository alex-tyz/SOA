package api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtraOperationsTest extends ApiTest {

    private static final String ITEMS = "/spaceMarines/spaceMarine";

    @Test
    void searchByAchievementsSubstringIsCaseSensitiveAndLiteral() {
        String tag = uniqueTag();
        create(marine("Hero", "10", "<achievements>Hero-" + tag + "</achievements>"));
        create(marine("Other", "10", "<achievements>Other</achievements>"));

        Api.Response found = Api.get("/space-marines/by-achievements?substring=" + Api.encode(tag));
        assertEquals(200, found.status());
        assertEquals(List.of("Hero"), found.texts(ITEMS + "/name"));

        assertEquals(0, Api.get("/space-marines/by-achievements?substring=" + Api.encode(tag.toUpperCase())).count(ITEMS));
        assertEquals(0, Api.get("/space-marines/by-achievements?substring=" + Api.encode("%" + tag)).count(ITEMS));
    }

    @Test
    void searchRequiresNonEmptySubstring() {
        assertEquals(400, Api.get("/space-marines/by-achievements").status());
        assertEquals(400, Api.get("/space-marines/by-achievements?substring=").status());
    }

    @Test
    void uniqueAchievementsAreSortedDistinctAndWithoutNulls() {
        String tag = uniqueTag();
        create(marine("A", "10", "<achievements>Hero-" + tag + "</achievements>"));
        create(marine("B", "10", "<achievements>Hero-" + tag + "</achievements>"));
        create(marine("C", "10", ""));

        Api.Response response = Api.get("/space-marines/achievements/unique");
        assertEquals(200, response.status());
        List<String> values = response.texts("/achievements/achievement");
        assertEquals(1, values.stream().filter(("Hero-" + tag)::equals).count());
        List<String> sorted = new ArrayList<>(values);
        sorted.sort(String::compareTo);
        assertEquals(sorted, values);
    }

    @Test
    void notXmlAcceptIs406() {
        assertEquals(406, Api.get("/space-marines/achievements/unique", "Accept", "application/json").status());
        assertEquals(406, Api.get("/space-marines/by-achievements?substring=x", "Accept", "application/json").status());
    }

    @Test
    void deleteByWeaponRemovesOneAndReturnsIt() {
        create(marine("Inferno", "10", "<weaponType>INFERNO_PISTOL</weaponType>"));

        Api.Response response = Api.delete("/space-marines/by-weapon-type/INFERNO_PISTOL");

        assertEquals(200, response.status());
        assertEquals("INFERNO_PISTOL", response.text("/spaceMarine/weaponType"));
        assertEquals(404, Api.get("/space-marines/" + response.text("/spaceMarine/id")).status());
    }

    @Test
    void deleteByWeaponValidatesTheValue() {
        for (String bad : new String[]{"LASER", "meltagun"}) {
            Api.Response response = Api.delete("/space-marines/by-weapon-type/" + bad);
            assertEquals(400, response.status(), bad);
            assertEquals("Некорректный path-параметр", response.text("/error/message"));
        }
    }
}
