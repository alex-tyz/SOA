package spacemarine.mapper;

import spacemarine.dto.ChapterDto;
import spacemarine.dto.CoordinatesDto;
import spacemarine.dto.SpaceMarineDto;
import spacemarine.entity.Chapter;
import spacemarine.entity.Coordinates;
import spacemarine.entity.SpaceMarine;

import java.util.List;

public final class SpaceMarineMapper {

    private SpaceMarineMapper() {
    }

    public static SpaceMarineDto toDto(SpaceMarine entity) {
        Coordinates coordinates = entity.getCoordinates();
        Chapter chapter = entity.getChapter();
        boolean hasChapter = chapter != null && (chapter.getName() != null || chapter.getParentLegion() != null);
        return new SpaceMarineDto(
                entity.getId(),
                entity.getName(),
                coordinates == null ? null : new CoordinatesDto(coordinates.getX(), coordinates.getY()),
                entity.getCreationDate(),
                entity.getHealth(),
                entity.getLoyal(),
                entity.getAchievements(),
                entity.getWeaponType(),
                hasChapter ? new ChapterDto(chapter.getName(), chapter.getParentLegion()) : null);
    }

    public static List<SpaceMarineDto> toDtoList(List<SpaceMarine> entities) {
        return entities.stream().map(SpaceMarineMapper::toDto).toList();
    }

    public static void copyFields(SpaceMarine from, SpaceMarine to) {
        to.setName(from.getName());
        to.setCoordinates(from.getCoordinates());
        to.setHealth(from.getHealth());
        to.setLoyal(from.getLoyal());
        to.setAchievements(from.getAchievements());
        to.setWeaponType(from.getWeaponType());
        to.setChapter(from.getChapter());
    }
}
