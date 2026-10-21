-- База данных второго сервиса (Starship).
-- Данные SpaceMarine принадлежат первому сервису; здесь хранятся только их ID.

CREATE TABLE IF NOT EXISTS starship (
    id integer PRIMARY KEY CHECK (id > 0)
);

CREATE TABLE IF NOT EXISTS starship_space_marine (
    space_marine_id integer PRIMARY KEY CHECK (space_marine_id > 0),
    starship_id integer NOT NULL REFERENCES starship (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_starship_space_marine_starship_id
    ON starship_space_marine (starship_id);

-- API не предоставляет создание кораблей, поэтому начальный корабль создаётся здесь.
INSERT INTO starship (id) VALUES (1)
ON CONFLICT (id) DO NOTHING;
