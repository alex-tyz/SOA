package spacemarine.validation;

import spacemarine.entity.Weapon;
import spacemarine.exception.ApiException;

import java.util.List;

public final class PathParams {

    private PathParams() {
    }

    public static int parseId(String raw) {
        try {
            int value = Integer.parseInt(raw);
            if (value >= 1 && !raw.startsWith("+")) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        throw ApiException.invalidPath(List.of("id: ожидалось целое число больше 0"));
    }

    public static Weapon parseWeapon(String raw) {
        try {
            return Weapon.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidPath(List.of("weaponType: недопустимое значение"));
        }
    }
}
