package ru.itmo.soa.starship;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.starship.infrastructure.persistence.StarshipRepository;

@Service
class StarshipService {
    private final StarshipRepository repository;
    private final MarineClient marines;

    StarshipService(StarshipRepository repository, MarineClient marines) {
        this.repository = repository;
        this.marines = marines;
    }

    @Transactional
    public String load(int starshipId, int marineId) {
        requireStarship(starshipId);
        if (repository.isMarineAssigned(marineId)) {
            throw new ServiceFailure(409, "Десантник уже находится на корабле");
        }
        String marine = marines.get(marineId);
        if (marine == null) {
            throw new ServiceFailure(404, "Десантник не найден");
        }
        List<Integer> onboard = repository.marineIdsOnBoard(starshipId);
        StringBuilder xml = new StringBuilder("<starship><id>").append(starshipId)
                .append("</id><spaceMarines>");
        for (int id : onboard) {
            String existing = marines.get(id);
            if (existing != null) {
                xml.append(existing);
            }
        }
        xml.append(marine).append("</spaceMarines></starship>");
        try {
            repository.loadMarine(starshipId, marineId);
        } catch (DuplicateKeyException e) {
            throw new ServiceFailure(409, "Десантник уже находится на корабле");
        }
        return xml.toString();
    }

    @Transactional
    public String unloadAll(int starshipId) {
        requireStarship(starshipId);
        List<Integer> onboard = repository.marineIdsOnBoard(starshipId);
        StringBuilder xml = new StringBuilder("<spaceMarines>");
        for (int id : onboard) {
            String marine = marines.get(id);
            if (marine != null) {
                xml.append(marine);
            }
        }
        xml.append("</spaceMarines>");
        repository.unloadAll(starshipId);
        return xml.toString();
    }

    private void requireStarship(int id) {
        if (!repository.existsForUpdate(id)) {
            throw new ServiceFailure(404, "Корабль не найден");
        }
    }
}
