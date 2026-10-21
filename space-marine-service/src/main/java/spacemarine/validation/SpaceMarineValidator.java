package spacemarine.validation;

import spacemarine.dto.ChapterDto;
import spacemarine.dto.SpaceMarineRequestDto;
import spacemarine.entity.Chapter;
import spacemarine.entity.Coordinates;
import spacemarine.entity.SpaceMarine;
import spacemarine.entity.Weapon;
import spacemarine.exception.ApiException;

import java.util.ArrayList;
import java.util.List;

public final class SpaceMarineValidator {

    private SpaceMarineValidator() {
    }

    public static SpaceMarine validate(SpaceMarineRequestDto dto) {
        if (dto == null) {
            throw ApiException.invalidBody(List.of("тело запроса пустое"));
        }

        List<String> typeErrors = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        String name = dto.getName();
        if (name == null) {
            violations.add("name: поле обязательно");
        } else if (name.isBlank()) {
            violations.add("name: строка не может быть пустой");
        }

        Coordinates coordinates = null;
        SpaceMarineRequestDto.CoordinatesRequestDto rawCoordinates = dto.getCoordinates();
        if (rawCoordinates == null) {
            violations.add("coordinates: поле обязательно");
        } else {
            Integer x = parseInt("coordinates.x", rawCoordinates.getX(), typeErrors, violations);
            Integer y = parseInt("coordinates.y", rawCoordinates.getY(), typeErrors, violations);
            coordinates = new Coordinates();
            coordinates.setX(x);
            coordinates.setY(y);
        }

        Integer health = parseInt("health", dto.getHealth(), typeErrors, violations);
        if (health != null && health <= 0) {
            violations.add("health: значение должно быть больше 0");
        }

        Boolean loyal = null;
        String rawLoyal = dto.getLoyal();
        if (rawLoyal == null) {
            violations.add("loyal: поле обязательно");
        } else if ("true".equals(rawLoyal.trim())) {
            loyal = Boolean.TRUE;
        } else if ("false".equals(rawLoyal.trim())) {
            loyal = Boolean.FALSE;
        } else {
            typeErrors.add("loyal: ожидалось true или false");
        }

        Weapon weaponType = null;
        String rawWeapon = dto.getWeaponType();
        if (rawWeapon != null) {
            try {
                weaponType = Weapon.valueOf(rawWeapon.trim());
            } catch (IllegalArgumentException e) {
                violations.add("weaponType: недопустимое значение");
            }
        }

        Chapter chapter = null;
        ChapterDto rawChapter = dto.getChapter();
        if (rawChapter != null) {
            if (rawChapter.getName() == null) {
                violations.add("chapter.name: поле обязательно");
            } else if (rawChapter.getName().isBlank()) {
                violations.add("chapter.name: строка не может быть пустой");
            }
            chapter = new Chapter();
            chapter.setName(rawChapter.getName());
            chapter.setParentLegion(rawChapter.getParentLegion());
        }

        if (!typeErrors.isEmpty()) {
            throw ApiException.invalidBody(typeErrors);
        }
        if (!violations.isEmpty()) {
            throw ApiException.unprocessable(violations);
        }

        SpaceMarine marine = new SpaceMarine();
        marine.setName(name);
        marine.setCoordinates(coordinates);
        marine.setHealth(health);
        marine.setLoyal(loyal);
        marine.setAchievements(dto.getAchievements());
        marine.setWeaponType(weaponType);
        marine.setChapter(chapter);
        return marine;
    }

    private static Integer parseInt(String field, String raw, List<String> typeErrors, List<String> violations) {
        if (raw == null) {
            violations.add(field + ": поле обязательно");
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            typeErrors.add(field + ": ожидалось целое число");
            return null;
        }
    }
}
