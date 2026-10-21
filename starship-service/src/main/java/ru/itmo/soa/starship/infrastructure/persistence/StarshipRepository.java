package ru.itmo.soa.starship.infrastructure.persistence;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StarshipRepository {
    private final JdbcTemplate db;

    public StarshipRepository(JdbcTemplate db) {
        this.db = db;
    }

    public boolean existsForUpdate(int starshipId) {
        return !db.queryForList(
                "SELECT id FROM starship WHERE id = ? FOR UPDATE",
                Integer.class, starshipId).isEmpty();
    }

    public boolean isMarineAssigned(int marineId) {
        return !db.queryForList(
                "SELECT space_marine_id FROM starship_space_marine WHERE space_marine_id = ?",
                Integer.class, marineId).isEmpty();
    }

    public List<Integer> marineIdsOnBoard(int starshipId) {
        return db.queryForList(
                "SELECT space_marine_id FROM starship_space_marine WHERE starship_id = ? ORDER BY space_marine_id",
                Integer.class, starshipId);
    }

    public void loadMarine(int starshipId, int marineId) {
        db.update("INSERT INTO starship_space_marine (starship_id, space_marine_id) VALUES (?, ?)",
                starshipId, marineId);
    }

    public void unloadAll(int starshipId) {
        db.update("DELETE FROM starship_space_marine WHERE starship_id = ?", starshipId);
    }
}
