package spacemarine.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "spaceMarines")
@XmlAccessorType(XmlAccessType.FIELD)
public class SpaceMarineListDto {

    @XmlElement(name = "spaceMarine")
    private List<SpaceMarineDto> spaceMarines;

    public SpaceMarineListDto() {
    }

    public SpaceMarineListDto(List<SpaceMarineDto> spaceMarines) {
        this.spaceMarines = spaceMarines;
    }
}
