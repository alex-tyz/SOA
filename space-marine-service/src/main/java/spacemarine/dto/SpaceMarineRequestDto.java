package spacemarine.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "spaceMarine")
@XmlAccessorType(XmlAccessType.FIELD)
public class SpaceMarineRequestDto {

    private String name;
    private CoordinatesRequestDto coordinates;
    private String health;
    private String loyal;
    private String achievements;
    private String weaponType;
    private ChapterDto chapter;

    public String getName() {
        return name;
    }

    public CoordinatesRequestDto getCoordinates() {
        return coordinates;
    }

    public String getHealth() {
        return health;
    }

    public String getLoyal() {
        return loyal;
    }

    public String getAchievements() {
        return achievements;
    }

    public String getWeaponType() {
        return weaponType;
    }

    public ChapterDto getChapter() {
        return chapter;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CoordinatesRequestDto {

        private String x;
        private String y;

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }
    }
}
