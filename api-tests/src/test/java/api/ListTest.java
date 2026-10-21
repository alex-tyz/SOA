package api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListTest extends ApiTest {

    private static final String NAMES = "/spaceMarinePage/spaceMarines/spaceMarine/name";

    private static String tag;
    private static String scope;

    @BeforeAll
    static void createData() {
        tag = uniqueTag();
        scope = "chapter.parentLegion=" + tag;
        String chapter = "<chapter><name>C</name><parentLegion>" + tag + "</parentLegion></chapter>";
        create(marine("Alpha", "100", "<achievements>Beta</achievements><weaponType>MELTAGUN</weaponType>" + chapter));
        create(marine("Bravo", "50", chapter));
        create("<spaceMarine><name>Charlie</name><coordinates><x>3</x><y>3</y></coordinates><health>100</health>"
                + "<loyal>false</loyal><achievements>Alpha</achievements><weaponType>MELTAGUN</weaponType>" + chapter
                + "</spaceMarine>");
        create(marine("Delta", "75", "<achievements>Beta</achievements><weaponType>HEAVY_BOLTGUN</weaponType>" + chapter));
        create(marine("Echo", "10", chapter));
    }

    private static Api.Response list(String query) {
        Api.Response response = Api.get("/space-marines?" + scope + (query.isEmpty() ? "" : "&" + query));
        assertEquals(200, response.status(), response.body());
        return response;
    }

    private static List<String> names(String query) {
        return list(query).texts(NAMES);
    }

    @Test
    void defaultsAreFirstPageOfTenSortedById() {
        Api.Response response = list("");
        assertEquals(List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo"), response.texts(NAMES));
        assertEquals("1", response.text("/spaceMarinePage/page"));
        assertEquals("10", response.text("/spaceMarinePage/size"));
        assertEquals("5", response.text("/spaceMarinePage/totalElements"));
        assertEquals("1", response.text("/spaceMarinePage/totalPages"));
    }

    @Test
    void sortsBySeveralFields() {
        assertEquals(List.of("Alpha", "Charlie", "Delta", "Bravo", "Echo"), names("sort=-health&sort=name"));
    }

    @Test
    void nullsAreLastInBothDirections() {
        assertEquals(List.of("Charlie", "Alpha", "Delta", "Bravo", "Echo"), names("sort=achievements"));
        assertEquals(List.of("Alpha", "Delta", "Charlie", "Bravo", "Echo"), names("sort=-achievements"));
    }

    @Test
    void filtersAreCombinedWithAnd() {
        assertEquals(List.of("Alpha", "Charlie"), names("weaponType=MELTAGUN"));
        assertEquals(List.of("Charlie"), names("loyal=false"));
        assertEquals(List.of("Alpha"), names("health=100&loyal=true"));
        assertEquals(5, names("chapter.name=C&creationDate=" + LocalDate.now()).size());
        assertEquals(List.of("Charlie"), names("coordinates.x=3"));
        assertEquals(List.of("Alpha", "Delta"), names("achievements=Beta"));
    }

    @Test
    void paginates() {
        Api.Response third = list("size=2&page=3");
        assertEquals(List.of("Echo"), third.texts(NAMES));
        assertEquals("3", third.text("/spaceMarinePage/totalPages"));
        assertEquals(List.of("Charlie", "Delta"), names("size=2&page=2"));
    }

    @Test
    void pageBeyondTheEndIsEmptyButKeepsTotals() {
        for (String query : new String[]{"page=99", "page=2147483647&size=100"}) {
            Api.Response response = list(query);
            assertEquals(0, response.count("/spaceMarinePage/spaceMarines/spaceMarine"), query);
            assertEquals("5", response.text("/spaceMarinePage/totalElements"), query);
        }
    }

    @Test
    void invalidQueryParametersAre400() {
        for (String query : new String[]{"page=0", "page=abc", "size=0", "size=101", "sort=foo",
                "sort=health&sort=-health", "weaponType=meltagun", "health=abc", "loyal=yes", "creationDate=2026-13-45"}) {
            Api.Response response = Api.get("/space-marines?" + query);
            assertEquals(400, response.status(), query);
            assertEquals("Некорректные query-параметры", response.text("/error/message"), query);
        }
    }

    @Test
    void allQueryErrorsAreReportedAtOnce() {
        Api.Response response = Api.get("/space-marines?page=0&health=abc&weaponType=LASER&loyal=yes&id=0&name=");
        assertEquals(400, response.status());
        assertEquals(6, response.count("/error/details/detail"));
    }
}
