package spacemarine.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import spacemarine.entity.SpaceMarine;
import spacemarine.entity.Weapon;
import spacemarine.exception.ApiException;
import spacemarine.mapper.SpaceMarineMapper;
import spacemarine.query.SpaceMarineQuery;
import spacemarine.repository.PageResult;
import spacemarine.repository.SpaceMarineRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Transactional
public class SpaceMarineService {

    @Inject
    SpaceMarineRepository repository;

    public SpaceMarine create(SpaceMarine candidate) {
        candidate.setCreationDate(LocalDate.now());
        repository.save(candidate);
        return candidate;
    }

    public SpaceMarine get(int id) {
        return findOrThrow(id);
    }

    public SpaceMarine update(int id, SpaceMarine candidate) {
        SpaceMarine existing = findOrThrow(id);
        SpaceMarineMapper.copyFields(candidate, existing);
        return existing;
    }

    public void delete(int id) {
        repository.delete(findOrThrow(id));
    }

    public SpaceMarine deleteAnyByWeapon(Weapon weapon) {
        SpaceMarine marine = repository.findFirstByWeapon(weapon);
        if (marine == null) {
            throw ApiException.notFound("SpaceMarine с weaponType=" + weapon + " не найден");
        }
        repository.delete(marine);
        return marine;
    }

    public PageResult<SpaceMarine> list(SpaceMarineQuery query) {
        return repository.findPage(query);
    }

    public List<SpaceMarine> findByAchievements(String substring) {
        return repository.findByAchievementsContaining(substring);
    }

    public List<String> uniqueAchievements() {
        List<String> result = new ArrayList<>(repository.findDistinctAchievements());
        result.sort(String::compareTo);
        return result;
    }

    private SpaceMarine findOrThrow(int id) {
        SpaceMarine marine = repository.find(id);
        if (marine == null) {
            throw ApiException.notFound("SpaceMarine с id=" + id + " не найден");
        }
        return marine;
    }
}
