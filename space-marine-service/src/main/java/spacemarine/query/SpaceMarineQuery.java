package spacemarine.query;

import java.util.List;
import java.util.Map;

public record SpaceMarineQuery(int page, int size, List<SortKey> sort, Map<SpaceMarineField, Object> filters) {
}
