package spacemarine.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "achievements")
@XmlAccessorType(XmlAccessType.FIELD)
public class AchievementListDto {

    @XmlElement(name = "achievement")
    private List<String> achievements;

    public AchievementListDto() {
    }

    public AchievementListDto(List<String> achievements) {
        this.achievements = achievements;
    }
}
