package spacemarine.validation;

import jakarta.ws.rs.core.MultivaluedMap;
import spacemarine.exception.ApiException;
import spacemarine.query.SortKey;
import spacemarine.query.SpaceMarineField;
import spacemarine.query.SpaceMarineQuery;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ListQueryParser {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private ListQueryParser() {
    }

    public static SpaceMarineQuery parse(MultivaluedMap<String, String> params) {
        List<String> errors = new ArrayList<>();

        int page = DEFAULT_PAGE;
        String rawPage = single(params, "page", errors);
        if (rawPage != null) {
            Integer value = parseInt(rawPage);
            if (value == null) {
                errors.add("page: ожидалось целое число");
            } else if (value < 1) {
                errors.add("page: значение должно быть больше 0");
            } else {
                page = value;
            }
        }

        int size = DEFAULT_SIZE;
        String rawSize = single(params, "size", errors);
        if (rawSize != null) {
            Integer value = parseInt(rawSize);
            if (value == null) {
                errors.add("size: ожидалось целое число");
            } else if (value < 1 || value > MAX_SIZE) {
                errors.add("size: значение должно быть от 1 до " + MAX_SIZE);
            } else {
                size = value;
            }
        }

        List<SortKey> sort = new ArrayList<>();
        Set<SpaceMarineField> used = EnumSet.noneOf(SpaceMarineField.class);
        List<String> rawSort = params.get("sort");
        if (rawSort != null) {
            for (String item : rawSort) {
                boolean descending = item.startsWith("-");
                String name = descending ? item.substring(1) : item;
                SpaceMarineField field = SpaceMarineField.byParam(name);
                if (field == null) {
                    errors.add("sort: недопустимое значение '" + item + "'");
                } else if (!used.add(field)) {
                    errors.add("sort: поле '" + name + "' указано несколько раз");
                } else {
                    sort.add(new SortKey(field, descending));
                }
            }
        }

        Map<SpaceMarineField, Object> filters = new LinkedHashMap<>();
        for (SpaceMarineField field : SpaceMarineField.values()) {
            String raw = single(params, field.param(), errors);
            if (raw == null) {
                continue;
            }
            try {
                filters.put(field, field.parse(raw));
            } catch (IllegalArgumentException e) {
                errors.add(field.param() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw ApiException.invalidQuery(errors);
        }
        return new SpaceMarineQuery(page, size, sort, filters);
    }

    private static String single(MultivaluedMap<String, String> params, String name, List<String> errors) {
        List<String> values = params.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            errors.add(name + ": параметр указан несколько раз");
            return null;
        }
        return values.get(0);
    }

    private static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
