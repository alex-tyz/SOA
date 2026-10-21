package spacemarine.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import spacemarine.entity.SpaceMarine;
import spacemarine.entity.Weapon;
import spacemarine.query.SortKey;
import spacemarine.query.SpaceMarineField;
import spacemarine.query.SpaceMarineQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SpaceMarineRepository {

    @PersistenceContext(unitName = "spacemarine")
    private EntityManager em;

    public void save(SpaceMarine marine) {
        em.persist(marine);
        em.flush();
    }

    public SpaceMarine find(int id) {
        return em.find(SpaceMarine.class, id);
    }

    public void delete(SpaceMarine marine) {
        em.remove(marine);
    }

    public SpaceMarine findFirstByWeapon(Weapon weapon) {
        List<SpaceMarine> result = em
                .createQuery("SELECT m FROM SpaceMarine m WHERE m.weaponType = :weapon ORDER BY m.id ASC",
                        SpaceMarine.class)
                .setParameter("weapon", weapon)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public PageResult<SpaceMarine> findPage(SpaceMarineQuery query) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder();
        int index = 0;
        for (Map.Entry<SpaceMarineField, Object> filter : query.filters().entrySet()) {
            where.append(index == 0 ? " WHERE " : " AND ")
                    .append(filter.getKey().path())
                    .append(" = :p")
                    .append(index);
            params.put("p" + index, filter.getValue());
            index++;
        }

        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(m) FROM SpaceMarine m" + where, Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        long offset = (long) (query.page() - 1) * query.size();
        if (offset >= total) {
            return new PageResult<>(List.of(), total);
        }

        List<String> order = new ArrayList<>();
        boolean sortedById = false;
        for (SortKey key : query.sort()) {
            if (key.field() == SpaceMarineField.ID) {
                sortedById = true;
                order.add(key.field().path() + (key.descending() ? " DESC" : " ASC"));
            } else {
                order.add(key.field().path() + (key.descending() ? " DESC" : " ASC") + " NULLS LAST");
            }
        }
        if (!sortedById) {
            order.add("m.id ASC");
        }

        TypedQuery<SpaceMarine> dataQuery = em.createQuery(
                "SELECT m FROM SpaceMarine m" + where + " ORDER BY " + String.join(", ", order),
                SpaceMarine.class);
        params.forEach(dataQuery::setParameter);
        List<SpaceMarine> items = dataQuery
                .setFirstResult((int) offset)
                .setMaxResults(query.size())
                .getResultList();
        return new PageResult<>(items, total);
    }

    public List<SpaceMarine> findByAchievementsContaining(String substring) {
        return em.createQuery(
                        "SELECT m FROM SpaceMarine m WHERE m.achievements IS NOT NULL "
                                + "AND LOCATE(:substring, m.achievements) > 0 ORDER BY m.id ASC",
                        SpaceMarine.class)
                .setParameter("substring", substring)
                .getResultList();
    }

    public List<String> findDistinctAchievements() {
        return em.createQuery(
                        "SELECT DISTINCT m.achievements FROM SpaceMarine m WHERE m.achievements IS NOT NULL",
                        String.class)
                .getResultList();
    }
}
