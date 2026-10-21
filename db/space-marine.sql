-- Пересоздаём таблицу с нуля, чтобы скрипт можно было запускать повторно
DROP TABLE IF EXISTS space_marine;

CREATE TABLE space_marine (
    -- id: целое > 0, уникальное, генерирует база (SERIAL = автоинкремент)
                              id                    SERIAL       PRIMARY KEY,

    -- name: не null, не пустая и не из одних пробелов (btrim обрезает пробелы)
                              name                  VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),

    -- coordinates: вложенный объект разворачиваем в две колонки, обе обязательны
                              coordinates_x         INTEGER      NOT NULL,
                              coordinates_y         INTEGER      NOT NULL,

    -- creationDate: не null, если не передали — база подставит сегодняшнюю дату
                              creation_date         DATE         NOT NULL DEFAULT CURRENT_DATE,

    -- health: обязательное, строго больше 0
                              health                INTEGER      NOT NULL CHECK (health > 0),

    -- loyal: обязательное
                              loyal                 BOOLEAN      NOT NULL,

    -- achievements: может быть null
                              achievements          TEXT,

    -- weaponType: может быть null, иначе только одно из значений enum Weapon
                              weapon_type           VARCHAR(20)  CHECK (weapon_type IN
                                                                        ('HEAVY_BOLTGUN', 'MELTAGUN', 'BOLT_PISTOL', 'INFERNO_PISTOL')),

    -- chapter: весь объект может отсутствовать (обе колонки null)
                              chapter_name          VARCHAR(255) CHECK (chapter_name IS NULL OR btrim(chapter_name) <> ''),
                              chapter_parent_legion VARCHAR(255),

    -- если указан parentLegion, то орден существует, а значит name у него обязателен
                              CHECK (chapter_name IS NOT NULL OR chapter_parent_legion IS NULL)
);