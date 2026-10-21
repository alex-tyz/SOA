package spacemarine.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import spacemarine.entity.Weapon;

import java.time.LocalDate;

@XmlRootElement(name = "spaceMarine")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"id", "name", "coordinates", "creationDate", "health", "loyal",
        "achievements", "weaponType", "chapter"})
public class SpaceMarineDto {

    private Integer id;
    private String name;
    private CoordinatesDto coordinates;
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate creationDate;
    private Integer health;
    private Boolean loyal;
    private String achievements;
    private Weapon weaponType;
    private ChapterDto chapter;

    public SpaceMarineDto() {
    }

    public SpaceMarineDto(Integer id, String name, CoordinatesDto coordinates, LocalDate creationDate,
                          Integer health, Boolean loyal, String achievements, Weapon weaponType,
                          ChapterDto chapter) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.health = health;
        this.loyal = loyal;
        this.achievements = achievements;
        this.weaponType = weaponType;
        this.chapter = chapter;
    }
}
