package spacemarine.query;

import spacemarine.entity.Weapon;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

public enum SpaceMarineField {
    ID("id", "m.id", SpaceMarineField::positiveInt),
    NAME("name", "m.name", SpaceMarineField::nonEmpty),
    COORDINATES_X("coordinates.x", "m.coordinates.x", SpaceMarineField::anyInt),
    COORDINATES_Y("coordinates.y", "m.coordinates.y", SpaceMarineField::anyInt),
    CREATION_DATE("creationDate", "m.creationDate", SpaceMarineField::date),
    HEALTH("health", "m.health", SpaceMarineField::positiveInt),
    LOYAL("loyal", "m.loyal", SpaceMarineField::bool),
    ACHIEVEMENTS("achievements", "m.achievements", value -> value),
    WEAPON_TYPE("weaponType", "m.weaponType", SpaceMarineField::weapon),
    CHAPTER_NAME("chapter.name", "m.chapter.name", SpaceMarineField::nonEmpty),
    CHAPTER_PARENT_LEGION("chapter.parentLegion", "m.chapter.parentLegion", value -> value);

    private final String param;
    private final String path;
    private final Function<String, Object> parser;

    SpaceMarineField(String param, String path, Function<String, Object> parser) {
        this.param = param;
        this.path = path;
        this.parser = parser;
    }

    public String param() {
        return param;
    }

    public String path() {
        return path;
    }

    public Object parse(String raw) {
        return parser.apply(raw);
    }

    public static SpaceMarineField byParam(String param) {
        for (SpaceMarineField field : values()) {
            if (field.param.equals(param)) {
                return field;
            }
        }
        return null;
    }

    private static Object anyInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ожидалось целое число");
        }
    }

    private static Object positiveInt(String raw) {
        int value = (Integer) anyInt(raw);
        if (value < 1) {
            throw new IllegalArgumentException("значение должно быть больше 0");
        }
        return value;
    }

    private static Object date(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("ожидалась дата в формате ГГГГ-ММ-ДД");
        }
    }

    private static Object bool(String raw) {
        if ("true".equals(raw) || "false".equals(raw)) {
            return Boolean.valueOf(raw);
        }
        throw new IllegalArgumentException("ожидалось true или false");
    }

    private static Object nonEmpty(String raw) {
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("строка не может быть пустой");
        }
        return raw;
    }

    private static Object weapon(String raw) {
        try {
            return Weapon.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("недопустимое значение");
        }
    }
}
